package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface BranchMode {

	double branch(Context context, Vertex vertex);

	public static final BranchMode NONE = new BranchMode() {

		@Override
		public double branch(Context context, Vertex vertex) {
			return 1d;
		}
	};

	public static final BranchMode DEGREE = new BranchMode() {

		@Override
		public double branch(Context context, Vertex vertex) {
			Long incidentEdgesCount = context.traversal().V(vertex.id()).toE(Direction.BOTH).count().next();
			if (incidentEdgesCount == 0d) {
				return 0d;
			}
			return 1d / incidentEdgesCount;
		}
	};

	public static final BranchMode FANOUT = new BranchMode() {

		@Override
		public double branch(Context context, Vertex vertex) {
			Long totalCount = context.traversal().V().count().next();
			if (totalCount == 0d) {
				return 0d;
			}
			Long adjacentVertexCount = context.traversal().V(vertex.id()).to(Direction.BOTH).count().next();
			return adjacentVertexCount / totalCount;
		}
	};

	public static final BranchMode BRANCH = new BranchMode() {

		@Override
		public double branch(Context context, Vertex vertex) {
			Long edgesCount = 0l;
			GraphTraversal<?, Edge> allowedEdges = context.allowedEdges(vertex);
			if (allowedEdges != null) {
				edgesCount = allowedEdges.count().next();
			}
			return 1d / Math.max(1d, edgesCount.doubleValue());
		}
	};

}
