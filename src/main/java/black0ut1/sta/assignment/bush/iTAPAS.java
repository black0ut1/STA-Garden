package black0ut1.sta.assignment.bush;

import black0ut1.data.Bush;
import black0ut1.data.Network;
import black0ut1.data.Pair;
import black0ut1.sta.Convergence;
import black0ut1.util.SSSP;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("unchecked")
public class iTAPAS extends BushBasedAlgorithm {
	
	protected static final double FLOW_EPSILON = 1e-12;
	protected static final double COST_EFFECTIVE_FACTOR = 0.5;
	protected static final int RANDOM_SHIFTS = 400;
	protected static final double POSTPROCESS_FLOW_PRECISION = 1e-6;
	
	private final boolean doPostProcess;
	
	protected final Random rng = new Random(42);
	protected final PASManager manager;
	
	/* Arrays specific for MFS method, extracted outside of the method
	 * to avoid unnecessary allocation.
	 */
	protected Network.Edge[] higherCostSegment = new Network.Edge[network.nodes];
	protected int[] scanStatus = new int[network.nodes];
	
	public iTAPAS(Parameters parameters, boolean doPostProcess) {
		super(parameters);
		this.doPostProcess = doPostProcess;
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
				double convIndicator = convergence.getData().getLast()[Convergence.Criterion.RELATIVE_GAP_1.ordinal()];
				yield convIndicator / 100;
		};
		
		for (int zone = 0; zone < network.zones; zone++) {
			Bush bush = bushes[zone];
			
			var pair = SSSP.minTree(network, zone, costs);
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
			
			for (Network.Edge edge : network.incomingOf(node)) {
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
		for (int i = 0; i < 20; i++) {
			
			for (Iterator<PAS> iterator = manager.getPASes().iterator(); iterator.hasNext(); ) {
				PAS pas = iterator.next();
				
				if (!shiftFlows(pas)) {
					manager.removePAS(iterator, pas);
				}
			}
		}
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
		
		for (PAS pas : manager.getPASes(potentialLink.endNode)) {
			
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
			int j = rng.nextInt(manager.getPASes().size());
			shiftFlows(manager.getPASes().get(j));
		}
	}
	
	//////////////////// Methods related to creating PASes ////////////////////
	
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
					
					if (postEdge != null) // we don't shift flows in postprocessing
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
		
		for (Network.Edge incomingEdge : network.incomingOf(node)) {
			
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
		for (Network.Edge edge = minTree[start]; ;
			 edge = minTree[edge.startNode]) {
			
			minSegment[i--] = edge.index;
			if (edge.startNode == tail)
				break;
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
	
	//////////////////// Methods related to postprocessing ////////////////////
	
	@Override
	protected void postProcess() {
		if (!doPostProcess)
			return;
		
		// TODO analyze number of PASes with 1 origin, analyze number of added PASes
		
		var pair = calculateNodeFlowAndApproachProportions();
		double[][] nodeFlows = pair.first();
		double[][] approachProportions = pair.second();
		
		System.out.println(calculateEntropy(nodeFlows));
		for (int j = 0; j < 5; j++) {
			
			addPASes();
			
			for (PAS pas : manager) {
				int head = pas.head(network);
				Vector<PropData> pasData = new Vector<>();
				
				double totalMinSegmentFlow = 0;
				double totalMaxSegmentFlow = 0;
				
				// collect all origins associated with this PAS
				for (int origin = 0; origin < network.zones; origin++) {
					pas.origin = origin;
					
					double minSFB = pas.minSegmentFlowBound(bushes);
					double maxSFB = pas.maxSegmentFlowBound(bushes);
					if (minSFB + maxSFB > 1e-8) {
						
						double minSegmentFlow = nodeFlows[origin][head];
						for (int i : pas.minSegment())
							minSegmentFlow *= approachProportions[origin][i];
						
						double maxSegmentFlow = nodeFlows[origin][head];
						for (int i : pas.maxSegment())
							maxSegmentFlow *= approachProportions[origin][i];
						
						totalMinSegmentFlow += minSegmentFlow;
						totalMaxSegmentFlow += maxSegmentFlow;
						
						pasData.add(new PropData(
								origin, minSegmentFlow, maxSegmentFlow
						));
					}
				}
				
				double proportion = totalMinSegmentFlow / (totalMinSegmentFlow + totalMaxSegmentFlow);
				
				for (PropData data : pasData) {
					Bush bush = bushes[data.origin];
					
					double flowAdjustment = proportion *
							(data.minSegmentFlow + data.maxSegmentFlow) - data.minSegmentFlow;
					
					if (flowAdjustment + data.minSegmentFlow < 0 || data.maxSegmentFlow - flowAdjustment < 0) {
						System.out.println("Something's wrong " + pasData.size());
					}
					
					for (int i : pas.minSegment())
						bush.addFlow(i, flowAdjustment);
					
					for (int i : pas.maxSegment())
						bush.addFlow(i, -flowAdjustment);
					
					updateNodeFlowsAndApproachProportions(
							nodeFlows, approachProportions, pas, data.origin);
				}
			}
			
			System.out.println(calculateEntropy(nodeFlows));
		}
	}
	
	protected Pair<double[][], double[][]> calculateNodeFlowAndApproachProportions() {
		double[][] nodeFlows = new double[network.zones][];
		double[][] approachProportions = new double[network.zones][];
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			// calculate origin-based node flow for each node
			double[] nodeFlow = new double[network.nodes];
			for (Network.Edge edge : network.getEdges()) {
				nodeFlow[edge.endNode] += bush.getEdgeFlow(edge.index);
			}
			nodeFlows[origin] = nodeFlow;
			
			// calculate origin-based approach proportions for each link
			double[] approachProportion = new double[network.edges];
			for (Network.Edge edge : network.getEdges()) {
				approachProportion[edge.index] = (nodeFlow[edge.endNode] == 0)
						? 0
						: bush.getEdgeFlow(edge.index) / nodeFlow[edge.endNode];
			}
			approachProportions[origin] = approachProportion;
		}
		
		return new Pair<>(nodeFlows, approachProportions);
	}
	
	protected void addPASes() {
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			var pair1 = SSSP.minTree(network, origin, costs);
			Network.Edge[] minTree = pair1.first();
			double[] minDistance = pair1.second();
			
			// create new PAS for every edge for which:
			// 1) origin flow < epsilon
			// 2) reduced cost < omega
			for (Network.Edge edge : network.getEdges()) {
				
				double reducedCost = minDistance[edge.startNode] + costs[edge.index] - minDistance[edge.endNode];
				if (bush.getEdgeFlow(edge.index) < FLOW_EPSILON && reducedCost < 1e-14) {
					
					// the flow from origin to destination edge.endNode is zero
					if (odMatrix.get(origin, edge.endNode) == 0)
						continue;
					
					Network.Edge e = mostFlowIncomingEdge(edge.endNode, bush);
					PAS pas = MFS(e, minTree, bush, edge);
					manager.addPAS(pas);
				}
			}
		}
	}
	
	protected void updateNodeFlowsAndApproachProportions(
			double[][] nodeFlows, double[][] approachProportions, PAS pas, int origin) {
		Network.Edge[] edges = network.getEdges();
		Bush bush = bushes[origin];
		
		for (int[] segment : new int[][]{pas.minSegment(), pas.maxSegment()}) {
			
			for (int i : segment) {
				int node = edges[i].endNode;
				
				double newNodeFlow = 0;
				for (Network.Edge edge : network.incomingOf(node)) {
					newNodeFlow += bush.getEdgeFlow(edge.index);
				}
				nodeFlows[origin][node] = newNodeFlow;
				
				for (Network.Edge edge : network.incomingOf(node)) {
					approachProportions[origin][edge.index] = (newNodeFlow == 0)
							? 0
							: bush.getEdgeFlow(edge.index) / newNodeFlow;
				}
			}
			
		}
	}
	
	protected double calculateEntropy(double[][] nodeFlows) {
		double entropy = 0;
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			for (Network.Edge edge : network.getEdges()) {
				double x = bush.getEdgeFlow(edge.index);
				double n = nodeFlows[origin][edge.endNode];
				entropy += (x == 0) // 0 * ln(0) = 0
						? 0
						: x * Math.log(x / n);
				System.out.print("");
			}
		}
		
		return -entropy;
	}
	
	
	protected static class PASManager implements Iterable<PAS> {
		private final Network network;
		private final ArrayList<PAS> P;
		private final ArrayList<PAS>[] Pj;
		
		public PASManager(Network network) {
			this.network = network;
			this.P = new ArrayList<>();
			this.Pj = new ArrayList[network.nodes];
			for (int i = 0; i < Pj.length; i++)
				Pj[i] = new ArrayList<>();
		}
		
		public void addPAS(PAS newPas) {
			List<PAS> Pj = this.Pj[newPas.head(network)];
			
			P.add(newPas);
			Pj.add(newPas);
		}
		
		public List<PAS> getPASes() {
			return P;
		}
		
		public List<PAS> getPASes(int head) {
			return Pj[head];
		}
		
		@Nonnull
		public Iterator<PAS> iterator() {
			return P.iterator();
		}
		
		public void removePAS(Iterator<PAS> it, PAS pas) {
			it.remove();
			Pj[pas.head(network)].remove(pas);
		}
	}
	
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
			for (int i : maxSegment())
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
		
		/* Updates which segment is min and which max. Also updates
		 * the costs of both segments.
		 */
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
	
	protected record PropData(
			int origin,
			double minSegmentFlow,
			double maxSegmentFlow
	) {}
}
