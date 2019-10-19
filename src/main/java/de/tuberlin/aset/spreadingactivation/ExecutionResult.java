package de.tuberlin.aset.spreadingactivation;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.PropertyKeyFactory;
import de.tuberlin.aset.spreadingactivation.spreadgraph.Generation;

public class ExecutionResult {

	private final GraphTraversalSource traversal;
	private final PropertyKeyFactory propertyKeyFactory;
	private final int pulse;

	private ExecutionResult(Builder builder) {
		this.traversal = builder.traversal;
		this.propertyKeyFactory = builder.propertyKeyFactory;
		this.pulse = builder.pulse;
	}

	public int pulse() {
		return pulse;
	}

	public double activation(Vertex vertex) {
		return activation(vertex, pulse);
	}

	public double activation(Vertex vertex, int pulse) {
		return (double) vertex.property(propertyKeyFactory.vertexActivationKey(pulse)).orElse(0d);
	}

	public double activation(Object id) {
		return activation(vertex(id));
	}

	public Vertex vertex(Object id) {
		return traversal.V(id).next();
	}

	public double activation(Object id, int pulse) {
		return activation(vertex(id), pulse);
	}

	public GraphTraversal<?, Vertex> activatedVertexes() {
		return activatedVertexes(pulse);
	}

	public GraphTraversal<?, Vertex> activatedVertexes(int pulse) {
		return traversal.V().has(propertyKeyFactory.vertexActivationKey(pulse)).order()
				.by(propertyKeyFactory.vertexActivationKey(pulse), Order.desc);
	}

	public GraphTraversal<?, Vertex> activatedVertexes(double minimumActivation) {
		return activatedVertexes(pulse, minimumActivation);
	}

	public GraphTraversal<?, Vertex> activatedVertexes(int pulse, double minimumActivation) {
		return traversal.V().has(propertyKeyFactory.vertexActivationKey(pulse), P.gte(minimumActivation)).order()
				.by(propertyKeyFactory.vertexActivationKey(pulse), Order.desc);
	}

	@SuppressWarnings("unchecked")
	public <F extends PropertyKeyFactory> F propertyKeyFactory() {
		return (F) propertyKeyFactory;
	}

	public void cleanup() {
		int pulses = this.pulse;
		String[] vertexPropertyKeys = new String[(pulses + 1) * 3];
		String[] edgePropertyKeys = new String[(pulses + 1) * 2];
		for (int pulse = 0; pulse <= pulses; pulse++) {
			vertexPropertyKeys[pulse * 3 + 0] = propertyKeyFactory.outputActivationKey(pulse);
			vertexPropertyKeys[pulse * 3 + 1] = propertyKeyFactory.inputActivationKey(pulse);
			vertexPropertyKeys[pulse * 3 + 2] = propertyKeyFactory.vertexActivationKey(pulse);
			edgePropertyKeys[pulse * 2 + 0] = propertyKeyFactory.edgeActivationKey(pulse, true);
			edgePropertyKeys[pulse * 2 + 1] = propertyKeyFactory.edgeActivationKey(pulse, false);
		}
		traversal.V().properties(vertexPropertyKeys).drop().iterate();
		traversal.E().properties(edgePropertyKeys).drop().iterate();
	}

	public Generation.Builder generateSpreadGraph() {
		return new Generation.Builder(this, traversal, propertyKeyFactory);
	}

	public static Builder build(GraphTraversalSource traversal, PropertyKeyFactory propertyKeyFactory) {
		return new Builder(traversal, propertyKeyFactory);
	}

	public static class Builder {

		private final GraphTraversalSource traversal;
		private final PropertyKeyFactory propertyKeyFactory;
		private int pulse = 0;

		private Builder(GraphTraversalSource traversal, PropertyKeyFactory propertyKeyFactory) {
			this.traversal = traversal;
			this.propertyKeyFactory = propertyKeyFactory;
		}

		public Builder pulse(int pulse) {
			this.pulse = pulse;
			return this;
		}

		public ExecutionResult create() {
			return new ExecutionResult(this);
		}

	}

}
