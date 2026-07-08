package black0ut1.dynamic.tdsp;

import black0ut1.data.PriorityQueue;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.util.DynamicUtils;

/**
 * The Decreasing Order of Time (DOT) algorithm. It is an all-to-one time-dependent
 * shortest paths algorithm for all departure times. It works with discrete model time and
 * discrete travel time. That is, travel times must be multiples of step size.
 * <p>
 * Bibliography:																		  <br>
 * - (Chabini, 1997) A New Algorithm for Shortest Paths in Discrete Dynamic Networks	  <br>
 * - (Chabini, 1998) Discrete Dynamic Shortest Path Problems in Transportation
 * Applications: Complexity and Algorithms with Optimal Run Time
 */
public class DOT {
	
	protected final DynamicNetwork network;
	protected final double stepSize;
	protected final int timeSteps;
	protected final boolean sssp;
	
	public DOT(DynamicNetwork network, double stepSize, int timeSteps, boolean sssp) {
		this.network = network;
		this.stepSize = stepSize;
		this.timeSteps = timeSteps;
		this.sssp = sssp;
	}
	
	public Pair<MixtureOutgoingFractions.Costs, MixtureOutgoingFractions.Indices> shortestPaths(
			MixtureOutgoingFractions c, double[][] travelTimes) {
		// Costs are defined at boundaries between time steps while mixture fractions and
		// shortest path indices are defined during time steps. The resolution here is to
		// use the right boundary values when determining the shortest path. The cost at
		// t=0 is the right boundary after first time step (i.e. the second instant).
		// 									|-|-|-|
		//									 ^^
		//    first time step (indices at t=0)^
		//                                    right boundary (costs at t=0)
		
		// 1. Initialize all costs to infinity, initialize shortest path indices
		MixtureOutgoingFractions.Costs costs = c.new Costs(Double.POSITIVE_INFINITY);
		MixtureOutgoingFractions.Indices outgoingIndices = c.new Indices();
		
		// 2. Initialize costs from destinations to themselves to 0, initialize turning
		// fractions of nodes adjacent to destinations
		for (int t = 0; t < timeSteps; t++)
			for (int d = 0; d < network.zones.length; d++) {
				costs.setCost(d, t, d, 0);
				outgoingIndices.setIndex(d, t, d, (byte) 0);
			}
		
		// 3. (Optional) Initialize costs at th last time step
		if (sssp) {
			for (int d = 0; d < network.zones.length; d++)
				sssp(d, travelTimes, costs, outgoingIndices);
		}
		
		// 4. Set the rest of the values in decreasing order of time
		for (int d = 0; d < network.zones.length; d++)
			for (int t = timeSteps - 2; t >= 0; t--)
				forTime(t, d, travelTimes, costs, outgoingIndices);
		
		return new Pair<>(costs, outgoingIndices);
	}
	
	protected void forTime(int t, int d, double[][] travelTimes, MixtureOutgoingFractions.Costs costs,
						   MixtureOutgoingFractions.Indices outgoingIndices) {
		for (Link link : network.links) {
			int n = link.tail.index;
			int m = link.head.index;
			
			// Normalized travel time must be >= 1. In other words, the original travel
			// time must be >= step size.                         right boundary vvv
			int travelTimeNormalized = (int) Math.round(travelTimes[link.index][t + 1] / stepSize);
			double travelTime = stepSize * travelTimeNormalized;
			
			double newCost;
			if (t + travelTimeNormalized > timeSteps - 1) {
				// Here, we use the the assumption that conditions are stationary after
				// the modelled period.
				newCost = travelTime + costs.getCost(m, timeSteps - 1, d);
			} else {
				newCost = travelTime + costs.getCost(m, t + travelTimeNormalized, d);
			}
			
			if (newCost < costs.getCost(n, t, d)) {
				costs.setCost(n, t, d, newCost);
				
				// find the index of link in link.tail.outgoingLinks
				int J = -1;
				for (int j = 0; j < link.tail.outgoingLinks.length; j++)
					if (link.tail.outgoingLinks[j] == link) {
						J = j;
						break;
					}
				
				outgoingIndices.setIndex(n, t, d, (byte) J);
			}
		}
	}
	
	protected void sssp(int destination, double[][] travelTimes, MixtureOutgoingFractions.Costs costs,
						MixtureOutgoingFractions.Indices outgoingIndices) {
		// This method operates on the very last time instant.
		
		PriorityQueue pq = new PriorityQueue(network.intersections.length, 0);
		byte[] mark = new byte[network.intersections.length];
		
		Intersection first = network.zones[destination].incomingLinks[0].tail;
		pq.add(first.index, 0);
		while (!pq.isEmpty()) {
			int headNode = pq.popMin();
			mark[headNode] = 2;
			
			for (Link incomingLink : network.intersections[headNode].incomingLinks) {
				int tailNode = incomingLink.tail.index;
				if (mark[tailNode] == 2)
					continue;
				
				Intersection tail = incomingLink.tail;
				int incomingLinkIndex = -1; // the index of incomingLink in tail.outgoingLinks
				for (int j = 0; j < tail.outgoingLinks.length; j++)
					if (incomingLink == tail.outgoingLinks[j]) {
						incomingLinkIndex = j;
						break;
					}
				
				// round the travel time to nearest multiple of step size
				double newCost = stepSize * Math.round(travelTimes[incomingLinkIndex][timeSteps] / stepSize);
				if (mark[tailNode] == 0) {
					mark[tailNode] = 1;
					costs.setCost(tailNode, timeSteps - 1, destination, newCost);
					outgoingIndices.setIndex(tailNode, timeSteps - 1, destination, (byte) incomingLinkIndex);
					pq.add(tailNode, newCost);
				} else if (newCost < costs.getCost(tailNode, timeSteps - 1, destination)) {
					costs.setCost(tailNode, timeSteps - 1, destination, newCost);
					outgoingIndices.setIndex(tailNode, timeSteps - 1, destination, (byte) incomingLinkIndex);
					pq.setLowerPriority(tailNode, newCost);
				}
			}
		}
	}
}
