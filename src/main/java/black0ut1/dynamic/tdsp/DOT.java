package black0ut1.dynamic.tdsp;

import black0ut1.data.PriorityQueue;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.util.DynamicUtils;
import black0ut1.util.Util;

import java.util.Arrays;

/**
 * The Decreasing Order of Time (DOT) algorithm. It is an all-to-one time-dependent
 * shortest paths algorithm for all departure times.
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
	
	public Pair<double[][][], MixtureOutgoingFractions[][]> shortestPaths() {
		// TODO optimize memory by returning only link indices (whether to array of all
		//  links or arrays of outgoing links for each node)
		// TODO precompute travel times for integer time steps
		
		// costs are defined at boundaries between time steps while mixture fractions are
		// defined during time steps, the resolution here is to use the left boundary
		// values
		
		// costs[t][n][d] is the shortest time from n to d, if departing from n at time t
		double[][][] costs = new double[timeSteps + 1][network.routedIntersections.length][network.zones.length];
		MixtureOutgoingFractions[][] mfs = new MixtureOutgoingFractions[network.routedIntersections.length][timeSteps];
		
		// 1. Initialize all costs to infinity, initialize mixture fractions
		for (double[][] a : costs)
			for (double[] b : a)
				Arrays.fill(b, Double.POSITIVE_INFINITY);
		
		for (int n = 0; n < network.routedIntersections.length; n++) {
			Intersection intersection = network.routedIntersections[n];
			
			for (int t = 0; t < timeSteps; t++) {
				double[][] tfs = new double[network.zones.length][intersection.outgoingLinks.length];
				mfs[n][t] = new MixtureOutgoingFractions(tfs);
			}
		}
		
		// 2. Initialize costs from destinations to themselves to 0, initialize turning
		// fractions of nodes adjacent to destinations
		for (int t = 0; t <= timeSteps; t++)
			for (int d = 0; d < network.zones.length; d++) {
				costs[t][d][d] = 0;
				
				if (t == timeSteps)
					continue;
				
				mfs[d][t].getDestinationFractions(d)[0] = 1;
			}
		
		// 3. (Optional) Initialize costs at th last time step
		if (sssp) {
			for (int d = 0; d < network.zones.length; d++)
				sssp(costs, mfs, d);
		}
		
		// 4. Set the rest of the values in decreasing order of time
		for (int d = 0; d < network.zones.length; d++) {
			for (int t = timeSteps; t >= 0; t--) {
				for (Link link : network.links) {
					int n = link.tail.index;
					int m = link.head.index;
					
					// this must not be lower than step size, i.e. travelTime / stepSize >= 1
					double travelTime = DynamicUtils.computeTravelTime(t, link, stepSize);
					double travelTimeNormalized = travelTime / stepSize;
					
					int rounded = (int) Math.round(travelTimeNormalized);
					
					double newCost;
					if (t + travelTimeNormalized >= timeSteps) {
						// the arrival time is larger than the modelled period
						newCost = travelTime + costs[timeSteps][m][d];
					} else if (Util.equals(travelTimeNormalized, rounded, 1e-10)) {
						// travel time sufficiently close to integer
						newCost = travelTime + costs[t + rounded][m][d];
					} else {
						// non-integer travel time, values must be interpolated
						int t0 = (int) travelTimeNormalized;  // integer part
						double p = travelTimeNormalized - t0; // fractional part
						
						if (t0 == timeSteps) {
							newCost = travelTime + costs[timeSteps][m][d];
						} else {
							double interpolated = (1 - p) * costs[t + t0][m][d] + p * costs[t + t0 + 1][m][d];
							newCost = travelTime + interpolated;
						}
					}
					
					if (newCost < costs[t][n][d]) {
						costs[t][n][d] = newCost;
						
						if (t == timeSteps)
							continue;
						
						// find the index of link in link.tail.outgoingLinks
						int J = -1;
						for (int j = 0; j < link.tail.outgoingLinks.length; j++)
							if (link.tail.outgoingLinks[j] == link) {
								J = j;
								break;
							}
						
						double[] turningFractions = mfs[n][t].getDestinationFractions(d);
						Arrays.fill(turningFractions, 0);
						turningFractions[J] = 1;
					}
				}
			}
		}
		
		return new Pair<>(costs, mfs);
	}
	
	protected void sssp(double[][][] costs, MixtureOutgoingFractions[][] mfs, int destination) {
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
				
				double newCost = DynamicUtils.computeTravelTime(timeSteps, incomingLink, stepSize);
				if (mark[tailNode] == 0) {
					mark[tailNode] = 1;
					costs[timeSteps][tailNode][destination] = newCost;
					
					double[] turningFractions = mfs[tailNode][timeSteps - 1].getDestinationFractions(destination);
					turningFractions[incomingLinkIndex] = 1;
					
					pq.add(tailNode, newCost);
				} else if (newCost < costs[0][tailNode][headNode]) {
					costs[timeSteps][tailNode][destination] = newCost;
					
					double[] turningFractions = mfs[tailNode][timeSteps - 1].getDestinationFractions(destination);
					Arrays.fill(turningFractions, 0);
					turningFractions[incomingLinkIndex] = 1;
					
					pq.setLowerPriority(tailNode, newCost);
				}
			}
		}
	}
}
