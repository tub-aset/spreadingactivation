package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.structure.Edge;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface EdgeWeight {

	double edgeWeight(Context context, Edge edge, boolean withDirection);

	public static final class Default {

		public static final EdgeWeight CONSTANT = new ConstantEdgeWeight(1d);

		public static final EdgeWeight CONSTANT(double edgeWeightFactor) {
			return new ConstantEdgeWeight(edgeWeightFactor);
		};

		public static final class ConstantEdgeWeight implements EdgeWeight {

			private final double edgeWeightFactor;

			public ConstantEdgeWeight(double edgeWeightFactor) {
				this.edgeWeightFactor = edgeWeightFactor;
			}

			@Override
			public double edgeWeight(Context context, Edge edge, boolean withDirection) {
				return edgeWeightFactor;
			}

			public double getEdgeWeightFactor() {
				return edgeWeightFactor;
			}

		}

	}

}
