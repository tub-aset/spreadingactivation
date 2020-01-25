package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.structure.Edge;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface EdgeWeight {

	double edgeWeight(Context context, Edge edge, boolean withDirection);

	public static final class Default {

		public static final EdgeWeight CONSTANT = new EdgeWeight() {

			@Override
			public double edgeWeight(Context context, Edge edge, boolean withDirection) {
				return 1d;
			}
		};

	}

}
