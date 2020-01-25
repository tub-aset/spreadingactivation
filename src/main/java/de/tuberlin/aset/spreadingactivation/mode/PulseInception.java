package de.tuberlin.aset.spreadingactivation.mode;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface PulseInception {

	Iterator<Vertex> startingVertices(Context context);

	public static final class Default {

		public static final PulseInception MINIMUM_ACTIVATION(double minimumActivation) {
			return new PulseInception() {

				@Override
				public Iterator<Vertex> startingVertices(Context context) {
					return context.traversal().V().has(context.vertexActivationKey(context.pulse() - 1),
							P.gte(minimumActivation));
				}
			};
		}

	}

}
