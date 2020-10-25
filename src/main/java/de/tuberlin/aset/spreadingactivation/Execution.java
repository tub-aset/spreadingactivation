package de.tuberlin.aset.spreadingactivation;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;

import de.tuberlin.aset.spreadingactivation.util.ExecutorQueue;
import de.tuberlin.aset.spreadingactivation.util.RunnableProcess;

public final class Execution extends RunnableProcess {

	private final GraphTraversalSource traversal;

	private final ExecutorService executor;
	private final int parallelTasks;

	private final Context context;
	private final PropertyKeyFactory propertyKeyFactory;

	private int pulse = 0;

	private Execution(Builder builder) {
		this.traversal = builder.traversal;
		this.executor = builder.executor;
		this.parallelTasks = builder.parallelTasks;
		this.propertyKeyFactory = builder.propertyKeyFactory != null ? builder.propertyKeyFactory
				: new DefaultPropertyKeyFactory(UUID.randomUUID().toString());

		this.context = new Context(builder.configuration, this);
	}

	public ExecutionResult getResult() {
		return ExecutionResult.build(traversal, propertyKeyFactory).pulse(pulse).create();
	}

	public void activate(GraphTraversal<?, Vertex> vertexFilter, double value) {
		if (isStarted()) {
			throw new IllegalStateException("execution already started");
		}
		if (isValidActivation(value)) {
			traversal.V().filter(vertexFilter)
					.property(Cardinality.single, propertyKeyFactory.vertexActivationKey(0), value).iterate();
		}
	}

	public void activate(GraphTraversal<Vertex, Long> activationTraversal) {
		traversal.V().property(Cardinality.single, propertyKeyFactory.vertexActivationKey(0), activationTraversal)
				.iterate();
	}

	@Override
	public void run() {
		checkFinished();
		started();

		ExecutorService executor = this.executor;
		boolean shutdownExecutor = false;
		if (executor == null) {
			executor = Executors.newCachedThreadPool();
			shutdownExecutor = true;
		}

		ExecutorQueue queue = new ExecutorQueue(executor, parallelTasks);

		try {
			pulseLoop: while (!this.isInterrupted() && pulse < context.pulses()) {
				pulse++;
				if (calculateOutputActivationAndEdgeActivation(queue, pulse)) {
					queue.awaitCompleted();

					if (calculateInputActivationAndVertexActivation(queue, pulse)) {
						queue.awaitCompleted();
					} else {
						break pulseLoop;
					}
				} else {
					break pulseLoop;
				}

				if (this.isInterrupted()) {
					break;
				}

				for (AbortCondition abortCondition : context.abortConditions()) {
					if (abortCondition.shouldAbort(this, context)) {
						break pulseLoop;
					}
				}
			}
			queue.awaitCompleted();
		} catch (Exception e) {
			throw new RuntimeException("exception in pulse " + pulse, e);
		} finally {
			if (shutdownExecutor) {
				executor.shutdown();
			}
			finished();
		}
	}

	private boolean calculateOutputActivationAndEdgeActivation(ExecutorQueue queue, int pulse) {
		Iterator<Vertex> startingVertices = context.startingVertices(context);

		if (!startingVertices.hasNext()) {
			return false;
		}

		queue.submit(new Iterator<Runnable>() {

			@Override
			public boolean hasNext() {
				return startingVertices.hasNext();
			}

			@Override
			public Runnable next() {
				Vertex fromVertex = startingVertices.next();
				return new Runnable() {

					@Override
					public void run() {
						double outputActivation = activation(fromVertex, pulse - 1);
						outputActivation *= context.attenuation(fromVertex);

						if (isValidActivation(outputActivation)) {
							outputActivation *= context.branch(fromVertex);
							if (isValidActivation(outputActivation)) {
								setPropertyValue(fromVertex, propertyKeyFactory.outputActivationKey(pulse),
										outputActivation);

								calculateEdgeActivation(queue, pulse, fromVertex, outputActivation);
							}
						}
					}

				};
			}
		});
		return true;
	}

	private final void calculateEdgeActivation(ExecutorQueue queue, int pulse, Vertex fromVertex,
			double outputActivation) {
		Iterator<Edge> edges = context.allowedEdges(fromVertex).dedup();
		queue.submit(new Iterator<Runnable>() {

			@Override
			public boolean hasNext() {
				return edges.hasNext();
			}

			@Override
			public Runnable next() {
				Edge edge = edges.next();
				return new Runnable() {

					@Override
					public void run() {
						boolean withDirection = edge.outVertex().equals(fromVertex);
						double edgeActivation = outputActivation * context.edgeWeight(edge, withDirection);
						if (isValidActivation(edgeActivation)) {
							setPropertyValue(edge, propertyKeyFactory.edgeActivationKey(pulse, withDirection),
									edgeActivation);
						}

					}
				};
			}
		});
	}

	private boolean calculateInputActivationAndVertexActivation(ExecutorQueue queue, int pulse) {
		Iterator<Vertex> vertexWithPreviousActivationOrEdgeActivation = traversal.V().or( //
				__.has(propertyKeyFactory.vertexActivationKey(pulse - 1)), //
				__.toE(Direction.BOTH).or( //
						__.has(propertyKeyFactory.edgeActivationKey(pulse, true)), //
						__.has(propertyKeyFactory.edgeActivationKey(pulse, false)) //
				) //
		);

		if (!vertexWithPreviousActivationOrEdgeActivation.hasNext()) {
			return false;
		}

		queue.submit(new Iterator<Runnable>() {

			@Override
			public boolean hasNext() {
				return vertexWithPreviousActivationOrEdgeActivation.hasNext();
			}

			@Override
			public Runnable next() {
				Vertex toVertex = vertexWithPreviousActivationOrEdgeActivation.next();
				return new Runnable() {

					@Override
					public void run() {
						double inputActivation = 0d;
						inputActivation += traversal.V(toVertex.id()).toE(Direction.IN)
								.values(propertyKeyFactory.edgeActivationKey(pulse, true)).sum().tryNext().orElse(0d)
								.doubleValue();
						inputActivation += traversal.V(toVertex.id()).toE(Direction.OUT)
								.values(propertyKeyFactory.edgeActivationKey(pulse, false)).sum().tryNext().orElse(0d)
								.doubleValue();

						if (isValidActivation(inputActivation)) {
							setPropertyValue(toVertex, propertyKeyFactory.inputActivationKey(pulse), inputActivation);
						}
						double lastVertexActivation = activation(toVertex, pulse - 1);
						double vertexActivation = context.activation(toVertex, inputActivation + lastVertexActivation);
						if (isValidActivation(vertexActivation)) {
							setPropertyValue(toVertex, propertyKeyFactory.vertexActivationKey(pulse), vertexActivation);
						}
					}
				};
			}
		});
		return true;
	}

	private boolean isValidActivation(double value) {
		return !Double.isInfinite(value) && !Double.isNaN(value) && value > 0d;
	}

	private void setPropertyValue(Element element, String key, Object value) {
		if (element.property(key).isPresent()) {
			throw new IllegalStateException("element " + element + ": property " + key + " already present");
		} else {
			if (element instanceof Vertex) {
				Vertex vertex = (Vertex) element;
				vertex.property(Cardinality.single, key, value);
			} else {
				element.property(key, value);
			}
		}
	}

	private double activation(Vertex vertex, int pulse) {
		return (double) vertex.property(propertyKeyFactory.vertexActivationKey(pulse)).orElse(0d);
	}

	public static Builder build(Configuration configuration, GraphTraversalSource traversal) {
		return new Builder(configuration, traversal);
	}

	public final static class Builder {

		private final Configuration configuration;
		private final GraphTraversalSource traversal;

		private ExecutorService executor;
		private int parallelTasks = Runtime.getRuntime().availableProcessors();

		private PropertyKeyFactory propertyKeyFactory;

		private Builder(Configuration configuration, GraphTraversalSource traversal) {
			this.configuration = configuration;
			this.traversal = traversal;
		}

		public Builder executor(ExecutorService executor) {
			this.executor = executor;
			return this;
		}

		public Builder parallelTasks(int parallelTasks) {
			this.parallelTasks = parallelTasks;
			return this;
		}

		public Builder propertyKeyFactory(PropertyKeyFactory propertyKeyFactory) {
			this.propertyKeyFactory = propertyKeyFactory;
			return this;
		}

		public Execution create() {
			return new Execution(this);
		}

	}

	public final static class Context {

		private final Configuration configuration;
		private final Execution execution;

		private Context(Configuration configuration, Execution execution) {
			this.configuration = configuration;
			this.execution = execution;
		}

		public int pulses() {
			return configuration.pulses();
		}

		public int pulse() {
			return execution.pulse;
		}

		public GraphTraversalSource traversal() {
			return execution.traversal;
		}

		public Iterator<Vertex> startingVertices(Context context) {
			return configuration.pulseInception().startingVertices(this);
		}

		public GraphTraversal<?, Edge> allowedEdges(Vertex vertex) {
			return execution.traversal.V(vertex.id()).toE(Direction.BOTH)
					.filter(configuration.sendMode().allowedEdges(this, vertex));
		}

		public double edgeWeight(Edge edge, boolean withDirection) {
			return configuration.edgeWeight().edgeWeight(this, edge, withDirection);
		}

		public double branch(Vertex vertex) {
			return configuration.branchMode().branch(this, vertex);
		}

		public double attenuation(Vertex vertex) {
			return configuration.attenuationMode().attenuation(this, vertex);
		}

		public double activation(Vertex vertex, double x) {
			return configuration.activationMode().activation(this, vertex, x);
		}

		public Collection<AbortCondition> abortConditions() {
			return configuration.abortConditions();
		}

		public String outputActivationKey(int pulse) {
			return execution.propertyKeyFactory.outputActivationKey(pulse);
		}

		public String edgeActivationKey(int pulse, boolean withDirection) {
			return execution.propertyKeyFactory.edgeActivationKey(pulse, withDirection);
		}

		public String inputActivationKey(int pulse) {
			return execution.propertyKeyFactory.inputActivationKey(pulse);
		}

		public String vertexActivationKey(int pulse) {
			return execution.propertyKeyFactory.vertexActivationKey(pulse);
		}

	}

	public static interface PropertyKeyFactory {

		String outputActivationKey(int pulse);

		String edgeActivationKey(int pulse, boolean withDirection);

		String inputActivationKey(int pulse);

		String vertexActivationKey(int pulse);

		void cleanupProperties(GraphTraversalSource traversal, int pulses);

	}

	public static class DefaultPropertyKeyFactory implements PropertyKeyFactory {

		private static final String OUTPUT_ACTIVATION_PROPERTY_KEY = "output_activation";
		private static final String EDGE_ACTIVATION_PROPERTY_KEY = "edge_activation";
		private static final String INPUT_ACTIVATION_PROPERTY_KEY = "input_activation";
		private static final String VERTEX_ACTIVATION_PROPERTY_KEY = "vertex_activation";

		private static final String SPLIT = "_";

		private final String propertyPrefix;

		public DefaultPropertyKeyFactory() {
			this(null);
		}

		public DefaultPropertyKeyFactory(String propertyPrefix) {
			this.propertyPrefix = propertyPrefix;
		}

		@Override
		public String outputActivationKey(int pulse) {
			return propertyKey(OUTPUT_ACTIVATION_PROPERTY_KEY, pulse);
		}

		@Override
		public String edgeActivationKey(int pulse, boolean withDirection) {
			return propertyKey(EDGE_ACTIVATION_PROPERTY_KEY, withDirection, pulse);
		}

		@Override
		public String inputActivationKey(int pulse) {
			return propertyKey(INPUT_ACTIVATION_PROPERTY_KEY, pulse);
		}

		@Override
		public String vertexActivationKey(int pulse) {
			return propertyKey(VERTEX_ACTIVATION_PROPERTY_KEY, pulse);
		}

		@Override
		public void cleanupProperties(GraphTraversalSource traversal, int pulses) {
			String[] vertexPropertyKeys = new String[(pulses + 1) * 3];
			String[] edgePropertyKeys = new String[(pulses + 1) * 2];
			for (int pulse = 0; pulse <= pulses; pulse++) {
				vertexPropertyKeys[pulse * 3 + 0] = outputActivationKey(pulse);
				vertexPropertyKeys[pulse * 3 + 1] = inputActivationKey(pulse);
				vertexPropertyKeys[pulse * 3 + 2] = vertexActivationKey(pulse);
				edgePropertyKeys[pulse * 2 + 0] = edgeActivationKey(pulse, true);
				edgePropertyKeys[pulse * 2 + 1] = edgeActivationKey(pulse, false);
			}
			traversal.V().properties(vertexPropertyKeys).drop().iterate();
			traversal.E().properties(edgePropertyKeys).drop().iterate();
		}

		public String getPropertyPrefix() {
			return propertyPrefix;
		}

		private String propertyKey(String key, boolean withDirection, int pulse) {
			StringBuilder sb = new StringBuilder();
			if (propertyPrefix != null && propertyPrefix.length() > 0) {
				sb.append(propertyPrefix);
				sb.append(SPLIT);
			}
			sb.append(key);
			sb.append(SPLIT);
			sb.append(withDirection);
			sb.append(SPLIT);
			sb.append(pulse);
			return sb.toString().intern();
		}

		private String propertyKey(String key, int pulse) {
			StringBuilder sb = new StringBuilder();
			if (propertyPrefix != null && propertyPrefix.length() > 0) {
				sb.append(propertyPrefix);
				sb.append(SPLIT);
			}
			sb.append(key);
			sb.append(SPLIT);
			sb.append(pulse);
			return sb.toString().intern();
		}

	}

}
