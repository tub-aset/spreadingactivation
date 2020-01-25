package de.tuberlin.aset.spreadingactivation;

import java.util.Collection;

import de.tuberlin.aset.spreadingactivation.mode.ActivationMode;
import de.tuberlin.aset.spreadingactivation.mode.AttenuationMode;
import de.tuberlin.aset.spreadingactivation.mode.BranchMode;
import de.tuberlin.aset.spreadingactivation.mode.EdgeWeight;
import de.tuberlin.aset.spreadingactivation.mode.PulseInception;
import de.tuberlin.aset.spreadingactivation.mode.SendMode;

public interface Configuration {

	int pulses();

	PulseInception pulseInception();

	ActivationMode activationMode();

	AttenuationMode attenuationMode();

	BranchMode branchMode();

	SendMode sendMode();

	EdgeWeight edgeWeight();

	Collection<AbortCondition> abortConditions();

}