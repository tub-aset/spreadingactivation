package de.tuberlin.aset.spreadingactivation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.tuberlin.aset.spreadingactivation.Execution.Context;
import de.tuberlin.aset.spreadingactivation.mode.ActivationMode;
import de.tuberlin.aset.spreadingactivation.mode.AttenuationMode;
import de.tuberlin.aset.spreadingactivation.mode.BranchMode;
import de.tuberlin.aset.spreadingactivation.mode.EdgeWeight;
import de.tuberlin.aset.spreadingactivation.mode.PulseInception;
import de.tuberlin.aset.spreadingactivation.mode.SendMode;

public class TypedSpreadingActivation implements Configuration {

	private final int pulses;
	private final PulseInception pulseInception;
	private final ActivationMode activationMode;
	private final AttenuationMode attenuationMode;
	private final BranchMode branchMode;
	private final SendMode sendMode;
	private final EdgeWeight edgeWeight;
	private final Collection<AbortCondition> abortConditions;

	private TypedSpreadingActivation(Builder builder) {
		this.pulses = builder.pulses;
		this.pulseInception = builder.pulseInception;
		this.activationMode = new TypedActivationMode(builder.vertexTypePropertyKey, builder.activationModes);
		this.attenuationMode = new TypedAttenuationMode(builder.vertexTypePropertyKey, builder.attenuationModes);
		this.branchMode = new TypedBranchMode(builder.vertexTypePropertyKey, builder.branchModes);
		this.sendMode = new TypedSendMode(builder.vertexTypePropertyKey, builder.sendModes);
		this.edgeWeight = new TypedEdgeWeight(builder.edgeTypePropertyKey, builder.edgeWeights);
		this.abortConditions = Collections.unmodifiableCollection(builder.abortConditions);
	}

	public Execution.Builder execution(GraphTraversalSource traversal) {
		return new Execution.Builder(this, traversal);
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
	public ActivationMode activationMode() {
		return activationMode;
	}

	@Override
	public AttenuationMode attenuationMode() {
		return attenuationMode;
	}

	@Override
	public BranchMode branchMode() {
		return branchMode;
	}

	@Override
	public SendMode sendMode() {
		return sendMode;
	}

	@Override
	public EdgeWeight edgeWeight() {
		return edgeWeight;
	}

	@Override
	public Collection<AbortCondition> abortConditions() {
		return abortConditions;
	}

	public static Builder build(int pulses, String typePropertyKey) {
		return new Builder(pulses, typePropertyKey, typePropertyKey);
	}

	public static Builder build(int pulses, String vertexTypePropertyKey, String edgeTypePropertyKey) {
		return new Builder(pulses, vertexTypePropertyKey, edgeTypePropertyKey);
	}

	public static class Builder {

		private int pulses;
		private String vertexTypePropertyKey;
		private String edgeTypePropertyKey;
		private PulseInception pulseInception = PulseInception.Default.MINIMUM_ACTIVATION(0d);
		private Map<Object, ActivationMode> activationModes = new HashMap<>();
		private Map<Object, AttenuationMode> attenuationModes = new HashMap<>();
		private Map<Object, BranchMode> branchModes = new HashMap<>();
		private Map<Object, SendMode> sendModes = new HashMap<>();
		private Map<Object, EdgeWeight> edgeWeights = new HashMap<>();
		private Collection<AbortCondition> abortConditions = new ArrayList<>();

		private Builder(int pulses, String vertexTypePropertyKey, String edgeTypePropertyKey) {
			this.pulses = pulses;
			this.vertexTypePropertyKey = vertexTypePropertyKey;
			this.edgeTypePropertyKey = edgeTypePropertyKey;
		}

		public Builder pulses(int pulses) {
			this.pulses = pulses;
			return this;
		}

		public Builder pulseInception(PulseInception pulseInception) {
			this.pulseInception = pulseInception;
			return this;
		}

		public Builder activationMode(Object type, ActivationMode activationMode) {
			this.activationModes.put(type, activationMode);
			return this;
		}

		public Builder attenuationMode(Object type, AttenuationMode attenuationMode) {
			this.attenuationModes.put(type, attenuationMode);
			return this;
		}

		public Builder branchMode(Object type, BranchMode branchMode) {
			this.branchModes.put(type, branchMode);
			return this;
		}

		public Builder sendMode(Object type, SendMode sendMode) {
			this.sendModes.put(type, sendMode);
			return this;
		}

		public Builder edgeWeight(Object type, EdgeWeight edgeWeight) {
			this.edgeWeights.put(type, edgeWeight);
			return this;
		}

		public Builder addAbortCondition(AbortCondition abortCondition) {
			this.abortConditions.add(abortCondition);
			return this;
		}

		public TypedSpreadingActivation create() {
			return new TypedSpreadingActivation(this);
		}

	}

	public static abstract class TypedMode<MODE> {

		private final String typePropertyKey;
		private final Map<Object, MODE> modes;
		private final MODE defaultMode;

		private TypedMode(String typePropertyKey, Map<Object, MODE> modes, MODE defaultMode) {
			this.typePropertyKey = typePropertyKey;
			this.defaultMode = defaultMode;
			this.modes = Collections.unmodifiableMap(modes);
		}

		public String getTypePropertyKey() {
			return typePropertyKey;
		}

		protected MODE getMode(Element element) {
			Property<Object> property = element.property(typePropertyKey);
			if (property.isPresent()) {
				return getMode(property.value());
			}
			return defaultMode;
		}

		public MODE getMode(Object type) {
			return modes.getOrDefault(type, defaultMode);
		}

		public Set<Object> getDefinedTypes() {
			return Collections.unmodifiableSet(modes.keySet());
		}

	}

	public static final class TypedMinimumActivationPulseInception extends TypedMode<Double> implements PulseInception {

		private TypedMinimumActivationPulseInception(String typePropertyKey, Map<Object, Double> minimumActivations) {
			super(typePropertyKey, minimumActivations, 0d);
		}

		private TypedMinimumActivationPulseInception(Builder builder) {
			this(builder.typePropertyKey, builder.minimumActivations);
		}

		@Override
		public Iterator<Vertex> startingVertices(Context context) {
			String typePropertyKey = getTypePropertyKey();
			Set<Object> definedTypes = getDefinedTypes();
			@SuppressWarnings("rawtypes")
			Traversal[] traversals = new Traversal[definedTypes.size()];
			int i = 0;
			for (Object type : definedTypes) {
				Double minimumActivation = getMode(type);
				traversals[i++] = __.has(typePropertyKey, type).has(context.vertexActivationKey(context.pulse() - 1),
						P.gte(minimumActivation));
			}
			return context.traversal().V().filter(__.or(traversals));
		}

		public static Builder build(String typePropertyKey) {
			return new Builder(typePropertyKey);
		}

		public static class Builder {

			private String typePropertyKey;
			private Map<Object, Double> minimumActivations = new HashMap<>();

			private Builder(String typePropertyKey) {
				this.typePropertyKey = typePropertyKey;
			}

			public Builder minimumActivation(Object type, Double minimumActivation) {
				this.minimumActivations.put(type, minimumActivation);
				return this;
			}

			public TypedMinimumActivationPulseInception create() {
				return new TypedMinimumActivationPulseInception(this);
			}

		}

	}

	public static final class TypedActivationMode extends TypedMode<ActivationMode> implements ActivationMode {

		private TypedActivationMode(String typePropertyKey, Map<Object, ActivationMode> activationModes) {
			super(typePropertyKey, activationModes, ActivationMode.Default.IDENTITY);
		}

		@Override
		public double activation(Context context, Vertex vertex, double x) {
			return getMode(vertex).activation(context, vertex, x);
		}

	}

	public static final class TypedAttenuationMode extends TypedMode<AttenuationMode> implements AttenuationMode {

		private TypedAttenuationMode(String typePropertyKey, Map<Object, AttenuationMode> attenuationModes) {
			super(typePropertyKey, attenuationModes, AttenuationMode.Default.IGNORE);
		}

		@Override
		public double attenuation(Context context, Vertex vertex) {
			return getMode(vertex).attenuation(context, vertex);
		}

	}

	public static final class TypedBranchMode extends TypedMode<BranchMode> implements BranchMode {

		private TypedBranchMode(String typePropertyKey, Map<Object, BranchMode> branchModes) {
			super(typePropertyKey, branchModes, BranchMode.Default.NONE);
		}

		@Override
		public double branch(Context context, Vertex vertex) {
			return getMode(vertex).branch(context, vertex);
		}
	}

	public static final class TypedSendMode extends TypedMode<SendMode> implements SendMode {

		private TypedSendMode(String typePropertyKey, Map<Object, SendMode> sendModes) {
			super(typePropertyKey, sendModes, SendMode.Default.BASIC);
		}

		@Override
		public GraphTraversal<?, Edge> allowedEdges(Context context, Vertex vertex) {
			return getMode(vertex).allowedEdges(context, vertex);
		}

	}

	public static final class TypedEdgeWeight extends TypedMode<EdgeWeight> implements EdgeWeight {

		private TypedEdgeWeight(String typePropertyKey, Map<Object, EdgeWeight> edgeWeights) {
			super(typePropertyKey, edgeWeights, EdgeWeight.Default.CONSTANT);
		}

		@Override
		public double edgeWeight(Context context, Edge edge, boolean withDirection) {
			return getMode(edge).edgeWeight(context, edge, withDirection);
		}

	}

}
