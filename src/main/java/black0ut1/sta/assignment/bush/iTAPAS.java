package black0ut1.sta.assignment.bush;

import black0ut1.data.Bush;
import black0ut1.data.Network;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import java.util.*;

@SuppressWarnings("unchecked")
public class iTAPAS extends BushBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	protected static final double FLOW_EPSILON = 1e-12;
	protected static final double REDUCED_COST_EPSILON = 1e-16;
	
	protected static final double COST_EFFECTIVE_FACTOR = 0.5;
	protected static final double FLOW_EFFECTIVE_FACTOR = 0.25;
	
	protected static final int RANDOM_SHIFTS = 400;
	protected static final int ORIGINS_SEARCHED = 50;
	
	protected final Random rng = new Random(42);
	
	protected final List<PAS>[] Pj = new List[network.nodes];
	protected final List<PAS> P = new ArrayList<>();
	
	public iTAPAS(Parameters parameters) {
		super(parameters);
		for (int i = 0; i < Pj.length; i++)
			Pj[i] = new ArrayList<>();
	}
	
	@Override
	protected void updateFlows() {
		for (int zone = 0; zone < network.zones; zone++) {
			Bush bush = bushes[zone];
//			System.out.println("zone: " + zone + ", no. of PASes: " + P.size());
			
			var pair = SSSP.minTree(network, zone, costs);
			Network.Edge[] minTree = pair.first();
			double[] minDistance = pair.second();
			
			var Lr = getPotentialLinks(minDistance, bush);
			
			
			while (!Lr.isEmpty()) {
				int edgeIndex = Lr.pop();
				Network.Edge edge = network.getEdges()[edgeIndex];
				
				double reducedCost = costs[edgeIndex] + minDistance[edge.startNode] - minDistance[edge.endNode];
				
				boolean cont = searchPASes(edge, reducedCost, bush);
				if (cont)
					continue;
				
				PAS newPas = MFS(network.getEdges()[edgeIndex], minTree, bush);
				if (newPas == null)
					continue;
				
				addPAS(newPas);
			}
			
			randomShifts();
		}
		
		eliminatePASes();
//		System.out.println("no. of PASes: " + P.size());
	}
	
	protected Stack<Integer> getPotentialLinks(double[] minDistance, Bush bush) {
		Stack<Integer> Lr = new Stack<>();
		
		for (int i = 0; i < network.edges; i++) {
			Network.Edge edge = network.getEdges()[i];
			double reducedCost = costs[i] + minDistance[edge.startNode] - minDistance[edge.endNode];
			
			if (bush.getEdgeFlow(i) > FLOW_EPSILON && reducedCost > REDUCED_COST_EPSILON)
				Lr.push(i);
		}
		
		return Lr;
	}
	
	protected void randomShifts() {
		// shift flows of up to RANDOM_SHIFTS random PASs
		
		Collections.shuffle(P, rng);
		for (int i = 0; i < Math.min(RANDOM_SHIFTS, P.size()); i++) {
			shiftFlows(P.get(i));
		}
	}
	
	protected void eliminatePASes() {
		for (int i = 0; i < 20; i++) {
			
			for (Iterator<PAS> iterator = P.iterator(); iterator.hasNext(); ) {
				PAS pas = iterator.next();
				
				// if these conditions hold, the PAS will be eliminated, but before that we search
				// some origins and maybe shift flows on them
				if (pas.maxSegmentFlowBound(bushes) == 0 &&
						pas.minSegmentFlowBound(bushes) == 0 &&
						pas.segmentsCostDifference(costs) != 0) {
					
					for (int j = 0; j < ORIGINS_SEARCHED; j++) {
						pas.origin = (pas.origin + j) % network.zones;
						
						if (pas.maxSegmentFlowBound(bushes) > 0)
							shiftFlows(pas);
					}
					
					iterator.remove();
					Pj[pas.head(network)].remove(pas);
					
				} else {
					shiftFlows(pas);
				}
			}
		}
	}
	
	protected void eliminatePASes2() {
		for (Iterator<PAS> iterator = P.iterator(); iterator.hasNext(); ) {
			PAS pas = iterator.next();
			
			if (!shiftFlows(pas)) {
				iterator.remove();
				Pj[pas.head(network)].remove(pas);
			}
		}
	}
	
	protected boolean searchPASes(Network.Edge edge, double reducedCost, Bush bush) {
		for (PAS pas : Pj[edge.endNode]) {
			if (pas.maxSegmentLastEdge() == edge.index
					&& pas.isEffective(costs, bushes,
					COST_EFFECTIVE_FACTOR * reducedCost,
					FLOW_EFFECTIVE_FACTOR * bush.getEdgeFlow(edge.index))) {
				
				shiftFlows(pas);
				return pas.maxSegmentFlowBound(bushes) > FLOW_EPSILON;
			}
		}
		
		return false;
	}
	
	protected void addPAS(PAS newPas) {
		List<PAS> Pj = this.Pj[newPas.head(network)];
		
		boolean pasAlreadyExists = false;
		for (PAS pas : Pj) {
			if (pas.equals(newPas)) {
				pasAlreadyExists = true;
				break;
			}
		}
		
		if (!pasAlreadyExists) {
			P.add(newPas);
			Pj.add(newPas);
		}
	}
	
	
	protected PAS MFS(Network.Edge ij, Network.Edge[] minTree, Bush bush) {
		int[] scanStatus = new int[network.nodes];
		
		// set scanStatus of nodes on minpath from root to j to -1
		for (Network.Edge edge = minTree[ij.endNode];
			 edge != null;
			 edge = minTree[edge.startNode]) {
			scanStatus[edge.startNode] = -1;
		}
		scanStatus[ij.endNode] = 1;
		
		// path of max segment from tail to start of i->head, higherCostSegment[v] = edge starting in v
		Network.Edge[] higherCostSegment = new Network.Edge[network.nodes];
		
		// now we back up along incoming links with max flow until we encounter node from mintree
		int node = ij.startNode;
		Network.Edge maxIncomingLink = ij;
		while (true) {
			
			// not specified in paper, needed to avoid infinite loop
			if (bush.getEdgeFlow(maxIncomingLink.index) < FLOW_EPSILON)
				return null;
			
			// add max incoming link to the higher cost segment of PAS
			higherCostSegment[node] = maxIncomingLink;
			
			switch (scanStatus[node]) {
				case 0: // nothing happened, continuing search
					scanStatus[node] = 1;
					break;
				
				case -1: // encountered node from mintree, the node is PAS tail
					PAS newPas = createPAS(ij, node, bush.root, minTree, higherCostSegment);
					shiftFlows(newPas);
					return newPas;
				
				// exact implementation is not working, always ends up in infinite loop
//					double reducedCost = costs[ij.index] + minDistance[ij.startNode] - minDistance[ij.endNode];
//
//					// if ij satisfies conditions (10)-(11) after flow shift,
//					// return new PAS, otherwise try all over again
//					if (bush.getEdgeFlow(ij.index) * reducedCost == 0 && reducedCost >= 0)
//						return newPas;
//					else
//						return identifyPAS(ij, minTree, bush, minDistance);
				
				case 1: // encountered node which was already searched, higherCostSegment is cycle
					removeCycleFlow(higherCostSegment, bush, node); // remove cycle flow and try all over again
					return MFS(ij, minTree, bush);
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
	
	protected PAS createPAS(Network.Edge ij, int tail, int root,
							Network.Edge[] minTree, Network.Edge[] higherCostSegment) {
		int head = ij.endNode;
		
		// TODO precompute in scanStatus
		int minSegmentLength = 0;
		for (Network.Edge edge = minTree[head]; ;
			 edge = minTree[edge.startNode]) {
			
			minSegmentLength++;
			if (edge.startNode == tail)
				break;
		}
		
		int maxSegmentLength = 0;
		for (Network.Edge edge = higherCostSegment[tail]; ;
			 edge = higherCostSegment[edge.endNode]) {
			
			maxSegmentLength++;
			if (edge.endNode == head)
				break;
		}
		
		int[] minSegment = new int[minSegmentLength];
		int i = minSegmentLength - 1;
		for (Network.Edge edge = minTree[head]; ;
			 edge = minTree[edge.startNode]) {
			
			minSegment[i--] = edge.index;
			if (edge.startNode == tail)
				break;
		}
		
		int[] maxSegment = new int[maxSegmentLength];
		i = 0;
		for (Network.Edge edge = higherCostSegment[tail]; ;
			 edge = higherCostSegment[edge.endNode]) {
			
			maxSegment[i++] = edge.index;
			if (edge.endNode == head)
				break;
		}
		maxSegment[maxSegmentLength - 1] = ij.index;
		
		return new PAS(root, minSegment, maxSegment);
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
	
	
	protected boolean shiftFlows(PAS pas) {
		// TODO get maxSegmentFlowBound and segmentsCostDifference from the condition
		double flowShift = findFlowShift(pas);
		if (flowShift == 0)
			return false;
		
		Bush bush = bushes[pas.origin];
		Network.Edge[] edges = network.getEdges();
		
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
	
	
	public static class PAS {
		
		public int origin;
		public final int[] minSegment;
		public final int[] maxSegment;
		
		public PAS(int origin, int[] minSegment, int[] maxSegment) {
			this.origin = origin;
			this.minSegment = minSegment;
			this.maxSegment = maxSegment;
		}
		
		public int maxSegmentLastEdge() {
			return maxSegment[maxSegment.length - 1];
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
		
		public double minSegmentFlowBound(Bush[] bushes) {
			double flowBound = Double.POSITIVE_INFINITY;
			
			for (int edgeIndex : minSegment) {
				
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
