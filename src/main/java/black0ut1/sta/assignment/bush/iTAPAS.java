package black0ut1.sta.assignment.bush;

import black0ut1.data.Bush;
import black0ut1.data.Network;
import black0ut1.sta.Convergence;
import black0ut1.util.SSSP;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("unchecked")
public class iTAPAS extends BushBasedAlgorithm {
	
	protected static final double FLOW_EPSILON = 1e-12;
	protected static final double COST_EFFECTIVE_FACTOR = 0.5;
	protected static final int RANDOM_SHIFTS = 400;
	
	protected final Random rng = new Random(42);
	protected final PASManager manager;
	
	/* Arrays specific for MFS method, extracted outside of the method
	 * to avoid unnecessary allocation.
	 */
	protected Network.Edge[] higherCostSegment = new Network.Edge[network.nodes];
	protected int[] scanStatus = new int[network.nodes];
	
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
				
				PAS newPas = MFS(edge, minTree, bush);
				if (newPas != null)
					manager.addPAS(newPas);
			}
			
			randomShifts();
		}
		
		eliminatePASes();
	}
	
	/* Potential link is every link in the network, which:
	 * 1) is not part of mintree from currently processed origin,
	 * 2) has nonzero (or bigger than some epsilon) origin flow and
	 * 3) sufficently large reduced cost.
	 * Conditions 2) and 3) are checked in the main loop.
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
	
	protected PAS matchPAS(Network.Edge potentialLink, double reducedCost) {
		
		for (PAS pas : manager.getPASes(potentialLink.endNode)) {
			
			pas.updateSegments(costs);
			if (pas.maxSegmentLastEdge() == potentialLink.index
					&& pas.segmentsCostDifference() > COST_EFFECTIVE_FACTOR * reducedCost) {
				
				// evaluating this condition is expensive, it is better to left it out (FLOW_EFFECTIVE_FACTOR = 0.25)
				// if (pas.maxSegmentFlowBound(bushes) > FLOW_EFFECTIVE_FACTOR * flow)
				return pas;
			}
		}
		
		return null;
	}
	
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
	 */
	protected PAS MFS(Network.Edge ij, Network.Edge[] minTree, Bush bush) {
		restart:
		while (true) {
			Arrays.fill(scanStatus, 0);
			
			// set scanStatus of nodes on minTree path from j to root to -(distance from j)
			int count = 1;
			for (Network.Edge edge = minTree[ij.endNode];
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
					
				} else if (scanStatus[node] < 0) { // encountered node from mintree, the node is PAS tail
					
					PAS newPas = createPAS(ij, node, -scanStatus[node],
							scanStatus[maxIncomingLink.endNode], minTree, higherCostSegment);
					newPas.origin = bush.root;
					
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
							Network.Edge[] minTree, Network.Edge[] higherCostSegment) {
		int head = ij.endNode;
		
		int[] minSegment = new int[minSegmentLen];
		int i = minSegmentLen - 1;
		for (Network.Edge edge = minTree[head]; ;
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
		
		return new PAS(minSegment, maxSegment);
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
	
	
	public static class PASManager implements Iterable<PAS> {
		private final Network network;
		private final List<PAS> P;
		private final List<PAS>[] Pj;
		
		public PASManager(Network network) {
			this.network = network;
			this.P = new ArrayList<>();
			this.Pj = new List[network.nodes];
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
	
	public static class PAS {
		
		public int origin;
		private int minSegmentIndex = 0;
		private final int[][] segments;
		private final double[] costs;
		
		public PAS(int[] minSegment, int[] maxSegment) {
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
}
