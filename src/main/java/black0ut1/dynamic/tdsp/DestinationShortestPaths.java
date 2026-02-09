package black0ut1.dynamic.tdsp;

import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Connector;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.node.RoutedIntersection;
import black0ut1.util.DynamicUtils;

import java.util.Arrays;

public class DestinationShortestPaths {
	
	protected final DynamicNetwork network;
	protected final double stepSize;
	protected final int timeSteps;
	
	public DestinationShortestPaths(DynamicNetwork network, double stepSize, int timeSteps) {
		this.network = network;
		this.stepSize = stepSize;
		this.timeSteps = timeSteps;
	}
	
	public double[][][] shortestPathCosts() {
		// costs[t][n][d] is the shortest time from n to d, if starting at time t
		double[][][] costs = new double[timeSteps + 1][network.routedIntersections.length][network.destinations.length];
		
		for (double[][] a : costs)
			for (double[] b : a)
				Arrays.fill(b, Double.POSITIVE_INFINITY);
		
		for (int t = timeSteps; t >= 0; t--) {
			for (int n = 0; n < network.routedIntersections.length; n++) {
				for (int d = 0; d < network.destinations.length; d++) {
					
					if (n == d) {
						costs[t][n][d] = 0;
						continue;
					}
					
					for (Link outgoingLink : network.routedIntersections[n].outgoingLinks) {
						if (outgoingLink instanceof Connector)
							continue;
						
						int m = outgoingLink.head.index;
						double newValue;
						
						// TODO computeTravelTime for integer t, precompute travelTime (same for all n and d)
						double travelTime = DynamicUtils.computeTravelTime(t, outgoingLink, stepSize) / stepSize;
						if (t + travelTime > timeSteps)
							continue;
						
						int rounded = (int) Math.round(travelTime);
						
						// travel time sufficiently close to integer
						if (Math.abs(travelTime - rounded) < 1e-8) {
							newValue = travelTime + costs[t + rounded][m][d];
						} else {
							// non-integer travel time, values must be interpolated
							int t0 = (int) travelTime; // integer part of travelTime
							double p = travelTime - t0; // fractional part of travelTime
							
							double interpolated = (1 - p) * costs[t + t0][m][d] + p * costs[t + t0 + 1][m][d];
							newValue = travelTime + interpolated;
						}
						
						costs[t][n][d] = Math.min(costs[t][n][d], newValue);
					}
				}
			}
		}
		
		return costs;
	}
	
	public MixtureFractions[][] shortestPathMixtureFractions(double[][][] costs) {
		// costs are defined at boundaries between time steps while mixture fractions are
		// defined during time steps, the resolution here is to use the left boundary
		// values
		MixtureFractions[][] mfs = new MixtureFractions[network.routedIntersections.length][timeSteps];
		for (int t = 0; t < timeSteps; t++) {
			for (int n = 0; n < network.routedIntersections.length; n++) {
				RoutedIntersection intersection = network.routedIntersections[n];
				
				DoubleMatrix[] tfs = new DoubleMatrix[network.destinations.length];
				
				for (int d = 0; d < network.destinations.length; d++) {
					DoubleMatrix turningFractions = new DoubleMatrix(
							intersection.incomingLinks.length,
							intersection.outgoingLinks.length);
					
					if (n == d) {
						for (int i = 0; i < intersection.incomingLinks.length; i++)
							turningFractions.set(i, 0, 1);

						tfs[d] = turningFractions;
						continue;
					}
					
					// the lowest cost of outgoing links
					double lowestCost = Double.POSITIVE_INFINITY;
					for (int j = 0; j < intersection.outgoingLinks.length; j++) {
						if (intersection.outgoingLinks[j] instanceof Connector)
							continue;
						
						int m = intersection.outgoingLinks[j].head.index;
						double cost = costs[t][m][d];
						
						lowestCost = Math.min(lowestCost, cost);
					}
					
					// find all the outgoing links with lowest cost (there may be more than 1 of them)
					boolean[] lowestCostLinks = new boolean[intersection.outgoingLinks.length];
					int lowestCostLinksNum = 0;
					for (int j = 0; j < intersection.outgoingLinks.length; j++) {
						if (intersection.outgoingLinks[j] instanceof Connector)
							continue;
						
						int m = intersection.outgoingLinks[j].head.index;
						double cost = costs[t][m][d];
						
						if (lowestCost == cost) {
							lowestCostLinks[j] = true;
							lowestCostLinksNum++;
						}
					}
					
					// sets the turning fraction to 1 for the lowest cost outgoing link
					// or uniformly distributes if there are more lowest cost links
					for (int i = 0; i < intersection.incomingLinks.length; i++)
						for (int j = 0; j < intersection.outgoingLinks.length; j++)
							if (lowestCostLinks[j])
								turningFractions.set(i, j, 1.0 / lowestCostLinksNum);
					
					tfs[d] = turningFractions;
				}
				
				MixtureFractions mf = new MixtureFractions(tfs);
				mfs[n][t] = mf;
			}
		}
		
		return mfs;
	}
}
