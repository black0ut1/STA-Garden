package black0ut1.dynamic.tdsp;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.util.Util;

/**
 * The Continuous Decreasing Order of Time (CDOT) algorithm. A version of {@link DOT}
 * algorithm which admits continuous travel times. Values between time steps are resolved
 * by linear interpolation.
 */
public class CDOT extends DOT {
	
	public CDOT(DynamicNetwork network, double stepSize, int timeSteps, boolean sssp) {
		super(network, stepSize, timeSteps, sssp);
	}
	
	@Override
	protected void forTime(int t, int d, double[][] travelTimes, MixtureOutgoingFractions.Costs costs,
						   MixtureOutgoingFractions.Indices outgoingIndices) {
		for (Link link : network.links) {
			int n = link.tail.index;
			int m = link.head.index;
			
			// Normalized travel time must be >= 1. In other words, the original travel
			// time must be >= step size.           right boundary vvv
			double travelTimeNormalized = travelTimes[link.index][t + 1] / stepSize;
			double travelTime = travelTimes[link.index][t + 1];
			
			int rounded = (int) Math.round(travelTimeNormalized);
			
			double newCost;
			if (t + rounded > timeSteps - 1) {
				// Here, we use the the assumption that conditions are stationary after
				// the modelled period.
				newCost = travelTime + costs.getCost(m, timeSteps - 1, d);
				
			} else if (Util.equals(travelTimeNormalized, rounded, 1e-10)) {
				// Travel time sufficiently close to an integer.
				newCost = travelTime + costs.getCost(m, t + rounded, d);
				
			} else {
				// Nnon-integer travel time, values must be interpolated.
				int t0 = (int) travelTimeNormalized;  // integer part
				double p = travelTimeNormalized - t0; // fractional part
				
				double interpolated = (1 - p) * costs.getCost(m, t + t0, d) + p * costs.getCost(m, t + t0 + 1, d);
				newCost = travelTime + interpolated;
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
}
