package de.tuberlin.aset.spreadingactivation.spreadgraph;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import de.tuberlin.aset.spreadingactivation.spreadgraph.SpreadGraph.DefaultPropertyKeyFactory;
import de.tuberlin.aset.spreadingactivation.spreadgraph.SpreadGraph.PropertyKeyFactory;
import de.tuberlin.aset.spreadingactivation.util.RunnableProcess;

public abstract class Transformation extends RunnableProcess {

	protected final SpreadGraph originalSpreadGraph;

	protected final GraphTraversalSource traversal;
	protected final int startPulse;
	protected final int endPulse;
	protected final PropertyKeyFactory propertyKeyFactory;

	protected final SpreadGraph spreadGraph;

	protected Transformation(Builder<?> builder) {
		this.traversal = builder.traversal != null ? builder.traversal : TinkerGraph.open().traversal();
		this.propertyKeyFactory = builder.propertyKeyFactory != null ? builder.propertyKeyFactory
				: DefaultPropertyKeyFactory.INSTANCE;
		this.startPulse = builder.startPulse;
		this.endPulse = builder.endPulse;

		this.originalSpreadGraph = builder.originalSpreadGraph;

		this.spreadGraph = SpreadGraph.build(traversal, startPulse, endPulse, propertyKeyFactory).create();
	}

	public SpreadGraph getSpreadGraph() {
		return spreadGraph;
	}

	protected Vertex getVertex(Vertex originalVertex, int pulse) {
		Object id = originalSpreadGraph.originalId(originalVertex);

		Optional<Vertex> optional = traversal.V().has(propertyKeyFactory.originalIdKey(), id)
				.has(propertyKeyFactory.pulseKey(), pulse).tryNext();
		if (optional.isPresent()) {
			return optional.get();
		}
		return null;
	}

	protected Vertex addVertex(Vertex originalVertex, int pulse) {
		Object id = originalSpreadGraph.originalId(originalVertex);

		Vertex vertex = traversal.addV(originalVertex.label()).next();
		vertex.property(propertyKeyFactory.originalIdKey(), id);
		vertex.property(propertyKeyFactory.pulseKey(), pulse);
		return vertex;
	}

	protected Edge addEdge(Edge originalEdge, Vertex fromVertex, Vertex toVertex) {
		Edge edge = fromVertex.addEdge(originalEdge.label(), toVertex);
		edge.property(propertyKeyFactory.originalIdKey(), spreadGraph.originalId(originalEdge));
		return edge;
	}

	public static class Builder<B extends Builder<B>> {

		private final SpreadGraph originalSpreadGraph;

		private GraphTraversalSource traversal;
		private int startPulse;
		private int endPulse;
		private PropertyKeyFactory propertyKeyFactory;

		protected Builder(SpreadGraph originalSpreadGraph) {
			this.originalSpreadGraph = originalSpreadGraph;

			this.startPulse = originalSpreadGraph.startPulse();
			this.endPulse = originalSpreadGraph.endPulse();
		}

		public B startPulse(int startPulse) {
			this.startPulse = startPulse;
			return self();
		}

		public B endPulse(int endPulse) {
			this.endPulse = endPulse;
			return self();
		}

		public B into(GraphTraversalSource spreadGraphTraversal) {
			this.traversal = spreadGraphTraversal;
			return self();
		}

		public B propertyKeyFactory(PropertyKeyFactory propertyKeyFactory) {
			this.propertyKeyFactory = propertyKeyFactory;
			return self();
		}

		@SuppressWarnings("unchecked")
		final B self() {
			return (B) this;
		}

	}

}
