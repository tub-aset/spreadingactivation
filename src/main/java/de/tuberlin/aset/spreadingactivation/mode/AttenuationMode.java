package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface AttenuationMode {

	double attenuation(Context context, Vertex vertex);

	public static final class Default {

		public static final AttenuationMode IGNORE = new AttenuationMode() {

			@Override
			public double attenuation(Context context, Vertex vertex) {
				return 1;
			}
		};

		public static final FixedAttenuationMode FIXED(double attenuationFactor) {
			return new FixedAttenuationMode(attenuationFactor);
		}

		public static final class FixedAttenuationMode implements AttenuationMode {

			private double attenuationFactor;

			private FixedAttenuationMode(double attenuationFactor) {
				this.attenuationFactor = attenuationFactor;
			}

			@Override
			public double attenuation(Context context, Vertex vertex) {
				return attenuationFactor;
			}

			public double getAttenuationFactor() {
				return attenuationFactor;
			}

		}

		public static final IncreasingAttenuationMode INCREASING(double attenuationFactor) {
			return new IncreasingAttenuationMode(attenuationFactor);
		}

		public static final class IncreasingAttenuationMode implements AttenuationMode {

			private double attenuationFactor;

			private IncreasingAttenuationMode(double attenuationFactor) {
				this.attenuationFactor = attenuationFactor;
			}

			@Override
			public double attenuation(Context context, Vertex vertex) {
				return Math.pow(0.99d, context.pulse()) * attenuationFactor;
			}

			public double getAttenuationFactor() {
				return attenuationFactor;
			}

		}

	}

}
