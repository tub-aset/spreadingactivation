package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface SendMode {

	GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex);

	public static final SendMode BASIC = new SendMode() {

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			return __.start();
		}
	};

	public static final SendMode RECENT_RECEIVER = new SendMode() {

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			if (context.pulse() == 1) {
				return BASIC.allowedEdges(context, vertex);
			}

			if (vertex.property(context.inputActivationKey(context.pulse() - 1)).isPresent()) {
				return __.start();
			}
			return __.is(false);
		}
	};

	public static final SendMode FORWARD = new SendMode() {

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			if (context.pulse() == 1) {
				return BASIC.allowedEdges(context, vertex);
			}
			return __.or(
					__.hasNot(context.edgeActivationKey(context.pulse() - 1, true)).toV(Direction.IN)
							.hasId(vertex.id()),
					__.hasNot(context.edgeActivationKey(context.pulse() - 1, false)).toV(Direction.OUT)
							.hasId(vertex.id()));
		}
	};

	public static final SendMode FORWARD_LOOP = new SendMode() {

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			if (context.pulse() == 1) {
				return BASIC.allowedEdges(context, vertex);
			}
			return __.or(
					__.hasNot(context.edgeActivationKey(context.pulse() - 1, true)).toV(Direction.IN)
							.hasId(vertex.id()),
					__.has(context.edgeActivationKey(context.pulse() - 1, true)).as("e").toV(Direction.IN)
							.hasId(vertex.id()).toE(Direction.IN)
							.has(context.edgeActivationKey(context.pulse() - 1, true)).where(P.neq("e")),
					__.hasNot(context.edgeActivationKey(context.pulse() - 1, false)).toV(Direction.OUT)
							.hasId(vertex.id()),
					__.has(context.edgeActivationKey(context.pulse() - 1, false)).as("e").toV(Direction.OUT)
							.hasId(vertex.id()).toE(Direction.OUT)
							.has(context.edgeActivationKey(context.pulse() - 1, false)).where(P.neq("e"))

			);
		}
	};
}
