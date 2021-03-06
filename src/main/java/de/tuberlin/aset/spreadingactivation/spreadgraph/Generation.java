package de.tuberlin.aset.spreadingactivation.spreadgraph;

import java.util.Iterator;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import de.tuberlin.aset.spreadingactivation.Execution;
import de.tuberlin.aset.spreadingactivation.ExecutionResult;
import de.tuberlin.aset.spreadingactivation.spreadgraph.SpreadGraph.DefaultPropertyKeyFactory;
import de.tuberlin.aset.spreadingactivation.spreadgraph.SpreadGraph.PropertyKeyFactory;
import de.tuberlin.aset.spreadingactivation.util.RunnableProcess;

public class Generation extends RunnableProcess {

	private final ExecutionResult executionResult;
	private final GraphTraversalSource originalTraversal;
	private final Execution.PropertyKeyFactory originalPropertyKeyFactory;

	private final GraphTraversalSource traversal;
	private final int startPulse;
	private final int endPulse;
	private final PropertyKeyFactory propertyKeyFactory;

	private final SpreadGraph spreadGraph;

	private Generation(Builder builder) {
		this.traversal = builder.traversal != null ? builder.traversal : TinkerGraph.open().traversal();
		this.propertyKeyFactory = builder.propertyKeyFactory != null ? builder.propertyKeyFactory
				: DefaultPropertyKeyFactory.INSTANCE;
		this.startPulse = builder.startPulse;
		this.endPulse = builder.endPulse;

		this.executionResult = builder.executionResult;
		this.originalTraversal = builder.originalTraversal;
		this.originalPropertyKeyFactory = builder.originalPropertyKeyFactory;

		this.spreadGraph = SpreadGraph.build(traversal, startPulse, endPulse, propertyKeyFactory).create();
	}

	public SpreadGraph getSpreadGraph() {
		return spreadGraph;
	}

	@Override
	public void run() {
		started();
		try {
			Iterator<Vertex> startVertices = executionResult.activatedVertices(startPulse);
			while (startVertices.hasNext()) {
				Vertex startVertex = startVertices.next();
				addVertex(startVertex, startPulse);
			}

			for (int pulse = startPulse + 1; pulse <= endPulse; pulse++) {
				if (this.isInterrupted()) {
					break;
				}
				GraphTraversal<?, Vertex> receivedActivationVertices = originalTraversal.V()
						.has(originalPropertyKeyFactory.inputActivationKey(pulse), P.gt(0d));
				while (receivedActivationVertices.hasNext()) {
					Vertex activatedVertex = receivedActivationVertices.next();
					Vertex toVertex = addVertex(activatedVertex, pulse);

					Iterator<Edge> edges = originalTraversal.V(activatedVertex.id()).toE(Direction.OUT)
							.has(originalPropertyKeyFactory.edgeActivationKey(pulse, false), P.gt(0d));
					while (edges.hasNext()) {
						Edge edge = edges.next();

						Vertex fromVertex = findVertex(edge.inVertex(), pulse - 1);

						addEdge(edge, fromVertex, toVertex);
					}

					edges = originalTraversal.V(activatedVertex.id()).toE(Direction.IN)
							.has(originalPropertyKeyFactory.edgeActivationKey(pulse, true), P.gt(0d));
					while (edges.hasNext()) {
						Edge edge = edges.next();

						Vertex fromVertex = findVertex(edge.outVertex(), pulse - 1);

						addEdge(edge, fromVertex, toVertex);
					}
				}
			}
		} finally {
			finished();
		}
	}

	private Edge addEdge(Edge originalEdge, Vertex fromVertex, Vertex toVertex) {
		Edge edge = fromVertex.addEdge(originalEdge.label(), toVertex);
		edge.property(propertyKeyFactory.originalIdKey(), originalEdge.id());
		return edge;
	}

	private Vertex addVertex(Vertex originalVertex, int pulse) {
		Object id = originalVertex.id();
		Optional<Vertex> optional = traversal.V().has(propertyKeyFactory.originalIdKey(), id)
				.has(propertyKeyFactory.pulseKey(), pulse).tryNext();
		if (optional.isPresent()) {
			return optional.get();
		}

		Vertex vertex = traversal.addV(originalVertex.label() + pulse).next();
		vertex.property(propertyKeyFactory.originalIdKey(), id);
		vertex.property(propertyKeyFactory.pulseKey(), pulse);
		return vertex;
	}

	private Vertex findVertex(Vertex originalVertex, int sincePulse) {
		Object id = originalVertex.id();
		return traversal.V().has(propertyKeyFactory.originalIdKey(), id)
				.has(propertyKeyFactory.pulseKey(), P.lte(sincePulse)).order()
				.by(propertyKeyFactory.pulseKey(), Order.desc).next();
	}

	public static final Builder build(ExecutionResult result, GraphTraversalSource traveral,
			Execution.PropertyKeyFactory propertyKeyFactory) {
		return new Builder(result, traveral, propertyKeyFactory);
	}

	public static class Builder {

		private final ExecutionResult executionResult;
		private final GraphTraversalSource originalTraversal;
		private final Execution.PropertyKeyFactory originalPropertyKeyFactory;

		private GraphTraversalSource traversal;
		private int startPulse;
		private int endPulse;
		private PropertyKeyFactory propertyKeyFactory;

		private Builder(ExecutionResult executionResult, GraphTraversalSource originalTraversal,
				Execution.PropertyKeyFactory originalPropertyKeyFactory) {
			this.executionResult = executionResult;
			this.originalTraversal = originalTraversal;
			this.originalPropertyKeyFactory = originalPropertyKeyFactory;

			this.startPulse = 0;
			this.endPulse = executionResult.pulse();
		}

		public Builder startPulse(int startPulse) {
			this.startPulse = startPulse;
			return this;
		}

		public Builder endPulse(int endPulse) {
			this.endPulse = endPulse;
			return this;
		}

		public Builder into(GraphTraversalSource spreadGraphTraversal) {
			this.traversal = spreadGraphTraversal;
			return this;
		}

		public Builder propertyKeyFactory(PropertyKeyFactory propertyKeyFactory) {
			this.propertyKeyFactory = propertyKeyFactory;
			return this;
		}

		public Generation create() {
			return new Generation(this);
		}
	}

}
