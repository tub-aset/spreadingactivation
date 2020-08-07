# Spreading Activation

Based on http://dx.doi.org/10.14279/depositonce-8408  and http://dx.doi.org/10.14279/depositonce-7136  
Implementation for apache/tinkerpop


## Usage

* Pulse `0` refer to the initial activated vertices (i.e. no output, edge nor input activation).  
* The direction of edges does not matter, activation will be spread both ways (in back and forth direction).
* Invalid or non-existent activation values (i.e. infinity, NaN, 0) will not be considered for further calculation nor stored.
  
The API heavily uses builder pattern, including many sensible defaults. The following examples show almost all possible options available. 

### Spreading Activation  

Build a spreading activation configuration:

```java
int pulses = 4;

SpreadingActivation spreadingActivation = SpreadingActivation.build(pulses)
		.pulseInception(PulseInception.Default.MINIMUM_ACTIVATION(0.1d))
		.activationMode(ActivationMode.Default.LOG10)
		.attenuationMode(AttenuationMode.Default.FIXED(0.99d))
		.branchMode(BranchMode.Default.NONE)
		.sendMode(SpreadingMode.Default.FORWARD)
		.edgeWeight(EdgeWeight.Default.CONSTANT)
		.create();
```

Build an execution task of a spreading activation for a graph:

```java
Graph graph = TinkerFactory.createModern();
		
Execution execution = spreadingActivation.execution(graph.traversal())
		.executor(Executors.newCachedThreadPool()) // default
		.parallelTasks(Runtime.getRuntime().availableProcessors()) // default
		.propertyKeyFactory(new Execution.DefaultPropertyKeyFactory(UUID.randomUUID().toString())) // default, see Execution.PropertyKeyFactory
		.create();
```

Activate initial vertices and start execution:

```java
execution.activate(__.has("name", "marko"), 1d);
execution.run();
```

Interrupt a running execution:

```java
execution.interrupt();
```

Get the state of an execution task (always finished==true at the end, whether interrupted or not):

```java
boolean started = execution.isStarted();
boolean interrupted = execution.isInterrupted();
boolean finished = execution.isFinished();
```

Get the result of the spreading activation, including activates results, last pulse and specific activation values for a certain vertex at a certain pulse:

```java
ExecutionResult result = execution.getResult();

int lastPulse = result.pulse();
GraphTraversal<?, Vertex> activatedVertices = result.activatedVertices(lastPulse);
double activation = result.activation(vertex, pulse);
```

Remove all spreading activation properties:

```java
result.cleanup();
```

*Hint:* Those properties are necessary for the following spread graphs, cleanup last but not least.


### Spread Graphs

Build an generation task to create a spread graph from a spreading activation result:

```java
Generation generation = result.generateSpreadGraph()
		.into(TinkerGraph.open().traversal()) // default
		.startPulse(0) // default
		.endPulse(lastPulse) // default
		.propertyKeyFactory(SpreadGraph.DefaultPropertyKeyFactory.INSTANCE) // default, see SpreadGraph.PropertyKeyFactory
		.create();
```

Starting, interrupting and querying state of generation is equivalent to execution:

```java
generation.run();

generation.interrupt();

boolean started = generation.isStarted();
boolean interrupted = generation.isInterrupted();
boolean finished = generation.isFinished();
```

Interrupt a running generation:

```java
generation.interrupt();
```

Get the state of an generation task (always finished==true at the end, whether interrupted or not):

```java
boolean started = generation.isStarted();
boolean interrupted = generation.isInterrupted();
boolean finished = generation.isFinished();
```

Get the vertices of the spread graph. Start vertex do not have incoming edges (sources), end vertices have no outgoing edges (sinks).

```java
SpreadGraph spreadGraph = generation.getSpreadGraph();

GraphTraversal<Vertex,Vertex> vertices = spreadGraph.vertices(lastPulse);
GraphTraversal<Vertex,Vertex> startVertices = spreadGraph.startVertices();
GraphTraversal<Vertex,Vertex> endVertices = spreadGraph.endVertices();
```

Build a minimization task to create a spread graph for a single relevant vertex from the original spreading activation (i.e. remove all edges and vertices, which did not contribute to activate this vertex while spreading activation)

```java
Vertex relevantVertex = activatedVertices.next();

RelevantMinimization minimization = RelevantMinimization.build(spreadGraph, relevantVertex.id())
		.into(TinkerGraph.open().traversal()) // default
		.startPulse(0) // default
		.endPulse(lastPulse) // default
		.propertyKeyFactory(SpreadGraph.DefaultPropertyKeyFactory.INSTANCE) // default, see SpreadGraph.PropertyKeyFactory
		.create();
```

Starting, interrupting and querying state of minimization is equivalent to execution:

```java
minimization.run();

minimization.interrupt();

boolean started = minimization.isStarted();
boolean interrupted = minimization.isInterrupted();
boolean finished = minimization.isFinished();
```

The result of a minimization is again a spread graph.

```java
SpreadGraph minimizedSpreadGraph = minimization.getSpreadGraph();

GraphTraversal<Vertex,Vertex> vertices = minimizedSpreadGraph.vertices(lastPulse);
GraphTraversal<Vertex,Vertex> startVertices = minimizedSpreadGraph.startVertices();
GraphTraversal<Vertex,Vertex> endVertices = minimizedSpreadGraph.endVertices();
```


### Extension Points

* Spreading Activation Modes: See interfaces `ActivationMode`, `AttenuationMode`, `BranchMode`, `EdgeWeight` and `SendMode`
* Property Keys: See interface `Execution.PropertyKeyFactory` and `SpreadGraph.PropertyKeyFactory`
* Spread Graph Transformation (like `RevelantMinimization`): Extend `Transformation` (and `Transformation.Builder`)


## Current Implementation Features

* extendable configuration via interfaces (all predefined spreading activation options implemented)
* parallel execution within spreading activation calculation
* spread graph for detailed analysis of spreading activation process
* all long term processes interruptible
* activation values as double, stored in arbitrary properties (parallel execution of multiple spreading activations on a single graph)
