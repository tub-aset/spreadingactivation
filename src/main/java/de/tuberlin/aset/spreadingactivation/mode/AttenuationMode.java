package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface AttenuationMode {

	double attenuation(Context context, Vertex vertex);

	public static final AttenuationMode IGNORE = new AttenuationMode() {

		@Override
		public double attenuation(Context context, Vertex vertex) {
			return 1;
		}
	};

	public static final AttenuationMode FIXED = new AttenuationMode() {

		@Override
		public double attenuation(Context context, Vertex vertex) {
			return context.attenuationFactor();
		}
	};

	public static final AttenuationMode INCREASING = new AttenuationMode() {

		@Override
		public double attenuation(Context context, Vertex vertex) {
			return Math.pow(0.99d, context.pulse()) * context.attenuationFactor();
		}
	};

}
