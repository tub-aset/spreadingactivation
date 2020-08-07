package de.tuberlin.aset.spreadingactivation;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
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

	@SuppressWarnings("unchecked")
	public void accumulateActivations(String sumKey, double lambda) {
		if (lambda == 1d) {
			String[] keys = new String[pulse + 1];

			for (int p = 0; p <= pulse; p++) {
				keys[p] = propertyKeyFactory.vertexActivationKey(p);
			}

			traversal.V().property(sumKey, __.coalesce(__.values(keys), __.constant(0d)).sum()).iterate();
		} else {
			Traversal<Object, Double>[] factors = new Traversal[pulse + 1];

			for (int p = 0; p <= pulse; p++) {
				double factor = Math.pow(lambda, p);
				factors[p] = __.coalesce(__.values(propertyKeyFactory.vertexActivationKey(p)), __.constant(0d))
						.as("act").constant(factor).as("factor").math("act * factor");
			}

			traversal.V().property(sumKey, __.union(factors).sum()).iterate();
		}
	}

	public GraphTraversalSource traversal() {
		return traversal;
	}

	@SuppressWarnings("unchecked")
	public <F extends PropertyKeyFactory> F propertyKeyFactory() {
		return (F) propertyKeyFactory;
	}

	public int pulse() {
		return pulse;
	}

	public double activation(Vertex vertex, int pulse) {
		return activation(vertex, propertyKeyFactory.vertexActivationKey(pulse));
	}

	public double activation(Vertex vertex, String propertyKey) {
		return (double) vertex.property(propertyKey).orElse(0d);
	}

	public Vertex vertex(Object id) {
		return traversal.V(id).next();
	}

	public double activation(Object id, int pulse) {
		return activation(vertex(id), pulse);
	}

	public GraphTraversal<?, Vertex> activatedVertices(int pulse) {
		return activatedVertices(propertyKeyFactory.vertexActivationKey(pulse));
	}

	public GraphTraversal<?, Vertex> activatedVertices(String propertyKey) {
		return traversal.V().has(propertyKey).order().by(propertyKey, Order.desc);
	}

	public GraphTraversal<?, Vertex> activatedVertices(int pulse, double minimumActivation) {
		return activatedVertices(propertyKeyFactory.vertexActivationKey(pulse), minimumActivation);
	}

	public GraphTraversal<?, Vertex> activatedVertices(String propertyKey, double minimumActivation) {
		return traversal.V().has(propertyKey, P.gte(minimumActivation)).order().by(propertyKey, Order.desc);
	}

	public void cleanup() {
		propertyKeyFactory.cleanupProperties(traversal, pulse);
	}

	public Generation.Builder generateSpreadGraph() {
		return Generation.build(this, traversal, propertyKeyFactory);
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
