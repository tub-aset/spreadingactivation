package de.tuberlin.aset.spreadingactivation;

import de.tuberlin.aset.spreadingactivation.Execution.Context;

public interface AbortCondition {

	boolean shouldAbort(Context context, ExecutionResult result);

}
