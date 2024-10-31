package black0ut1.sta.assignment.bush;

import black0ut1.data.*;
import black0ut1.sta.assignment.Algorithm;

import java.util.Arrays;

public class B extends BushBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	public B(Algorithm.Parameters parameters) {
		super(parameters);
	}
	
	@Override
	protected void updateFlows() {
		for (Bush bush : bushes) {
			// add some edges to bush, but maintain bush acyclicity
			improveBush(bush);
			
			// get trees of minimal and maximal paths to each node
			var trees = getTrees(bush);
			var minTree = trees.first();
			var minDist = trees.second(); // and also min. distance
			var maxTree = trees.third();
			
			for (int node = 0; node < network.nodes; node++) {
				
				// find the divergence node, where the min. and max. path to node are no longer the same
				int divNode = findDivergenceNode(minTree, maxTree, node, minDist);
				if (divNode == -1)
					continue;
				
				// find the amount of flow that will be shifted from max. path segment to min. path segment
				double deltaX = findFlowDelta(minTree, maxTree, bush, node, divNode);
				if (deltaX == 0)
					continue;
				
				// shift the flow from max. path segment to min. path segment
				shiftFlows(minTree, maxTree, bush, node, divNode, deltaX);
			}
			
			// remove arcs with zero flow
			removeUnusedArcs(bush, minTree);
			
			updateCosts();
		}
	}
	
	protected void improveBush(Bush bush) {
		double[] maxDistance = getMaxDistance(bush);
		
		for (Network.Edge edge : network.getEdges()) {
			if (maxDistance[edge.startNode] == Double.NEGATIVE_INFINITY || maxDistance[edge.endNode] == Double.NEGATIVE_INFINITY)
				continue;
			
			if (maxDistance[edge.startNode] < maxDistance[edge.endNode]) {
				bush.addEdge(edge.index);
			}
		}
	}
	
	protected double[] getMaxDistance(Bush bush) {
		int[] indegree = indegree(bush);
		
		
		double[] maxTreeDistance = new double[network.nodes];
		Arrays.fill(maxTreeDistance, Double.NEGATIVE_INFINITY);
		maxTreeDistance[bush.root] = 0;
		
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		while (!queue.isEmpty()) {
			int startNode = queue.dequeue();
			
			for (Network.Edge edge : network.neighborsOf(startNode)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				double newDistance = maxTreeDistance[startNode] + costs[edge.index];
				if (maxTreeDistance[edge.endNode] < newDistance) {
					maxTreeDistance[edge.endNode] = newDistance;
				}
				
				indegree[edge.endNode]--;
				if (indegree[edge.endNode] == 0)
					queue.enqueue(edge.endNode);
			}
		}
		
		return maxTreeDistance;
	}
	
	protected Triplet<Network.Edge[], double[], Network.Edge[]> getTrees(Bush bush) {
		int[] indegree = indegree(bush);
		
		
		double[] minTreeDistance = new double[network.nodes];
		Arrays.fill(minTreeDistance, Double.POSITIVE_INFINITY);
		minTreeDistance[bush.root] = 0;
		
		Network.Edge[] minTreePrevious = new Network.Edge[network.nodes];
		
		double[] maxTreeDistance = new double[network.nodes];
		Arrays.fill(maxTreeDistance, Double.NEGATIVE_INFINITY);
		maxTreeDistance[bush.root] = 0;
		
		Network.Edge[] maxTreePrevious = new Network.Edge[network.nodes];
		
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		while (!queue.isEmpty()) {
			int startNode = queue.dequeue();
			
			for (Network.Edge edge : network.neighborsOf(startNode)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				double newDistance = minTreeDistance[startNode] + costs[edge.index];
				if (minTreeDistance[edge.endNode] > newDistance) {
					minTreeDistance[edge.endNode] = newDistance;
					minTreePrevious[edge.endNode] = edge;
				}
				
				newDistance = maxTreeDistance[startNode] + costs[edge.index];
				if (maxTreeDistance[edge.endNode] < newDistance && bush.getEdgeFlow(edge.index) != 0) {
					maxTreeDistance[edge.endNode] = newDistance; // ^^ edge must have flow
					maxTreePrevious[edge.endNode] = edge;
				}
				
				indegree[edge.endNode]--;
				if (indegree[edge.endNode] == 0)
					queue.enqueue(edge.endNode);
			}
		}
		
		return new Triplet<>(minTreePrevious, minTreeDistance, maxTreePrevious);
	}
	
	protected int[] indegree(Bush bush) {
		int[] indegree = new int[network.nodes];
		
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			indegree[edge.endNode]++;
		}
		
		return indegree;
	}
	
	protected int findDivergenceNode(Network.Edge[] minTree, Network.Edge[] maxTree,
									 int node, double[] minDist) {
		Network.Edge ijMin = minTree[node];
		Network.Edge ijMax = maxTree[node];
		
		if (ijMin == null || ijMax == null)
			return -1;
		if (ijMin.startNode == ijMax.startNode)
			return -1;
		
		while (ijMin.startNode != ijMax.startNode) {
			
			if (minDist[ijMax.startNode] == minDist[ijMin.startNode]) {
				ijMax = maxTree[ijMax.startNode];
				ijMin = minTree[ijMin.startNode];
			}
			
			while (minDist[ijMin.startNode] < minDist[ijMax.startNode]) {
				ijMax = maxTree[ijMax.startNode];
			}
			
			while (minDist[ijMax.startNode] < minDist[ijMin.startNode]) {
				ijMin = minTree[ijMin.startNode];
			}
		}
		
		return ijMin.startNode;
	}
	
	protected double findFlowDelta(Network.Edge[] minTree, Network.Edge[] maxTree,
								   Bush bush, int node, int divNode) {
		
		double maxDeltaX = Double.POSITIVE_INFINITY;
		Network.Edge edge = maxTree[node];
		while (edge != null && edge.endNode != divNode) {
			if (maxDeltaX > bush.getEdgeFlow(edge.index))
				maxDeltaX = bush.getEdgeFlow(edge.index);
			
			edge = maxTree[edge.startNode];
		}
		
		if (maxDeltaX == 0)
			return 0;
		
		double deltaX = 0;
		for (int i = 0; i < NEWTON_MAX_ITERATIONS; i++) {
			
			double minPathFlow = 0;
			double minPathFlowDerivative = 0;
			edge = minTree[node];
			while (edge != null && edge.endNode != divNode) {
				minPathFlow += costFunction.function(edge, flows[edge.index] + deltaX);
				minPathFlowDerivative += costFunction.derivative(edge, flows[edge.index] + deltaX);
				
				edge = minTree[edge.startNode];
			}
			
			
			double maxPathFlow = 0;
			double maxPathFlowDerivative = 0;
			edge = maxTree[node];
			while (edge != null && edge.endNode != divNode) {
				maxPathFlow += costFunction.function(edge, flows[edge.index] - deltaX);
				maxPathFlowDerivative += costFunction.derivative(edge, flows[edge.index] - deltaX);
				
				edge = maxTree[edge.startNode];
			}
			
			double newDeltaX = deltaX + (maxPathFlow - minPathFlow) / (maxPathFlowDerivative + minPathFlowDerivative);
			
			if (Math.abs(deltaX - newDeltaX) < NEWTON_EPSILON) {
				deltaX = newDeltaX;
				break;
			} else
				deltaX = newDeltaX;
		}
		
		return Math.min(Math.max(deltaX, 0), maxDeltaX);
	}
	
	protected void shiftFlows(Network.Edge[] minTree, Network.Edge[] maxTree,
							  Bush bush, int node, int lca, double deltaX) {
		
		Network.Edge edge = minTree[node];
		while (edge != null && edge.endNode != lca) {
			flows[edge.index] += deltaX;
			bush.addFlow(edge.index, deltaX);
			
			edge = minTree[edge.startNode];
		}
		
		edge = maxTree[node];
		while (edge != null && edge.endNode != lca) {
			flows[edge.index] -= deltaX;
			bush.addFlow(edge.index, -deltaX);
			
			edge = maxTree[edge.startNode];
		}
	}
	
	protected void removeUnusedArcs(Bush bush, Network.Edge[] minTree) {
		
		for (int i = 0; i < network.getEdges().length; i++) {
			if (bush.getEdgeFlow(i) <= 0)
				bush.removeEdge(i);
		}
		
		for (Network.Edge edge : minTree) {
			if (edge != null)
				bush.addEdge(edge.index);
		}
	}
}
