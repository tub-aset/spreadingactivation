package de.tuberlin.aset.spreadingactivation.mode;

import java.util.Arrays;
import java.util.Collection;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public final class SpreadingMode implements SendMode {

	private final Collection<SendMode> sendModes;

	public SpreadingMode(SendMode... sendModes) {
		this(Arrays.asList(sendModes));
	}

	public SpreadingMode(Collection<SendMode> sendModes) {
		this.sendModes = sendModes;
	}

	@Override
	public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
		@SuppressWarnings("unchecked")
		Traversal<?, Edge>[] traversals = new Traversal[sendModes.size()];
		int i = 0;
		for (SendMode sendMode : sendModes) {
			traversals[i++] = sendMode.allowedEdges(context, vertex);
		}
		return __.and(traversals);
	}

	public static final class Default {

		public static final SpreadingMode BASIC = new SpreadingMode(SendMode.Default.BASIC);

		public static final SpreadingMode RECENT_RECEIVER = new SpreadingMode(SendMode.Default.RECENT_RECEIVER);

		public static final SpreadingMode FORWARD = new SpreadingMode(SendMode.Default.FORWARD);

		public static final SpreadingMode FORWARD_RECENT_RECEIVER = new SpreadingMode(SendMode.Default.FORWARD,
				SendMode.Default.RECENT_RECEIVER);

		public static final SpreadingMode FORWARD_LOOP = new SpreadingMode(SendMode.Default.FORWARD_LOOP);

		public static final SpreadingMode FORWARD_LOOP_RECENT_RECEIVER = new SpreadingMode(
				SendMode.Default.FORWARD_LOOP, SendMode.Default.RECENT_RECEIVER);

	}

}
