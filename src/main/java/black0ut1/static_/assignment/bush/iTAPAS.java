package black0ut1.static_.assignment.bush;

import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.STAConvergence;
import black0ut1.util.SSSP;

import java.util.*;

public class iTAPAS extends BushBasedAlgorithm {
	
	protected static final double FLOW_EPSILON = 1e-12;
	protected static final double COST_EFFECTIVE_FACTOR = 0.5;
	protected static final int RANDOM_SHIFTS = 400;
	
	protected final Random rng = new Random(42);
	protected final PASManager manager;
	
	public iTAPAS(Parameters parameters) {
		super(parameters);
		this.manager = new PASManager(parameters.network);
	}
	
	@Override
	protected void mainLoopIteration() {
		double minReducedCost = switch (iteration) {
			case 0:
				yield 0.1;
			case 1:
				yield 0.001;
			default:
				double convIndicator = convergence.getData().getLast()[STAConvergence.Criterion.RELATIVE_GAP_1.ordinal()];
				yield convIndicator / 100;
		};
		
		for (int zone = 0; zone < network.zones; zone++) {
			Bush bush = bushes[zone];
			
			var pair = SSSP.dijkstra(network, zone, costs);
			Network.Edge[] minTree = pair.first();
			double[] minDistance = pair.second();
			
			Network.Edge[] potentialLinks = findPotentialLinks(minTree, bush);
			for (Network.Edge edge : potentialLinks) {
				if (edge == null)
					break;
				
				if (bush.getEdgeFlow(edge.index) <= FLOW_EPSILON)
					continue;
				
				double reducedCost = minDistance[edge.startNode] + costs[edge.index] - minDistance[edge.endNode];
				if (reducedCost < minReducedCost) // TODO check performance impact of this condition
					continue;
				
				PAS found = matchPAS(edge, reducedCost);
				if (found != null) {
					shiftFlows(found);
					
					if (found.maxSegmentFlowBound(bushes) > FLOW_EPSILON
							&& found.minSegmentFlowBound(bushes) > FLOW_EPSILON)
						continue;
				}
				
				PAS newPas = MFS(edge, minTree, bush, null);
				if (newPas != null)
					manager.addPAS(newPas);
			}
			
			randomShifts();
		}
		
		eliminatePASes();
	}
	
	/* Potential link is every link in the network, which:
	 * 1) is not part of mintree from currently processed origin,
	 * 2) has nonzero (or bigger than some epsilon) origin flow,
	 * 3) has sufficently large reduced cost.
	 * Conditions 2) and 3) are checked in the main loop.
	 * - Array potentialLinks serves as sort of stack and is terminated with null.
	 */
	protected Network.Edge[] findPotentialLinks(Network.Edge[] minTree, Bush bush) {
		Network.Edge[] potentialLinks = new Network.Edge[network.edges];
		int i = 0;
		
		for (int node = 0; node < network.nodes; node++) {
			if (minTree[node] == null || node == bush.root)
				continue;
			
			for (Network.Edge edge : network.backwardStar(node)) {
				if (edge == minTree[node])
					continue;
				
				potentialLinks[i++] = edge;
			}
		}
		
		return potentialLinks;
	}
	
	/* This method iterates over the set of PASes 20x, trying
	 * to shift flows on each PAS. If PAS is equilibriated (shiftFlows
	 * shifts zero), the PAS is removed.
	 */
	protected void eliminatePASes() {
		boolean[] toBeRemoved = new boolean[manager.getCountP()];
		
		for (int i = 0; i < 20; i++) {
			
			for (int j = 0; j < manager.getCountP(); j++) {
				if (toBeRemoved[j])
					continue;
				
				PAS pas = manager.getPASes()[j];
				if (!shiftFlows(pas))
					toBeRemoved[j] = true;
			}
		}
		
		manager.removePASes(toBeRemoved);
	}
	
	/* This method tries to find existing effective PAS for a potential link.
	 * It iterates over all PASes whose head is potentialLink.endNode.
	 * The qualifications for an effective PAS are:
	 * 1) pot. link is the last link in max segment,
	 * 2) cost of max segment - cost of min segment > 0.5 * reduced cost of pot. link,
	 * 3) flow bound on max segment > 0.25 * origin flow on pot. link.
	 * - Evaluating 3) is expensive, it is better to leave it out.
	 */
	protected PAS matchPAS(Network.Edge potentialLink, double reducedCost) {
		
		for (int i = 0; i < manager.getCountPj(potentialLink.endNode); i++) {
			PAS pas = manager.getPASes(potentialLink.endNode)[i];
			
			pas.updateSegments(costs);
			if (pas.maxSegmentLastEdge() == potentialLink.index
					&& pas.segmentsCostDifference() > COST_EFFECTIVE_FACTOR * reducedCost) {
				
				// if (pas.maxSegmentFlowBound(bushes) > FLOW_EFFECTIVE_FACTOR * flow)
				return pas;
			}
		}
		
		return null;
	}
	
	/* Takes 400 random PASes and shifts flow on them. Simple as. */
	protected void randomShifts() {
		for (int i = 0; i < RANDOM_SHIFTS; i++) {
			int j = rng.nextInt(manager.getCountP());
			shiftFlows(manager.getPASes()[j]);
		}
	}
	
	//////////////////// Methods related to creating PASes ////////////////////
	
	/* These two arrays are local for MFS() method. The reason why they
	 * are extracted out is because this method is called many many times
	 * and if it would allocate these arrays each time, it would consume
	 * large amount of memory which would be taxing on garbage collector.
	 * Experiments on Sydney network shown decrease from 470GB to 25GB of
	 * allocated memory (during the whole run of the algorithm).
	 */
	protected Network.Edge[] higherCostSegment = new Network.Edge[network.nodes];
	protected int[] scanStatus = new int[network.nodes];
	
	/* Most Flow Search - Creates a new PAS, where the min segment is
	 * a segment of a min tree and max segment is found by backing up
	 * by the links with most origin flow.
	 * 0) The PAS's head is ij.endNode.
	 * 1) scanStatus of every node starts with 0.
	 * 2) Set scanStatus of nodes that are on the min path from root to
	 *    head to -1 (head excluded). Better yet, set it to -(distance
	 *    in links from head), so that when constructing PAS, we already
	 *    have the length of min segment.
	 * 3) Now the most flow search begins. Start with head and back up
	 *    along the links with most origin flow. With each step, three
	 *    things can occur:
	 *    i) scanStatus[mostFlowLink.startNode] == 0 - we encountered
	 * 		 node, which is not yet discovered, set its scanStatus to
	 * 		 1. Or better, set is to (distance from head) so that we
	 * 		 have the length of max segment when we're done.
	 *    ii) scanStatus[mostFlowLink.startNode] < 0 - min tree node
	 *      encountered (which is also the PAS's tail), we can create
	 * 		new PAS. Min segment are links on the min tree from head
	 * 		to tail, max segment are the links along which we were backing
	 * 		up in step 3).
	 * 	  iii) scanStatus[mostFlowLink.startNode] > 0 - cycle flow
	 * 		 encountered. Remove the cycle flow and restart the whole
	 * 		 method.
	 * - When postEdge is not null, the MFS procedure is slightly modified
	 *   for the purposes of postprocessing.
	 */
	protected PAS MFS(Network.Edge ij, Network.Edge[] minTree, Bush bush, Network.Edge postEdge) {
		restart:
		while (true) {
			Arrays.fill(scanStatus, 0);
			
			
			// set scanStatus of nodes on minTree path to negative numbers
			// the number indicates distance in links from ij.endNode - this
			// is useful for creating array for minSegment (it is its length)
			int count = 1;
			int start = ij.endNode;
			
			// in postprocessing, the backing up along min tree starts with start node of postEdge
			if (postEdge != null) {
				scanStatus[postEdge.startNode] = -count;
				count++;
				start = postEdge.startNode;
			}
			
			for (Network.Edge edge = minTree[start];
				 edge != null;
				 edge = minTree[edge.startNode]) {
				scanStatus[edge.startNode] = -count;
				count++;
			}
			
			
			count = 1;
			scanStatus[ij.endNode] = count++;
			
			// now we back up along incoming links with max flow until we encounter node from mintree
			int node = ij.startNode;
			Network.Edge maxIncomingLink = ij;
			while (true) {
				
				// not specified in paper, needed to avoid infinite loop
				if (bush.getEdgeFlow(maxIncomingLink.index) == 0)
					return null;
				
				// add max incoming link to the higher cost segment of PAS
				higherCostSegment[node] = maxIncomingLink;
				
				if (scanStatus[node] == 0) { // nothing happened, continuing search
					scanStatus[node] = count;
					count++;
					
				} else if (scanStatus[node] < 0) { // encountered node from mintree, PAS can now be constructed
					
					int minSegmentLen = -scanStatus[node];
					int maxSegmentLen = scanStatus[maxIncomingLink.endNode];
					
					PAS newPas = createPAS(ij, node, minSegmentLen, maxSegmentLen, minTree, bush.root, postEdge);
					
					if (postEdge == null) // we don't shift flows in postprocessing
						shiftFlows(newPas);
					
					return newPas;
					
				} else if (scanStatus[node] > 0) { // encountered node which was already searched, higherCostSegment is cycle
					
					// remove cycle flow and try all over again
					removeCycleFlow(higherCostSegment, bush, node);
					continue restart;
					
				}
				
				// find the incoming link with max flow
				maxIncomingLink = mostFlowIncomingEdge(node, bush);
				
				node = maxIncomingLink.startNode;
			}
		}
	}
	
	protected Network.Edge mostFlowIncomingEdge(int node, Bush bush) {
		double maxIncomingLinkBushFlow = Double.NEGATIVE_INFINITY;
		Network.Edge maxIncomingLink = null;
		
		for (Network.Edge incomingEdge : network.backwardStar(node)) {
			
			if (bush.getEdgeFlow(incomingEdge.index) > maxIncomingLinkBushFlow) {
				maxIncomingLinkBushFlow = bush.getEdgeFlow(incomingEdge.index);
				maxIncomingLink = incomingEdge;
			}
		}
		
		return maxIncomingLink;
	}
	
	protected PAS createPAS(Network.Edge ij, int tail, int minSegmentLen, int maxSegmentLen,
							Network.Edge[] minTree, int origin, Network.Edge postEdge) {
		int head = ij.endNode;
		
		
		int[] minSegment = new int[minSegmentLen];
		int i = minSegmentLen - 1;
		
		int start = head;
		if (postEdge != null) {
			minSegment[i--] = postEdge.index;
			start = postEdge.startNode;
		}
		if (i != -1) {
			for (Network.Edge edge = minTree[start]; ;
				 edge = minTree[edge.startNode]) {
				
				minSegment[i--] = edge.index;
				if (edge.startNode == tail)
					break;
			}
		}
		
		
		int[] maxSegment = new int[maxSegmentLen];
		i = 0;
		for (Network.Edge edge = higherCostSegment[tail]; ;
			 edge = higherCostSegment[edge.endNode]) {
			
			maxSegment[i++] = edge.index;
			if (edge.endNode == head)
				break;
		}
		maxSegment[maxSegmentLen - 1] = ij.index;
		
		
		return new PAS(minSegment, maxSegment, origin);
	}
	
	protected void removeCycleFlow(Network.Edge[] higherCostSegment, Bush bush, int cycleNode) {
		
		double minCycleFlow = Double.POSITIVE_INFINITY;
		Network.Edge cycleEdge = higherCostSegment[cycleNode];
		while (true) {
			minCycleFlow = Math.min(minCycleFlow, bush.getEdgeFlow(cycleEdge.index));
			
			if (cycleEdge.endNode == cycleNode)
				break;
			
			cycleEdge = higherCostSegment[cycleEdge.endNode];
		}
		
		cycleEdge = higherCostSegment[cycleNode];
		while (true) {
			bush.addFlow(cycleEdge.index, -minCycleFlow);
			flows[cycleEdge.index] -= minCycleFlow;
			costs[cycleEdge.index] = costFunction.function(cycleEdge, flows[cycleEdge.index]);
			
			if (cycleEdge.endNode == cycleNode)
				break;
			
			cycleEdge = higherCostSegment[cycleEdge.endNode];
		}
	}
	
	//////////////////// Methods related to shifting flows ////////////////////
	
	protected boolean shiftFlows(PAS pas) {
		pas.updateSegments(costs);
		
		double flowShift = findFlowShift(pas);
		if (flowShift <= FLOW_EPSILON)
			return false;
		
		Bush bush = bushes[pas.origin];
		var edges = network.getEdges();
		
		for (int edgeIndex : pas.minSegment()) {
			bush.addFlow(edgeIndex, flowShift);
			flows[edgeIndex] += flowShift;
			costs[edgeIndex] = costFunction.function(edges[edgeIndex], flows[edgeIndex]);
		}
		
		for (int edgeIndex : pas.maxSegment()) {
			bush.addFlow(edgeIndex, -flowShift);
			flows[edgeIndex] -= flowShift;
			costs[edgeIndex] = costFunction.function(edges[edgeIndex], flows[edgeIndex]);
		}
		
		return true;
	}
	
	protected double findFlowShift(PAS pas) {
		double maxFlowShift = pas.maxSegmentFlowBound(bushes);
		Network.Edge[] edges = network.getEdges();
		
		
		double minSegmentCostDerivative = 0;
		for (int edgeIndex : pas.minSegment()) {
			minSegmentCostDerivative += costFunction.derivative(edges[edgeIndex], flows[edgeIndex]);
		}
		
		double maxSegmentCostDerivative = 0;
		for (int edgeIndex : pas.maxSegment()) {
			maxSegmentCostDerivative += costFunction.derivative(edges[edgeIndex], flows[edgeIndex]);
		}
		
		double flowShift = pas.segmentsCostDifference() / (maxSegmentCostDerivative + minSegmentCostDerivative);
		return Math.min(flowShift, maxFlowShift);
	}
	
	////////////////////////////////////////////////////////////
	
	protected static class PASManager {
		
		private final Network network;
		
		private PAS[] P;
		private int countP = 0;
		
		private final PAS[][] Pj;
		private final int[] countsPj;
		
		public PASManager(Network network) {
			this.network = network;
			this.P = new PAS[network.edges];
			this.Pj = new PAS[network.nodes][10];
			this.countsPj = new int[network.nodes];
		}
		
		public void addPAS(PAS newPas) {
			if (countP == P.length) {
				PAS[] newArray = new PAS[(int) (P.length * 1.5)];
				System.arraycopy(P, 0, newArray, 0, P.length);
				P = newArray;
			}
			P[countP++] = newPas;
			
			int head = newPas.head(network);
			if (countsPj[head] == Pj[head].length) {
				PAS[] newArray = new PAS[(int) (Pj[head].length * 1.5)];
				System.arraycopy(Pj[head], 0, newArray, 0, Pj[head].length);
				Pj[head] = newArray;
			}
			Pj[head][countsPj[head]++] = newPas;
		}
		
		public PAS[] getPASes() {
			return P;
		}
		
		public int getCountP() {
			return countP;
		}
		
		public PAS[] getPASes(int head) {
			return Pj[head];
		}
		
		public int getCountPj(int head) {
			return countsPj[head];
		}
		
		public void removePASes(boolean[] toBeRemoved) {
			int count = 0;
			for (int i = 0; i < countP; i++) {
				if (toBeRemoved[i])
					continue;
				
				P[count++] = P[i];
			}
			countP = count;
			
			Arrays.fill(countsPj, 0);
			for (int i = 0; i < countP; i++) {
				int head = P[i].head(network);
				Pj[head][countsPj[head]++] = P[i];
			}
		}
	}
	
	/**
	 * Pair of Alternative Segments.
	 * A segment is a sequence of consecutive edges - essentially a
	 * path, but connecting any two nodes, not just origin and destination.
	 * A pair of alternative segments (PAS) is exactly what it means,
	 * but with three conditions:
	 * 1) both segments start in the same node - the tail,
	 * 2) both segments end in the same node - the head,
	 * 3) the segments do not share any edges and any nodes, apart from
	 *    tail and head.
	 * PAS also has an origin associated with it.
	 * <p>
	 * When PAS is created, one of the segments (minSegment) has lesser
	 * cost than the other (maxSegment). However, these roles can switch
	 * as the algorithm shift flows. That's why there is field minSegmentIndex
	 * which point to the minSegment in array segments. To update the
	 * roles, call updateSegments(), which computes the costs of both
	 * segments and compares them to determine the roles.
	 */
	protected static class PAS {
		
		public int origin;
		private int minSegmentIndex = 0;
		private final int[][] segments;
		private final double[] costs;
		
		public PAS(int[] minSegment, int[] maxSegment, int origin) {
			this.origin = origin;
			this.segments = new int[2][];
			this.costs = new double[2];
			
			segments[minSegmentIndex] = minSegment;
			segments[1 - minSegmentIndex] = maxSegment;
		}
		
		public int[] minSegment() {
			return segments[minSegmentIndex];
		}
		
		public int[] maxSegment() {
			return segments[1 - minSegmentIndex];
		}
		
		public int minSegmentLastEdge() {
			return minSegment()[minSegment().length - 1];
		}
		
		public int maxSegmentLastEdge() {
			return maxSegment()[maxSegment().length - 1];
		}
		
		public double minSegmentFlowBound(Bush[] bushes) {
			Bush bush = bushes[origin];
			
			double flowBound = Double.POSITIVE_INFINITY;
			for (int i : minSegment())
				flowBound = Math.min(flowBound, bush.getEdgeFlow(i));
			
			return flowBound;
		}
		
		public double maxSegmentFlowBound(Bush[] bushes) {
			Bush bush = bushes[origin];
			
			double flowBound = Double.POSITIVE_INFINITY;
			for (int i : maxSegment())
				flowBound = Math.min(flowBound, bush.getEdgeFlow(i));
			
			return flowBound;
		}
		
		public void updateSegments(double[] costs) {
			this.costs[0] = 0;
			for (int i : segments[0]) {
				this.costs[0] += costs[i];
			}
			
			this.costs[1] = 0;
			for (int i : segments[1]) {
				this.costs[1] += costs[i];
			}
			
			minSegmentIndex = (this.costs[0] < this.costs[1]) ? 0 : 1;
		}
		
		public double segmentsCostDifference() {
			return costs[1 - minSegmentIndex] - costs[minSegmentIndex];
		}
		
		public int head(Network network) {
			return network.getEdges()[minSegmentLastEdge()].endNode;
		}
	}
}
