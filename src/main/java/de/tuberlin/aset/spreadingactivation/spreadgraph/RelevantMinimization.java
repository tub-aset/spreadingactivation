package de.tuberlin.aset.spreadingactivation.spreadgraph;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class RelevantMinimization extends Transformation {

	private final Object originalId;

	private RelevantMinimization(Builder builder) {
		super(builder);

		this.originalId = builder.originalId;
	}

	@Override
	public void run() {
		started();
		try {
			GraphTraversal<Vertex, Vertex> originalRevelantVertexes = originalSpreadGraph.vertexes(originalId);

			while (originalRevelantVertexes.hasNext()) {
				if (this.isInterrupted()) {
					break;
				}

				Vertex originalRevelantVertex = originalRevelantVertexes.next();

				int pulse = originalSpreadGraph.pulse(originalRevelantVertex);

				if (pulse <= endPulse) {
					Vertex relevantVertex = getVertex(originalRevelantVertex, pulse);

					if (relevantVertex == null) {
						relevantVertex = addVertex(originalRevelantVertex, pulse);
					}

					addPathBackwards(originalRevelantVertex, relevantVertex);
				}

			}
		} finally {
			finished();
		}
	}

	private void addPathBackwards(Vertex originalTargetVertex, Vertex targetVertex) {
		if (this.isInterrupted()) {
			return;
		}
		Iterator<Edge> edges = originalTargetVertex.edges(Direction.IN);
		while (edges.hasNext()) {
			Edge edge = edges.next();
			Vertex originalSourceVertex = edge.outVertex();
			int pulse = originalSpreadGraph.pulse(originalSourceVertex);

			if (pulse >= startPulse) {
				Vertex sourceVertex = getVertex(originalSourceVertex, pulse);

				if (sourceVertex != null) {
					addEdge(edge, sourceVertex, targetVertex);
				} else {
					sourceVertex = addVertex(originalSourceVertex, pulse);
					addEdge(edge, sourceVertex, targetVertex);

					addPathBackwards(originalSourceVertex, sourceVertex);
				}
			}
		}
	}

	public static Builder build(SpreadGraph originalSpreadGraph, Object originalId) {
		return new Builder(originalSpreadGraph, originalId);
	}

	public static class Builder extends Transformation.Builder<Builder> {

		private final Object originalId;

		private Builder(SpreadGraph originalSpreadGraph, Object originalId) {
			super(originalSpreadGraph);
			this.originalId = originalId;
		}

		public RelevantMinimization create() {
			return new RelevantMinimization(this);
		}

	}

}
