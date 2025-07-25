package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.RoutedIntersection;
import black0ut1.dynamic.loading.node.Origin;

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
public class FastSweepingILTM_DNL extends ILTM_DNL {
	
	public FastSweepingILTM_DNL(DynamicNetwork network, TimeDependentODM odm,
								double stepSize, int steps, double precision) {
		super(network, odm, stepSize, steps, precision);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		// 1. Load traffic from each origin onto the connector
		for (Origin origin : network.origins) {
			Link outgoingLink = origin.outgoingLinks[0];
			outgoingLink.computeReceivingFlow(t);
			
			var pair = origin.computeOrientedMixtureFlows(t);
			
			MixtureFlow outgoingFlow = pair.second()[0];
			outgoingLink.inflow[t] = outgoingFlow;
			outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
		}
		
		// Initialize update potential of every intersection to inf
		// and clone intersection array for sorting
		RoutedIntersection[] intersections = network.routedIntersections.clone();
		for (RoutedIntersection intersection : intersections)
			intersection.potential = Double.POSITIVE_INFINITY;
		
		// 2. Iterate until update potential of every intersection of
		// is under precision
		do {
			// 2.1 For each intersection
			for (RoutedIntersection node : intersections) {
				
				// Update potential of this node is sufficiently small
				// so we do not need to update it.
				if (node.potential < precision)
					continue;
				
				nodeUpdates++;
				
				// 2.1.1 Update sending flow of each incoming link
				for (Link incomingLink : node.incomingLinks)
					incomingLink.computeSendingFlow(t);
				
				// 2.1.2 Update receiving flow of each outgoing link
				for (Link outgoingLink : node.outgoingLinks)
					outgoingLink.computeReceivingFlow(t);
				
				// 2.1.3 Compute oriented flows using intersection model
				var pair = node.computeOrientedMixtureFlows(t);
				
				// 2.1.4 Remove oriented flows from incoming links
				for (int i = 0; i < node.incomingLinks.length; i++) {
					Link incomingLink = node.incomingLinks[i];
					MixtureFlow incomingFlow = pair.first()[i];
					
					double Xad = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
					
					// increase update potential of the link tail
					if (incomingLink instanceof LTM) {
						double Vi = incomingLink.cumulativeOutflow[t + 1];
						((RoutedIntersection) incomingLink.tail).potential += ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
					}
					
					incomingLink.outflow[t] = incomingFlow;
					incomingLink.cumulativeOutflow[t + 1] = Xad;
				}
				
				// 2.1.5 Load oriented flows onto outgoing links
				for (int j = 0; j < node.outgoingLinks.length; j++) {
					Link outgoingLink = node.outgoingLinks[j];
					MixtureFlow outgoingFlow = pair.second()[j];
					
					double Xbd = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
					
					// increase update potential of the link head
					if (outgoingLink instanceof LTM) {
						double Ui = outgoingLink.cumulativeInflow[t + 1];
						((RoutedIntersection) outgoingLink.head).potential += ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					}
					
					outgoingLink.inflow[t] = outgoingFlow;
					outgoingLink.cumulativeInflow[t + 1] = Xbd;
				}
				
				// 2.1.6 This intersection was just updated, thus
				// potential is 0
				node.potential = 0;
			}
			
			// 2.2 Sort intersection array so that the intersections
			// with most potential are updated first
			Arrays.sort(intersections, Comparator.comparingDouble(o -> -o.potential));
			
		} while (intersections[0].potential > precision);
		
		
		// 3. Sink traffic from connector to each destination
		for (Destination destination : network.destinations) {
			Link incomingLink = destination.incomingLinks[0];
			incomingLink.computeSendingFlow(t);
			
			var pair = destination.computeOrientedMixtureFlows(t);
			
			MixtureFlow incomingFlow = pair.first()[0];
			incomingLink.outflow[t] = incomingFlow;
			incomingLink.cumulativeOutflow[t + 1] = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
		}
	}
}
