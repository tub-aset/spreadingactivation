package de.tuberlin.aset.spreadingactivation.mode;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface PulseInception {

	Iterator<Vertex> startingVertices(Context context);

	public static final class Default {

		public static final MinimumActivationPulseInception MINIMUM_ACTIVATION = new MinimumActivationPulseInception(
				0d);

		public static final MinimumActivationPulseInception MINIMUM_ACTIVATION(double minimumActivation) {
			return new MinimumActivationPulseInception(minimumActivation);
		}

		public static final class MinimumActivationPulseInception implements PulseInception {

			private final double minimumActivation;

			private MinimumActivationPulseInception(double minimumActivation) {
				this.minimumActivation = minimumActivation;
			}

			@Override
			public Iterator<Vertex> startingVertices(Context context) {
				return context.traversal().V() //
						.has(context.vertexActivationKey(context.pulse() - 1), P.gte(minimumActivation)) //
						.dedup();
			}

			public double getMinimumActivation() {
				return minimumActivation;
			}

		}

	}

}
