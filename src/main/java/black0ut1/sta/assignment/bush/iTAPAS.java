package black0ut1.sta.assignment.bush;

import black0ut1.data.Bush;
import black0ut1.data.Network;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("unchecked")
public class iTAPAS extends BushBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	protected static final double FLOW_EPSILON = 1e-12;
	
	protected static final double COST_EFFECTIVE_FACTOR = 0.5;
	protected static final double FLOW_EFFECTIVE_FACTOR = 0.25;
	
	protected final PASManager manager;
	
	public iTAPAS(Parameters parameters) {
		super(parameters);
		this.manager = new PASManager(parameters.network);
	}
	
	@Override
	protected void mainLoopIteration() {
		for (int zone = 0; zone < network.zones; zone++) {
			Bush bush = bushes[zone];
//			System.out.print("\rzone: " + zone + ", no. of PASes: "
//					+ manager.getPASes().size() + "-------");
			
			var pair = SSSP.minTree(network, zone, costs);
			Network.Edge[] minTree = pair.first();
			double[] minDistance = pair.second();
			
			for (int node = 0; node < network.nodes; node++) {
				if (minTree[node] == null)
					continue;
				
				for (Network.Edge edge : network.incomingOf(node)) {
					if (edge == minTree[node] || bush.getEdgeFlow(edge.index) <= FLOW_EPSILON)
						continue;
					
					PAS found = searchPASes(edge, minTree[node]);
					
					double cost = COST_EFFECTIVE_FACTOR *
							(minDistance[edge.startNode] + costs[edge.index] - minDistance[edge.endNode]);
					double flow = FLOW_EFFECTIVE_FACTOR * bush.getEdgeFlow(edge.index);
					if (found != null && found.isEffective(costs, bushes, cost, flow)) {
						shiftFlows(found);
					} else {
						PAS newPas = MFS(edge, minTree, bush);
						if (newPas != null)
							manager.addPAS(newPas);
					}
				}
			}
		}
		
		eliminatePASes();
//		System.out.println("no. of PASes: " + manager.getPASes().size());
	}
	
	protected void eliminatePASes() {
		for (int i = 0; i < 10; i++) {
			
			nextPAS:
			for (Iterator<PAS> iterator = manager.getPASes().iterator(); iterator.hasNext(); ) {
				PAS pas = iterator.next();
				
				if (!shiftFlows(pas)) {
					
					for (int j = 0; j < 50; j++) {
						pas.origin = (pas.origin + 1) % network.zones;
						if (shiftFlows(pas))
							continue nextPAS;
					}
					
					manager.removePAS(iterator, pas);
				}
			}
		}
	}
	
	protected PAS searchPASes(Network.Edge higherCostEdge, Network.Edge lowerCostEdge) {
		for (PAS pas : manager.getPASes(higherCostEdge.endNode)) {
			if (pas.maxSegmentLastEdge() == higherCostEdge.index &&
					pas.minSegmentLastEdge() == lowerCostEdge.index) {
				return pas;
			}
		}
		
		return null;
	}
	
	//////////////////// Methods related to creating PASes ////////////////////
	
	protected PAS MFS(Network.Edge ij, Network.Edge[] minTree, Bush bush) {
		restart:
		while (true) {
			int[] scanStatus = new int[network.nodes];
			
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
			
			// path of max segment from tail to start of i->head, higherCostSegment[v] = edge starting in v
			Network.Edge[] higherCostSegment = new Network.Edge[network.nodes];
			
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
				double maxIncomingLinkBushFlow = Double.NEGATIVE_INFINITY;
				for (Network.Edge incomingEdge : network.incomingOf(node)) {
					
					if (bush.getEdgeFlow(incomingEdge.index) > maxIncomingLinkBushFlow) {
						maxIncomingLinkBushFlow = bush.getEdgeFlow(incomingEdge.index);
						maxIncomingLink = incomingEdge;
					}
				}
				
				node = maxIncomingLink.startNode;
			}
		}
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
		double flowShift = findFlowShift(pas);
		if (flowShift == 0)
			return false;
		
		Bush bush = bushes[pas.origin];
		var edges = network.getEdges();
		
		for (int edgeIndex : pas.minSegment) {
			bush.addFlow(edgeIndex, flowShift);
			flows[edgeIndex] += flowShift;
			costs[edgeIndex] = costFunction.function(edges[edgeIndex], flows[edgeIndex]);
		}
		
		for (int edgeIndex : pas.maxSegment) {
			bush.addFlow(edgeIndex, -flowShift);
			flows[edgeIndex] -= flowShift;
			costs[edgeIndex] = costFunction.function(edges[edgeIndex], flows[edgeIndex]);
		}
		
		return true;
	}
	
	protected double findFlowShift(PAS pas) {
		double maxFlowShift = pas.maxSegmentFlowBound(bushes);
		if (maxFlowShift == 0)
			return 0;
		
		Network.Edge[] edges = network.getEdges();
		
		double flowShift = 0;
		for (int i = 0; i < NEWTON_MAX_ITERATIONS; i++) {
			
			double minSegmentCost = 0;
			double minSegmentCostDerivative = 0;
			for (int edgeIndex : pas.minSegment) {
				minSegmentCost += costFunction.function(edges[edgeIndex], flows[edgeIndex] + flowShift);
				minSegmentCostDerivative += costFunction.derivative(edges[edgeIndex], flows[edgeIndex] + flowShift);
			}
			
			double maxSegmentCost = 0;
			double maxSegmentCostDerivative = 0;
			for (int edgeIndex : pas.maxSegment) {
				maxSegmentCost += costFunction.function(edges[edgeIndex], flows[edgeIndex] - flowShift);
				maxSegmentCostDerivative += costFunction.derivative(edges[edgeIndex], flows[edgeIndex] - flowShift);
			}
			
			// Newton's method might not converge if not for this condition
			if (i == 0 && minSegmentCost > maxSegmentCost)
				return 0;
			
			double newFlowShift = flowShift + (maxSegmentCost - minSegmentCost) /
					(maxSegmentCostDerivative + minSegmentCostDerivative);
			
			if (Math.abs(flowShift - newFlowShift) < NEWTON_EPSILON) {
				flowShift = newFlowShift;
				break;
			} else {
				flowShift = newFlowShift;
			}
		}
		
		return Util.projectToInterval(flowShift, 0, maxFlowShift);
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
		public final int[] minSegment;
		public final int[] maxSegment;
		
		public PAS(int[] minSegment, int[] maxSegment) {
			this.minSegment = minSegment;
			this.maxSegment = maxSegment;
		}
		
		public int maxSegmentLastEdge() {
			return maxSegment[maxSegment.length - 1];
		}
		
		public int minSegmentLastEdge() {
			return minSegment[minSegment.length - 1];
		}
		
		public boolean isEffective(double[] costs, Bush[] bushes, double cost, double flow) {
			boolean isCostEffective = segmentsCostDifference(costs) > cost;
			boolean isFlowEffective = maxSegmentFlowBound(bushes) > flow;
			return isCostEffective && isFlowEffective;
		}
		
		public double segmentsCostDifference(double[] costs) {
			return PAS.segmentCost(maxSegment, costs) - PAS.segmentCost(minSegment, costs);
		}
		
		public double maxSegmentFlowBound(Bush[] bushes) {
			double flowBound = Double.POSITIVE_INFINITY;
			
			for (int edgeIndex : maxSegment) {
				
				double bushFlow = bushes[origin].getEdgeFlow(edgeIndex);
				if (bushFlow < flowBound)
					flowBound = bushFlow;
			}
			
			return flowBound;
		}
		
		private static double segmentCost(int[] segment, double[] costs) {
			double cost = 0;
			
			for (int edgeIndex : segment)
				cost += costs[edgeIndex];
			
			return cost;
		}
		
		public int head(Network network) {
			return network.getEdges()[minSegment[minSegment.length - 1]].endNode;
		}
	}
}
