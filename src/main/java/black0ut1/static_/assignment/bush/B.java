package black0ut1.static_.assignment.bush;

import black0ut1.data.*;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Quadruplet;
import black0ut1.static_.assignment.Settings;

import java.util.Arrays;

public class B extends BushBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	protected static final double FLOW_EPSILON = 1e-10;
	
	protected final int[][] topologicalOrders = new int[network.zones][];
	protected double bushRelativeGap = 0;
	
	public B(Settings settings) {
		super(settings);
		for (int i = 0; i < network.zones; i++)
			topologicalOrders[i] = new int[network.nodes];
	}
	
	
	@Override
	protected void initialize() {
		super.initialize();
		
		for (Bush bush : bushes)
			updateTopologicalOrder(bush);
	}
	
	@Override
	protected void mainLoopIteration() {
		// add some edges to bush, but maintain bush acyclicity
		for (Bush bush : bushes) {
			improveBush(bush);
			updateTopologicalOrder(bush);
		}
		
		
		// equilibriate all bushes 20x times
		// if bush is equilibriated enough in some iteration, it is skipped for the rest of iterations
		// if all bushes are equilibriated enough, we continue with next step
		boolean[] updateBush = new boolean[network.zones];
		Arrays.fill(updateBush, true);
		for (int i = 0; i < 20; i++) {
			int bushesDone = 0;
			
			for (Bush bush : bushes) {
				if (!updateBush[bush.root])
					continue;
				
				// get trees of minimal and maximal paths to each node
				var quadruplet = getTrees(bush, LongestPathPolicy.USED);
				
				if (isEquilibriated(bush, quadruplet.third())) {
					updateBush[bush.root] = false;
					continue;
				}
				
				equilibriateBush(bush, quadruplet.first(), quadruplet.second());
				bushesDone++;
			}
			
			System.out.println("Inner iteration " + i + ", bushes equilibriated: " +
					(network.zones - bushesDone) + "/" + network.zones);
			
			if (bushesDone == 0 || iteration == 0)
				break;
		}
		
		
		// computation of relative gap (sspt / tstt - 1)
		System.out.print("Bush relative gap: ");
		double sptt = 0;
		for (Bush bush : bushes) {
			double[] minDistance = getTrees(bush, LongestPathPolicy.NONE).third();
			
			for (int destination = 0; destination < network.zones; destination++) {
				if (odm.get(bush.root, destination) == 0)
					continue;
				
				sptt += odm.get(bush.root, destination) * minDistance[destination];
			}
		}
		
		double tstt = 0;
		for (int i = 0; i < network.edges; i++) {
			tstt += flows[i] * costs[i];
		}
		
		bushRelativeGap = tstt / sptt - 1;
		System.out.printf("%.10f\n", bushRelativeGap);
		
		
		// remove arcs with zero flow
		for (Bush bush : bushes) {
			Network.Edge[] minTree = getTrees(bush, LongestPathPolicy.NONE).first();
			removeUnusedArcs(bush, minTree);
			updateTopologicalOrder(bush);
		}
	}
	
	@Override
	protected void equilibrateBush(Bush bush) {}
	
	protected void equilibriateBush(Bush bush, Network.Edge[] minTree, Network.Edge[] maxTree) {
		int[] divergenceNodes = findDivergenceNodes(minTree, maxTree, bush.root);
		for (int node = 0; node < network.nodes; node++) {
			
			// find the divergence node, where the min. and max. path to node are no longer the same
			int divNode = divergenceNodes[node];
			if (divNode == -1)
				continue;
			
			// find the amount of flow that will be shifted from max. path segment to min. path segment
			double deltaX = findFlowDelta(minTree, maxTree, bush, node, divNode);
			if (deltaX == 0)
				continue;
			
			// shift the flow from max. path segment to min. path segment
			shiftFlows(minTree, maxTree, bush, node, divNode, deltaX);
		}
		updateCosts();
	}
	
	protected boolean isEquilibriated(Bush bush, double[] minTreeDistance) {
		double bushSPTT = 0;
		for (int i = 0; i < network.zones; i++) {
			if (odm.get(bush.root, i) == 0)
				continue;
			
			bushSPTT += odm.get(bush.root, i) * minTreeDistance[i];
		}
		
		double bushRCTT = 0;
		for (int ij = 0; ij < network.edges; ij++) {
			int i = network.getEdges()[ij].tail;
			int j = network.getEdges()[ij].head;
			if (minTreeDistance[i] == Double.POSITIVE_INFINITY
					|| minTreeDistance[j] == Double.POSITIVE_INFINITY)
				continue;
			
			bushRCTT += bush.getEdgeFlow(ij) * (minTreeDistance[i] + costs[ij] - minTreeDistance[j]);
		}
		
		if (bushSPTT == 0) // case when all flow from bush.root is 0 in the ODM
			return true;
		
		return bushRCTT / bushSPTT < 0.25 * bushRelativeGap;
	}
	
	protected void improveBush(Bush bush) {
		double[] maxDistance = getTrees(bush, LongestPathPolicy.DEFAULT).fourth();
		
		for (Network.Edge edge : network.getEdges()) {
			if (maxDistance[edge.tail] == Double.NEGATIVE_INFINITY || maxDistance[edge.head] == Double.NEGATIVE_INFINITY)
				continue;
			
			if (maxDistance[edge.tail] < maxDistance[edge.head]) {
				bush.addEdge(edge.index);
			}
		}
	}
	
	protected Quadruplet<Network.Edge[], Network.Edge[], double[], double[]>
	getTrees(Bush bush, LongestPathPolicy policy) {
		int[] order = topologicalOrders[bush.root];
		
		double[] minTreeDistance = new double[network.nodes];
		Arrays.fill(minTreeDistance, Double.POSITIVE_INFINITY);
		minTreeDistance[bush.root] = 0;
		
		Network.Edge[] minTreePrevious = new Network.Edge[network.nodes];
		
		double[] maxTreeDistance = new double[network.nodes];
		Arrays.fill(maxTreeDistance, Double.NEGATIVE_INFINITY);
		maxTreeDistance[bush.root] = 0;
		
		Network.Edge[] maxTreePrevious = new Network.Edge[network.nodes];
		
		
		for (int node : order) {
			if (node == -1)
				continue;
			
			for (Network.Edge edge : network.forwardStar(node)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				double newDistance = minTreeDistance[node] + costs[edge.index];
				if (minTreeDistance[edge.head] > newDistance) {
					minTreeDistance[edge.head] = newDistance;
					minTreePrevious[edge.head] = edge;
				}
				
				// we dont care for longest path
				if (policy == LongestPathPolicy.NONE)
					continue;
				
				newDistance = maxTreeDistance[node] + costs[edge.index];
				if (maxTreeDistance[edge.head] < newDistance &&
						// longest distance is the only criterion
						(policy == LongestPathPolicy.DEFAULT
								// edges of longest path must contain flow
								|| (policy == LongestPathPolicy.USED && bush.getEdgeFlow(edge.index) > 0)
								// edges of longest path must contain flow or must be part of mintree
								|| (policy == LongestPathPolicy.USED_OR_SP && (bush.getEdgeFlow(edge.index) > 0 || edge == minTreePrevious[edge.head])))) {
					maxTreeDistance[edge.head] = newDistance;
					maxTreePrevious[edge.head] = edge;
				}
			}
		}
		
		return new Quadruplet<>(
				minTreePrevious, maxTreePrevious,
				minTreeDistance, maxTreeDistance);
	}
	
	protected void updateTopologicalOrder(Bush bush) {
		int[] indegree = new int[network.nodes];
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			indegree[edge.head]++;
		}
		
		int[] topologicalOrder = topologicalOrders[bush.root];
		Arrays.fill(topologicalOrder, -1);
		int counter = 0;
		
		IntQueue queue = new IntQueue(network.nodes);
		queue.enqueue(bush.root);
		while (!queue.isEmpty()) {
			
			int startNode = queue.dequeue();
			topologicalOrder[counter++] = startNode;
			
			for (Network.Edge edge : network.forwardStar(startNode)) {
				if (!bush.edgeExists(edge.index))
					continue;
				
				indegree[edge.head]--;
				if (indegree[edge.head] == 0)
					queue.enqueue(edge.head);
			}
		}
		
		for (int i : topologicalOrder) {
			assert odm.get(bush.root, i) == 0;
		}
	}
	
	protected int findDivergenceNode(Network.Edge[] minTree, Network.Edge[] maxTree,
									 int node, double[] minDist) {
		Network.Edge ijMin = minTree[node];
		Network.Edge ijMax = maxTree[node];
		
		if (ijMin == null || ijMax == null)
			return -1;
		if (ijMin.tail == ijMax.tail)
			return -1;
		
		while (ijMin.tail != ijMax.tail) {
			
			if (minDist[ijMax.tail] == minDist[ijMin.tail]) {
				ijMax = maxTree[ijMax.tail];
				ijMin = minTree[ijMin.tail];
			}
			
			while (minDist[ijMin.tail] < minDist[ijMax.tail]) {
				ijMax = maxTree[ijMax.tail];
			}
			
			while (minDist[ijMax.tail] < minDist[ijMin.tail]) {
				ijMin = minTree[ijMin.tail];
			}
		}
		
		return ijMin.tail;
	}
	
	protected int[] findDivergenceNodes(Network.Edge[] minTree, Network.Edge[] maxTree, int root) {
		int[] divNodes = new int[network.nodes];
		Arrays.fill(divNodes, -1);
		
		int[] mark = new int[network.nodes];
		Arrays.fill(mark, -1);
		
		for (int node = 0; node < network.nodes; node++) {
			Network.Edge ijMin = minTree[node];
			Network.Edge ijMax = maxTree[node];
			
			if (ijMin == null || ijMax == null || ijMin == ijMax) {
				divNodes[node] = -1;
				continue;
			}
			
			int SPnode = ijMin.tail;
			int LPnode = ijMax.tail;
			while (true) {
				if (SPnode == root || LPnode == root) {
					divNodes[node] = root;
					break;
				}
				
				SPnode = minTree[SPnode].tail;
				if (mark[SPnode] == node) {
					divNodes[node] = SPnode;
					break;
				}
				mark[SPnode] = node;
				
				LPnode = maxTree[LPnode].tail;
				if (mark[LPnode] == node) {
					divNodes[node] = LPnode;
					break;
				}
				mark[LPnode] = node;
			}
		}
		
		return divNodes;
	}
	
	protected double findFlowDelta(Network.Edge[] minTree, Network.Edge[] maxTree,
								   Bush bush, int node, int divNode) {
		
		double maxDeltaX = Double.POSITIVE_INFINITY;
		Network.Edge edge = maxTree[node];
		while (edge != null && edge.head != divNode) {
			if (maxDeltaX > bush.getEdgeFlow(edge.index))
				maxDeltaX = bush.getEdgeFlow(edge.index);
			
			edge = maxTree[edge.tail];
		}
		
		if (maxDeltaX == 0)
			return 0;
		
		double deltaX = 0;
		for (int i = 0; i < NEWTON_MAX_ITERATIONS; i++) {
			
			double minPathFlow = 0;
			double minPathFlowDerivative = 0;
			edge = minTree[node];
			while (edge != null && edge.head != divNode) {
				minPathFlow += costFunction.function(edge, flows[edge.index] + deltaX);
				minPathFlowDerivative += costFunction.derivative(edge, flows[edge.index] + deltaX);
				
				edge = minTree[edge.tail];
			}
			
			
			double maxPathFlow = 0;
			double maxPathFlowDerivative = 0;
			edge = maxTree[node];
			while (edge != null && edge.head != divNode) {
				maxPathFlow += costFunction.function(edge, flows[edge.index] - deltaX);
				maxPathFlowDerivative += costFunction.derivative(edge, flows[edge.index] - deltaX);
				
				edge = maxTree[edge.tail];
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
		while (edge != null && edge.head != lca) {
			flows[edge.index] += deltaX;
			bush.addFlow(edge.index, deltaX);
			
			edge = minTree[edge.tail];
		}
		
		edge = maxTree[node];
		while (edge != null && edge.head != lca) {
			flows[edge.index] -= deltaX;
			bush.addFlow(edge.index, -deltaX);
			
			edge = maxTree[edge.tail];
		}
	}
	
	protected void removeUnusedArcs(Bush bush, Network.Edge[] minTree) {
		
		for (int i = 0; i < network.getEdges().length; i++) {
			if (bush.getEdgeFlow(i) <= FLOW_EPSILON)
				bush.removeEdge(i);
		}
		
		for (Network.Edge edge : minTree) {
			if (edge != null)
				bush.addEdge(edge.index);
		}
	}
	
	private void printBush(Bush bush) {
		for (Network.Edge edge : network.getEdges()) {
			if (!bush.edgeExists(edge.index))
				continue;
			
			System.out.println(edge.tail + " " + edge.head);
		}
	}
	
	protected enum LongestPathPolicy {
		NONE,
		DEFAULT,
		USED,
		USED_OR_SP
	}
}
