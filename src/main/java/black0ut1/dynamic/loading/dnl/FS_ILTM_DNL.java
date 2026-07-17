package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.routing.MixtureFlow;

import java.util.Arrays;
import java.util.Comparator;

/**
 * This version of {@code ILTM_DNL} (see documentation) uses fast
 * sweeping, meaning before each iteration, it sorts intersections by
 * their update potential in descending order. This reduces the amount
 * of node updates.
 * <p>
 * Tested on ChicagoSketch - only brings significant speedup for very
 * large step sizes (5 and more). Brings no speedup for step sizes up
 * to 2.
 * <p>
 * Bibliography:													  <br>
 * - (Himpe et al., 2016) An efficient iterative link transmission
 * model															  <br>
 * - (Zhao, Hongkai, 2005) A fast sweeping method for eikonal equations
 */
public class FS_ILTM_DNL extends ILTM_DNL {
	
	public FS_ILTM_DNL(DynamicNetwork network, TimeDependentODM odm,
					   double stepSize, int steps, double precision) {
		super(network, odm, stepSize, steps, precision);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		// 1. Warm/cold-start the cumulative values that will be set during this time step.
		// Warm-starting uses the value of cumulative inflow/outflow from the same time
		// of previous run. Cold-starting uses the value from the previous time step of
		// the current run.
		for (Link link : network.links) {
			link.cumulativeInflow[t + 1] = Math.max(link.cumulativeInflow[t + 1], link.cumulativeInflow[t]);
			link.cumulativeOutflow[t + 1] = Math.max(link.cumulativeOutflow[t + 1], link.cumulativeOutflow[t]);
		}
		
		// 2. Initialize update potential of every intersection to infinity and clone
		// intersection array for sorting.
		Intersection[] intersections = network.intersections.clone();
		for (Intersection intersection : intersections)
			intersection.potential = Double.POSITIVE_INFINITY;
		
		// 3. Iterate until update potential of every intersection of is under precision.
		do {
			// 3.1 For each intersection
			for (Intersection node : intersections) {
				
				// Update potential of this node is sufficiently small so we do not need
				// to update it.
				if (node.potential < precision)
					continue;
				
				nodeUpdates++;
				
				// 3.1.1 Update sending flow of each incoming link.
				for (Link incomingLink : node.incomingLinks)
					incomingLink.computeSendingFlow(t);
				
				// 3.1.2 Update receiving flow of each outgoing link.
				for (Link outgoingLink : node.outgoingLinks)
					outgoingLink.computeReceivingFlow(t);
				
				// 3.1.3 Compute oriented mixture flows using routing model.
				var pair = node.computeMixtureInflowsOutflows(t);
				
				// 3.1.4 Increase outflows of incoming links.
				for (int i = 0; i < node.incomingLinks.length; i++) {
					Link incomingLink = node.incomingLinks[i];
					MixtureFlow incomingFlow = pair.first()[i];
					
					double Xad = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
					
					// Increase update potential of the link tail.
					if (incomingLink instanceof LTM) {
						double Vi = incomingLink.cumulativeOutflow[t + 1];
						incomingLink.tail.potential += ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
					}
					
					incomingLink.outflow[t] = incomingFlow;
					incomingLink.cumulativeOutflow[t + 1] = Xad;
				}
				
				// 3.1.5 Increase inflows of outgoing links.
				for (int j = 0; j < node.outgoingLinks.length; j++) {
					Link outgoingLink = node.outgoingLinks[j];
					MixtureFlow outgoingFlow = pair.second()[j];
					
					double Xbd = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
					
					// Increase update potential of the link head.
					if (outgoingLink instanceof LTM) {
						double Ui = outgoingLink.cumulativeInflow[t + 1];
						outgoingLink.head.potential += ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					}
					
					outgoingLink.inflow[t] = outgoingFlow;
					outgoingLink.cumulativeInflow[t + 1] = Xbd;
				}
				
				// 3.1.6 This intersection was just updated, thus potential is 0.
				node.potential = 0;
			}
			
			// 2.2 Sort intersection array so that the intersections
			// with most potential are updated first
			Arrays.sort(intersections, Comparator.comparingDouble(o -> -o.potential));
			
		} while (intersections[0].potential > precision);
	}
}
