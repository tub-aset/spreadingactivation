package de.tuberlin.aset.spreadingactivation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import de.tuberlin.aset.spreadingactivation.mode.ActivationMode;
import de.tuberlin.aset.spreadingactivation.mode.AttenuationMode;
import de.tuberlin.aset.spreadingactivation.mode.BranchMode;
import de.tuberlin.aset.spreadingactivation.mode.EdgeWeight;
import de.tuberlin.aset.spreadingactivation.mode.PulseInception;
import de.tuberlin.aset.spreadingactivation.mode.SendMode;

public class SpreadingActivation {

	private final int pulses;
	private final PulseInception pulseInception;
	private final ActivationMode activationMode;
	private final AttenuationMode attenuationMode;
	private final BranchMode branchMode;
	private final SendMode sendMode;
	private final EdgeWeight edgeWeight;
	private final Collection<AbortCondition> abortConditions;

	private SpreadingActivation(Builder builder) {
		this.pulses = builder.pulses;
		this.pulseInception = builder.pulseInception;
		this.activationMode = builder.activationMode;
		this.attenuationMode = builder.attenuationMode;
		this.branchMode = builder.branchMode;
		this.sendMode = builder.sendMode;
		this.edgeWeight = builder.edgeWeight;
		this.abortConditions = Collections.unmodifiableCollection(new ArrayList<>(builder.abortConditions));
	}

	public Execution.Builder execution(GraphTraversalSource traversal) {
		return new Execution.Builder(this, traversal);
	}

	public int pulses() {
		return pulses;
	}

	public PulseInception pulseInception() {
		return pulseInception;
	}

	public ActivationMode activationMode() {
		return activationMode;
	}

	public AttenuationMode attenuationMode() {
		return attenuationMode;
	}

	public BranchMode branchMode() {
		return branchMode;
	}

	public SendMode sendMode() {
		return sendMode;
	}

	public EdgeWeight edgeWeight() {
		return edgeWeight;
	}

	public Collection<AbortCondition> abortConditions() {
		return abortConditions;
	}

	public static class Builder {

		private int pulses;
		private PulseInception pulseInception = PulseInception.Default.MINIMUM_ACTIVATION(0);
		private ActivationMode activationMode = ActivationMode.Default.IDENTITY;
		private AttenuationMode attenuationMode = AttenuationMode.Default.IGNORE;
		private BranchMode branchMode = BranchMode.Default.NONE;
		private SendMode sendMode = SendMode.Default.BASIC;
		private EdgeWeight edgeWeight = EdgeWeight.Default.CONSTANT;
		private Collection<AbortCondition> abortConditions = new ArrayList<>();

		private Builder(int pulses) {
			this.pulses = pulses;
		}

		public Builder(SpreadingActivation spreadingActivation) {
			this.pulses = spreadingActivation.pulses;
			this.pulseInception = spreadingActivation.pulseInception;
			this.activationMode = spreadingActivation.activationMode;
			this.attenuationMode = spreadingActivation.attenuationMode;
			this.branchMode = spreadingActivation.branchMode;
			this.sendMode = spreadingActivation.sendMode;
			this.edgeWeight = spreadingActivation.edgeWeight;
			this.abortConditions = new ArrayList<>(spreadingActivation.abortConditions);
		}

		public Builder pulses(int pulses) {
			this.pulses = pulses;
			return this;
		}

		public Builder pulseInception(PulseInception pulseInception) {
			this.pulseInception = pulseInception;
			return this;
		}

		public Builder activationMode(ActivationMode activationMode) {
			this.activationMode = activationMode;
			return this;
		}

		public Builder attenuationMode(AttenuationMode attenuationMode) {
			this.attenuationMode = attenuationMode;
			return this;
		}

		public Builder branchMode(BranchMode branchMode) {
			this.branchMode = branchMode;
			return this;
		}

		public Builder sendMode(SendMode sendMode) {
			this.sendMode = sendMode;
			return this;
		}

		public Builder edgeWeight(EdgeWeight edgeWeight) {
			this.edgeWeight = edgeWeight;
			return this;
		}

		public Builder addAbortCondition(AbortCondition abortCondition) {
			this.abortConditions.add(abortCondition);
			return this;
		}

		public SpreadingActivation create() {
			return new SpreadingActivation(this);
		}

	}

	public static Builder build(int pulses) {
		return new Builder(pulses);
	}

	public static Builder build(SpreadingActivation spreadingActivation) {
		return new Builder(spreadingActivation);
	}

}
