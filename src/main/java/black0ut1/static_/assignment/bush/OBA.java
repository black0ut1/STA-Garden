package black0ut1.static_.assignment.bush;

import black0ut1.data.*;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.data.tuple.Triplet;
import black0ut1.static_.assignment.Settings;

import java.util.Arrays;
import java.util.Vector;

public class OBA extends BushBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	public OBA(Settings settings) {
		super(settings);
	}
	
	
	@Override
	protected void mainLoopIteration() {
		for (Bush bush : bushes) {
			
			improveBush(bush);
			
			int[] indegree = indegree(bush);
			var a = getNodeFlowsAndAlpha(bush, indegree);
			double[] nFlows = a.first();
			double[] alpha = a.second();
			
			var labels = getLabels(bush, indegree, alpha);
			double[] meanDistance = labels.first();
			double[] derivativeDistance = labels.second();
			int[] topologicalOrder1 = labels.third().first(); // startNode -> order
			int[] topologicalOrder2 = labels.third().second(); // order -> startNode
			
			for (int node = 0; node < network.nodes; node++) {
				
				Network.Edge basicApproach = findBasicApproach(bush, node, meanDistance);
				if (basicApproach == null)
					continue;
				
				var pair = getPaths(node, basicApproach, topologicalOrder1, bush);
				var basicPath = pair.first();
				var nonbasicPaths = pair.second();
				
				for (Vector<Network.Edge> nonbasicPath : nonbasicPaths) {
					if (nonbasicPath.isEmpty())
						continue;
					
					double deltaX = getFlowDelta(nonbasicPath.getFirst(),
							basicPath.getFirst(), bush, meanDistance, derivativeDistance);
					
					shiftFlows(basicPath, nonbasicPath, deltaX, bush);
				}
				
				updateAlpha(alpha, nFlows, node, bush);
				
				updateNodes(topologicalOrder2, nFlows, alpha, bush);
				
				for (int i = 0; i < network.getEdges().length; i++) {
					if (bush.getEdgeFlow(i) <= 0)
						bush.removeEdge(i);
				}
			}
			
			updateCosts();
		}
	}
	
	@Override
	protected void equilibrateBush(Bush bush) {}
	
	protected void updateNodes(int[] topOrd2, double[] nFlows, double[] alpha, Bush bush) {
		for (int i = network.nodes - 1; i >= 0; i--) {
			int n = topOrd2[i];
			
			nFlows[n] = odm.get(bush.root, n);
			for (Network.Edge edge : network.forwardStar(n)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				nFlows[n] += bush.getEdgeFlow(edge.index);
			}
			
			for (Network.Edge edge : network.backwardStar(n)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				bush.addFlow(edge.index, alpha[edge.index] * nFlows[n] - bush.getEdgeFlow(edge.index));
				flows[edge.index] += alpha[edge.index] * nFlows[n] - bush.getEdgeFlow(edge.index);
			}
		}
	}
	
	protected void updateAlpha(double[] alpha, double[] nFlows, int node, Bush bush) {
		for (Network.Edge edge : network.backwardStar(node)) {
			if (!bush.edgeExists(edge.index))
				continue;
			
			alpha[edge.index] = bush.getEdgeFlow(edge.index) / nFlows[edge.head];
		}
	}
	
	protected void shiftFlows(Vector<Network.Edge> basicPath,
							  Vector<Network.Edge> nonbasicPath,
							  double deltaX, Bush bush) {
		int basicApproachIndex = basicPath.getFirst().index;
		int nonbasicApproachIndex = nonbasicPath.getFirst().index;
		
		bush.addFlow(basicApproachIndex, deltaX);
		flows[basicApproachIndex] += deltaX;
		bush.addFlow(nonbasicApproachIndex, -deltaX);
		flows[nonbasicApproachIndex] -= deltaX;
	}
	
	protected Network.Edge findBasicApproach(Bush bush, int node, double[] meanDistance) {
		Network.Edge basic = null;
		
		double basicDist = Double.POSITIVE_INFINITY;
		for (Network.Edge edge : network.backwardStar(node)) {
			if (!bush.edgeExists(edge.index))
				continue;
			
			double dist = meanDistance[node] + costs[edge.index];
			if (dist < basicDist) {
				basic = edge;
				basicDist = dist;
			}
		}
		
		return basic;
	}
	
	protected Pair<Vector<Network.Edge>, Vector<Network.Edge>[]> getPaths(
			int node, Network.Edge basicApproach, int[] topologicalOrder, Bush bush) {
		var approaches = network.backwardStar(node);
		
		// edges of basic path from root to node (reversed)
		Vector<Network.Edge> basicPath = new Vector<>();
		
		// edges of nonbasic paths from root to node (reversed)
		Vector<Network.Edge>[] nonbasicPaths = new Vector[approaches.size()];
		for (int i = 0; i < nonbasicPaths.length; i++)
			nonbasicPaths[i] = new Vector<>();
		
		// create basic path
		for (Network.Edge edge = basicApproach; ; ) {
			basicPath.add(edge);
			
			Network.Edge next = null;
			int minOrder = Integer.MAX_VALUE;
			for (Network.Edge edge1 : network.backwardStar(edge.tail)) {
				if (!bush.edgeExists(edge1.index))
					continue;
				
				if (topologicalOrder[edge1.tail] < minOrder) {
					next = edge1;
					minOrder = topologicalOrder[edge1.tail];
				}
			}
			
			if (next == null)
				break;
			
			edge = next;
		}
		
		// create nonbasic paths
		int i = 0;
		for (Network.Edge nonBasicApproach : approaches) {
			if (!bush.edgeExists(nonBasicApproach.index) || nonBasicApproach == basicApproach)
				continue;
			
			var nonbasicPath = nonbasicPaths[i++];
			
			for (Network.Edge edge = nonBasicApproach; ; ) {
				nonbasicPath.add(edge);
				
				Network.Edge next = null;
				int minOrder = Integer.MAX_VALUE;
				for (Network.Edge edge1 : network.backwardStar(edge.tail)) {
					if (!bush.edgeExists(edge1.index))
						continue;
					
					if (topologicalOrder[edge1.tail] < minOrder) {
						next = edge1;
						minOrder = topologicalOrder[edge1.tail];
					}
				}
				
				if (next == null)
					break;
				
				edge = next;
			}
		}
		
		return new Pair<>(basicPath, nonbasicPaths);
	}
	
	protected double getFlowDelta(Network.Edge nonbasic, Network.Edge basic, Bush bush,
								  double[] meanDistance, double[] derivativeDistance) {
		
		double deltaX = 0;
		for (int i = 0; i < NEWTON_MAX_ITERATIONS; i++) {
			double M_Basic = meanDistance[basic.tail] +
					costFunction.function(nonbasic, flows[nonbasic.index] + deltaX);
			double M_Nonbasic = meanDistance[nonbasic.tail] +
					costFunction.function(nonbasic, flows[nonbasic.index] - deltaX);
			
			double D_Basic = derivativeDistance[basic.tail] +
					costFunction.derivative(nonbasic, flows[nonbasic.index] + deltaX);
			double D_Nonbasic = derivativeDistance[nonbasic.tail] +
					costFunction.derivative(nonbasic, flows[nonbasic.index] - deltaX);
			
			double newDeltaX = deltaX - (M_Nonbasic - M_Basic) / (D_Nonbasic + D_Basic);
			
			if (Math.abs(deltaX - newDeltaX) < NEWTON_EPSILON) {
				deltaX = newDeltaX;
				break;
			} else
				deltaX = newDeltaX;
		}
		
		return Math.min(deltaX, bush.getEdgeFlow(nonbasic.index));
	}
	
	protected Pair<double[], double[]> getNodeFlowsAndAlpha(Bush bush, int[] indegree) {
		double[] nFlows = new double[network.nodes];
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			
			nFlows[edge.head] += bush.getEdgeFlow(edge.index);
		}
		
		double[] alpha = new double[network.edges];
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			
			alpha[edge.index] = (nFlows[edge.head] != 0)
					? bush.getEdgeFlow(edge.index) / nFlows[edge.head]
					: 1.0 / indegree[edge.head];
		}
		
		return new Pair<>(nFlows, alpha);
	}
	
	protected Triplet<double[], double[], Pair<int[], int[]>> getLabels(Bush bush, int[] indegree, double[] alpha) {
		double[] meanDistance = new double[network.nodes];
		double[] derivativeDistance = new double[network.nodes];
		
		int[] topOrder1 = new int[network.nodes];
		int[] topOrder2 = new int[network.nodes];
		int i = 0;
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		while (!queue.isEmpty()) {
			int startNode = queue.dequeue();
			
			topOrder1[startNode] = i;
			topOrder2[i] = startNode;
			i++;
			
			for (Network.Edge edge : network.forwardStar(startNode)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				double edgeDistance = meanDistance[startNode] + costs[edge.index];
				meanDistance[edge.head] += alpha[edge.index] * edgeDistance;
				
				double edgeDerivativeDistance = derivativeDistance[startNode] +
						costFunction.derivative(edge, flows[edge.index]);
				derivativeDistance[edge.head] += alpha[edge.index] * alpha[edge.index] * edgeDerivativeDistance;
				
				indegree[edge.head]--;
				if (indegree[edge.head] == 0)
					queue.enqueue(edge.head);
			}
		}
		
		return new Triplet<>(meanDistance, derivativeDistance, new Pair<>(topOrder1, topOrder2));
	}
	
	protected void improveBush(Bush bush) {
		double[] maxDistance = getMaxDistance(bush);
		
		for (Network.Edge edge : network.getEdges()) {
			if (maxDistance[edge.tail] == Double.NEGATIVE_INFINITY || maxDistance[edge.head] == Double.NEGATIVE_INFINITY)
				continue;
			
			if (maxDistance[edge.tail] < maxDistance[edge.head]) {
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
			
			for (Network.Edge edge : network.forwardStar(startNode)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				double newDistance = maxTreeDistance[startNode] + costs[edge.index];
				if (maxTreeDistance[edge.head] < newDistance) {
					maxTreeDistance[edge.head] = newDistance;
				}
				
				indegree[edge.head]--;
				if (indegree[edge.head] == 0)
					queue.enqueue(edge.head);
			}
		}
		
		return maxTreeDistance;
	}
	
	protected int[] indegree(Bush bush) {
		int[] indegree = new int[network.nodes];
		
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			indegree[edge.head]++;
		}
		
		return indegree;
	}
}
