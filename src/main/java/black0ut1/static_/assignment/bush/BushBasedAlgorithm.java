package black0ut1.static_.assignment.bush;

import black0ut1.data.IntQueue;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Quadruplet;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.util.SSSP;

import java.util.Arrays;


public abstract class BushBasedAlgorithm extends Algorithm {
	
	protected static final double FLOW_EPSILON = 1e-10;
	
	protected final Bush[] bushes;
	protected final BushUpdateStrategy bushUpdateStrategy = BushUpdateStrategy.DIAL;
	
	public BushBasedAlgorithm(Settings settings) {
		super(settings);
		this.bushes = new Bush[network.zones];
	}
	
	@Override
	protected void initialize() {
		for (int zone = 0; zone < bushes.length; zone++)
			bushes[zone] = createBush(zone);
		
		for (Bush bush : bushes)
			for (Network.Edge edge : network.getEdges())
				flows[edge.index] += bush.getEdgeFlow(edge.index);
		
		updateCosts();
	}
	
	protected Bush createBush(int origin) {
		Bush bush = new Bush(network.edges, origin);
		
		var pair = SSSP.dijkstra(network, origin, costs);
		Network.Edge[] minimalTree = pair.first();
		double[] minimalDistance = pair.second();
		
		if (bushUpdateStrategy == BushUpdateStrategy.DIAL) {
			
			for (Network.Edge edge : network.getEdges())
				if (minimalDistance[edge.tail] < minimalDistance[edge.head])
					bush.addEdge(edge.index);
		} else {
			
			for (Network.Edge edge : minimalTree)
				bush.addEdge(edge.index);
		}
		
		for (int destination = 0; destination < network.zones; destination++) {
			double trips = odm.get(origin, destination);
			if (trips == 0)
				continue;
			
			for (Network.Edge edge = minimalTree[destination];
				 edge != null;
				 edge = minimalTree[edge.tail]) {
				bush.addFlow(edge.index, trips);
			}
		}
		
		return bush;
	}
	
	@Override
	protected void mainLoopIteration() {
		for (Bush bush : bushes) {
			updateBush(bush);
			equilibrateBush(bush);
		}
		
		for (int i = 0; i < 10; i++) {
			for (Bush bush : bushes) {
				equilibrateBush(bush);
			}
		}
	}
	
	protected abstract void equilibrateBush(Bush bush);
	
	protected void updateBush(Bush bush) {
		// Dial's strategy is different from Bargera's and Nie's
		if (bushUpdateStrategy == BushUpdateStrategy.DIAL) {
			double[] minimalDistance = getTrees(bush, true, false, false).third();
			
			for (Network.Edge edge : network.getEdges()) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				Network.Edge mirror = network.mirrorEdgeOf(edge.index);
				if (minimalDistance[edge.tail] + costs[edge.index] < minimalDistance[edge.head]) {
					bush.removeEdge(edge.index);
					bush.addEdge(mirror.index);
				}
			}
			
			return;
		}
		
		var q = getTrees(bush, true, true, false);
		Network.Edge[] minimalTree = q.first();
		double[] minimalDistance = q.third();
		double[] maximalDistance = q.fourth();
		
		// remove unused links but maintain connectedness
		for (int i = 0; i < network.edges; i++)
			if (bush.getEdgeFlow(i) <= FLOW_EPSILON)
				bush.removeEdge(i);
		for (Network.Edge edge : minimalTree)
			if (edge != null)
				bush.addEdge(edge.index);
		
		if (bushUpdateStrategy == BushUpdateStrategy.BARGERA) {
			
			for (Network.Edge edge : network.getEdges())
				if (maximalDistance[edge.tail] < maximalDistance[edge.head])
					bush.addEdge(edge.index);
			return;
		}
		
		if (bushUpdateStrategy == BushUpdateStrategy.NIE) {
			
			int linksAdded = 0;
			for (Network.Edge edge : network.getEdges()) {
				
				if (minimalDistance[edge.tail] == Double.POSITIVE_INFINITY
						|| minimalDistance[edge.head] == Double.POSITIVE_INFINITY) {
					continue;
				}
				
				if (maximalDistance[edge.tail] + costs[edge.index] < maximalDistance[edge.head]
						&& minimalDistance[edge.tail] + costs[edge.index] < minimalDistance[edge.head]) {
					bush.addEdge(edge.index);
					linksAdded++;
				}
			}
			
			// fallback to Bargera's strategy
			if (linksAdded == 0)
				for (Network.Edge edge : network.getEdges()) {
					
					if (minimalDistance[edge.tail] == Double.POSITIVE_INFINITY
							|| minimalDistance[edge.head] == Double.POSITIVE_INFINITY) {
						continue;
					}
					
					if (maximalDistance[edge.tail] + costs[edge.index] < maximalDistance[edge.head])
						bush.addEdge(edge.index);
				}
		}
	}
	
	protected Quadruplet<Network.Edge[], Network.Edge[], double[], double[]>
	getTrees(Bush bush, boolean minimalPath, boolean maximalPath, boolean maximalUsed) {
		Network.Edge[] minimalTree = null, maximalTree = null;
		double[] minimalDistance = null, maximalDistance = null;
		
		if (minimalPath) {
			minimalTree = new Network.Edge[network.nodes];
			
			minimalDistance = new double[network.nodes];
			Arrays.fill(minimalDistance, Double.POSITIVE_INFINITY);
			minimalDistance[bush.root] = 0;
		}
		
		if (maximalPath) {
			maximalTree = new Network.Edge[network.nodes];
			
			maximalDistance = new double[network.nodes];
			Arrays.fill(maximalDistance, Double.NEGATIVE_INFINITY);
			maximalDistance[bush.root] = 0;
		}
		
		int[] indegree = new int[network.nodes];
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			indegree[edge.head]++;
		}
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		while (!queue.isEmpty()) {
			int node = queue.dequeue();
			
			for (Network.Edge edge : network.forwardStar(node)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				if (minimalPath) {
					double newDistance = minimalDistance[node] + costs[edge.index];
					if (minimalDistance[edge.head] > newDistance) {
						minimalDistance[edge.head] = newDistance;
						minimalTree[edge.head] = edge;
					}
				}
				
				if (maximalPath) {
					double newDistance = maximalDistance[node] + costs[edge.index];
					if (maximalDistance[edge.head] < newDistance && (!maximalUsed || bush.getEdgeFlow(edge.index) > 0)) {
						maximalDistance[edge.head] = newDistance;
						maximalTree[edge.head] = edge;
					}
				}
				
				indegree[edge.head]--;
				if (indegree[edge.head] == 0)
					queue.enqueue(edge.head);
			}
		}
		
		return new Quadruplet<>(minimalTree, maximalTree, minimalDistance, maximalDistance);
	}
	
	protected int[] topologicalOrder(Bush bush) {
		int[] indegree = new int[network.nodes];
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			indegree[edge.head]++;
		}
		
		int[] result = new int[network.nodes];
		Arrays.fill(result, -1);
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		int i = 0;
		while (!queue.isEmpty()) {
			int node = queue.dequeue();
			result[i++] = node;
			
			for (Network.Edge edge : network.forwardStar(node)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				indegree[edge.head]--;
				if (indegree[edge.head] == 0)
					queue.enqueue(edge.head);
			}
		}
		
		return result;
	}
	
	public Bush[] getBushes() {
		return bushes;
	}
	
	public enum BushUpdateStrategy {
		BARGERA,
		DIAL,
		NIE,
	}
}
