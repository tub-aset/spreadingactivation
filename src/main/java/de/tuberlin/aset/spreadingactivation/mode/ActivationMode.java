package de.tuberlin.aset.spreadingactivation.mode;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface ActivationMode {

	double activation(Context context, Vertex vertex, double x);

	public static final ActivationMode IDENTITY = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return x;
		}
	};

	public static final ActivationMode SIG = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return 2d * ((1d / (1d + Math.exp(-x))) - 0.5d);
		}
	};

	public static final ActivationMode LOG2 = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return Math.log(x + 1) / Math.log(2d);
		}
	};

	public static final ActivationMode LOG10 = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return (Math.log10(x + 1));
		}
	};

	public static final ActivationMode LOG2SIG = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return SIG.activation(context, vertex, LOG2.activation(context, vertex, x));
		}
	};

	public static final ActivationMode LOG10SIG = new ActivationMode() {

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return SIG.activation(context, vertex, LOG10.activation(context, vertex, x));
		}
	};

}
