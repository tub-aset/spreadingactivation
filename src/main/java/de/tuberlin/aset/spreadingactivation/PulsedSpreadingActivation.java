package de.tuberlin.aset.spreadingactivation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;
import de.tuberlin.aset.spreadingactivation.mode.ActivationMode;
import de.tuberlin.aset.spreadingactivation.mode.AttenuationMode;
import de.tuberlin.aset.spreadingactivation.mode.BranchMode;
import de.tuberlin.aset.spreadingactivation.mode.EdgeWeight;
import de.tuberlin.aset.spreadingactivation.mode.PulseInception;
import de.tuberlin.aset.spreadingactivation.mode.SendMode;

public final class PulsedSpreadingActivation implements Configuration {

	private final int pulses;
	private final PulseInception pulseInception;
	private final PulsedActivationMode activationMode;
	private final PulsedAttenuationMode attenuationMode;
	private final PulsedBranchMode branchMode;
	private final PulsedSendMode sendMode;
	private final PulsedEdgeWeight edgeWeight;
	private final Collection<AbortCondition> abortConditions;

	private PulsedSpreadingActivation(Builder builder) {
		this.pulses = builder.pulses;
		this.pulseInception = builder.pulseInception;
		this.activationMode = new PulsedActivationMode(builder.activationModes, builder.defaultActivationMode);
		this.attenuationMode = new PulsedAttenuationMode(builder.attenuationModes, builder.defaultAttenuationMode);
		this.branchMode = new PulsedBranchMode(builder.branchModes, builder.defaultBranchMode);
		this.sendMode = new PulsedSendMode(builder.sendModes, builder.defaultSendMode);
		this.edgeWeight = new PulsedEdgeWeight(builder.edgeWeights, builder.defaultEdgeWeight);
		this.abortConditions = Collections.unmodifiableCollection(builder.abortConditions);
	}

	@Override
	public Execution.Builder execution(GraphTraversalSource traversal) {
		return Execution.build(this, traversal);
	}

	@Override
	public int pulses() {
		return pulses;
	}

	@Override
	public PulseInception pulseInception() {
		return pulseInception;
	}

	@Override
	public PulsedActivationMode activationMode() {
		return activationMode;
	}

	@Override
	public PulsedAttenuationMode attenuationMode() {
		return attenuationMode;
	}

	@Override
	public PulsedBranchMode branchMode() {
		return branchMode;
	}

	@Override
	public PulsedSendMode sendMode() {
		return sendMode;
	}

	@Override
	public PulsedEdgeWeight edgeWeight() {
		return edgeWeight;
	}

	@Override
	public Collection<AbortCondition> abortConditions() {
		return abortConditions;
	}

	public static Builder build(int pulses) {
		return new Builder(pulses);
	}

	public static final class Builder {

		private ActivationMode defaultActivationMode = ActivationMode.Default.IDENTITY;
		private AttenuationMode defaultAttenuationMode = AttenuationMode.Default.IGNORE;
		private BranchMode defaultBranchMode = BranchMode.Default.NONE;
		private SendMode defaultSendMode = SendMode.Default.BASIC;
		private EdgeWeight defaultEdgeWeight = EdgeWeight.Default.CONSTANT;
		private int pulses;
		private PulseInception pulseInception = PulseInception.Default.MINIMUM_ACTIVATION;
		private Map<Integer, ActivationMode> activationModes = new HashMap<>();
		private Map<Integer, AttenuationMode> attenuationModes = new HashMap<>();
		private Map<Integer, BranchMode> branchModes = new HashMap<>();
		private Map<Integer, SendMode> sendModes = new HashMap<>();
		private Map<Integer, EdgeWeight> edgeWeights = new HashMap<>();
		private Collection<AbortCondition> abortConditions = new ArrayList<>();

		private Builder(int pulses) {
			this.pulses = pulses;
		}

		public Builder pulses(int pulses) {
			this.pulses = pulses;
			return this;
		}

		public Builder pulseInception(PulseInception pulseInception) {
			this.pulseInception = pulseInception;
			return this;
		}

		public Builder activationMode(Integer maxPulse, ActivationMode activationMode) {
			this.activationModes.put(maxPulse, activationMode);
			return this;
		}

		public Builder defaultActivationMode(ActivationMode defaultActivationMode) {
			this.defaultActivationMode = defaultActivationMode;
			return this;
		}

		public Builder attenuationMode(Integer maxPulse, AttenuationMode attenuationMode) {
			this.attenuationModes.put(maxPulse, attenuationMode);
			return this;
		}

		public Builder defaultAttenuationMode(AttenuationMode defaultAttenuationMode) {
			this.defaultAttenuationMode = defaultAttenuationMode;
			return this;
		}

		public Builder branchMode(Integer maxPulse, BranchMode branchMode) {
			this.branchModes.put(maxPulse, branchMode);
			return this;
		}

		public Builder defaultBranchMode(BranchMode defaultBranchMode) {
			this.defaultBranchMode = defaultBranchMode;
			return this;
		}

		public Builder sendMode(Integer maxPulse, SendMode sendMode) {
			this.sendModes.put(maxPulse, sendMode);
			return this;
		}

		public Builder defaultSendMode(SendMode defaultSendMode) {
			this.defaultSendMode = defaultSendMode;
			return this;
		}

		public Builder edgeWeight(Integer maxPulse, EdgeWeight edgeWeight) {
			this.edgeWeights.put(maxPulse, edgeWeight);
			return this;
		}

		public Builder defaultEdgeWeight(EdgeWeight defaultEdgeWeight) {
			this.defaultEdgeWeight = defaultEdgeWeight;
			return this;
		}

		public Builder addAbortCondition(AbortCondition abortCondition) {
			this.abortConditions.add(abortCondition);
			return this;
		}

		public PulsedSpreadingActivation create() {
			return new PulsedSpreadingActivation(this);
		}

	}

	public static abstract class PulsedMode<MODE> {

		private final Map<Integer, MODE> modes;
		private final MODE defaultMode;

		private PulsedMode(Map<Integer, MODE> modes, MODE defaultMode) {
			this.defaultMode = defaultMode;
			this.modes = Collections.unmodifiableMap(modes);
		}

		public MODE getMode(int pulse) {
			return modes.entrySet().stream().filter(entry -> entry.getKey() <= pulse)
					.max((e1, e2) -> e1.getKey().compareTo(e2.getKey())).map(entry -> entry.getValue())
					.orElse(defaultMode);
		}

		public Set<Integer> getDefinedPulses() {
			return Collections.unmodifiableSet(modes.keySet());
		}

		public MODE getDefaultMode() {
			return defaultMode;
		}

	}

	public static final class PulsedMinimumActivationPulseInception extends PulsedMode<Double>
			implements PulseInception {

		private PulsedMinimumActivationPulseInception(Map<Integer, Double> minimumActivations,
				Double defaultMinimumActivation) {
			super(minimumActivations, defaultMinimumActivation);
		}

		private PulsedMinimumActivationPulseInception(Builder builder) {
			this(builder.minimumActivations, builder.defaultMinimumActivation);
		}

		@Override
		public Iterator<Vertex> startingVertices(Context context) {
			Double minimumActivation = getMode(context.pulse());
			return context.traversal().V()
					.has(context.vertexActivationKey(context.pulse() - 1), P.gte(minimumActivation)).dedup();
		}

		public static Builder build() {
			return new Builder();
		}

		public static class Builder {

			private Map<Integer, Double> minimumActivations = new HashMap<>();
			private Double defaultMinimumActivation = 0d;

			private Builder() {
			}

			public Builder minimumActivation(Integer maxPulse, Double minimumActivation) {
				this.minimumActivations.put(maxPulse, minimumActivation);
				return this;
			}

			public Builder defaultMinimumActivation(Double defaultMinimumActivation) {
				this.defaultMinimumActivation = defaultMinimumActivation;
				return this;
			}

			public PulsedMinimumActivationPulseInception create() {
				return new PulsedMinimumActivationPulseInception(this);
			}

		}

	}

	public static final class PulsedActivationMode extends PulsedMode<ActivationMode> implements ActivationMode {

		private PulsedActivationMode(Map<Integer, ActivationMode> activationModes,
				ActivationMode defaultActivationMode) {
			super(activationModes, defaultActivationMode);
		}

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return getMode(context.pulse()).activation(context, vertex, x);
		}

	}

	public static final class PulsedAttenuationMode extends PulsedMode<AttenuationMode> implements AttenuationMode {

		private PulsedAttenuationMode(Map<Integer, AttenuationMode> attenuationModes,
				AttenuationMode defaultAttenuationMode) {
			super(attenuationModes, defaultAttenuationMode);
		}

		@Override
		public double attenuation(Context context, Vertex vertex) {
			return getMode(context.pulse()).attenuation(context, vertex);
		}

	}

	public static final class PulsedBranchMode extends PulsedMode<BranchMode> implements BranchMode {

		private PulsedBranchMode(Map<Integer, BranchMode> branchModes, BranchMode defaultBranchMode) {
			super(branchModes, defaultBranchMode);
		}

		@Override
		public double branch(Context context, Vertex vertex) {
			return getMode(context.pulse()).branch(context, vertex);
		}
	}

	public static final class PulsedSendMode extends PulsedMode<SendMode> implements SendMode {

		private PulsedSendMode(Map<Integer, SendMode> sendModes, SendMode defaultSendMode) {
			super(sendModes, defaultSendMode);
		}

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			return getMode(context.pulse()).allowedEdges(context, vertex);
		}

	}

	public static final class PulsedEdgeWeight extends PulsedMode<EdgeWeight> implements EdgeWeight {

		private PulsedEdgeWeight(Map<Integer, EdgeWeight> edgeWeights, EdgeWeight defaultEdgeWeight) {
			super(edgeWeights, defaultEdgeWeight);
		}

		@Override
		public double edgeWeight(Context context, Edge edge, boolean withDirection) {
			return getMode(context.pulse()).edgeWeight(context, edge, withDirection);
		}

	}

}
