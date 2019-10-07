package de.tuberlin.aset.spreadingactivation.spreadgraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class SpreadGraph {

	private final GraphTraversalSource traversal;
	private final int startPulse;
	private final int endPulse;
	private final PropertyKeyFactory propertyKeyFactory;

	SpreadGraph(GraphTraversalSource traversal, int startPulse, int endPulse, PropertyKeyFactory propertyKeyFactory) {
		this.traversal = traversal;
		this.startPulse = startPulse;
		this.endPulse = endPulse;
		this.propertyKeyFactory = propertyKeyFactory;
	}

	public GraphTraversal<Vertex, Vertex> vertices(Object originalId) {
		return traversal.V().has(propertyKeyFactory.originalIdKey(), originalId);
	}

	public GraphTraversal<Vertex, Vertex> vertices(int pulse) {
		return traversal.V().has(propertyKeyFactory.pulseKey(), pulse);
	}

	public GraphTraversal<Vertex, Vertex> vertices(int pulse, Object originalId) {
		return traversal.V().has(propertyKeyFactory.originalIdKey(), originalId).has(propertyKeyFactory.pulseKey(),
				pulse);
	}

	public int startPulse() {
		return startPulse;
	}

	public int endPulse() {
		return endPulse;
	}

	public GraphTraversal<Vertex, Vertex> startVertexes() {
		return traversal.V().not(__.inE());
	}

	public GraphTraversal<Vertex, Vertex> endVertexes() {
		return traversal.V().not(__.outE());
	}

	public int pulse(Vertex vertex) {
		return (int) vertex.property(propertyKeyFactory.pulseKey()).value();
	}

	public Object originalId(Vertex vertex) {
		return vertex.property(propertyKeyFactory.originalIdKey()).value();
	}

	public Object originalId(Edge edge) {
		return edge.property(propertyKeyFactory.originalIdKey()).value();
	}

	public GraphTraversalSource traversal() {
		return traversal;
	}

	@SuppressWarnings("unchecked")
	public <F extends PropertyKeyFactory> F propertyKeyFactory() {
		return (F) propertyKeyFactory;
	}

	public static interface PropertyKeyFactory {

		String pulseKey();

		String originalIdKey();

	}

	public static class DefaultPropertyKeyFactory implements PropertyKeyFactory {

		public static final DefaultPropertyKeyFactory INSTANCE = new DefaultPropertyKeyFactory();

		private DefaultPropertyKeyFactory() {
		}

		@Override
		public String pulseKey() {
			return "pulse";
		}

		@Override
		public String originalIdKey() {
			return "original";
		}

	}

}
