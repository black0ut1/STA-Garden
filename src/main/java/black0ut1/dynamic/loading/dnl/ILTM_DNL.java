package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.routing.MixtureFlow;

/**
 * Iterative link transmission model dynamic network loading (basic
 * version). This type of DNL is similar to {@code BasicDNL},  but it
 * executes step 2. of {@code BasicDNL} multiple times. This allows to
 * circumvent the limit put on step size by the link with lowest free
 * flow time. The amount of these inner iterations is dependent on how
 * consistent the solution should be (which is determined by the
 * precision).
 * <p>
 * It assumes that the network consists only of connectors and LTM
 * links. It is also much faster, when executed on a network which
 * contains values from previous DNL if changes in input are small.
 * <p>
 * Bibliography:													  <br>
 * - (Himpe et al., 2016) An efficient iterative link transmission
 * model															  <br>
 */
public class ILTM_DNL extends DynamicNetworkLoading {
	
	protected final double precision;
	
	public ILTM_DNL(DynamicNetwork network, TimeDependentODM odm,
					double stepSize, int steps, double precision) {
		super(network, odm, stepSize, steps);
		this.precision = precision;
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
		
		// 2. Initialize update potential of every intersection to infinity.
		for (Intersection intersection : network.intersections)
			intersection.potential = Double.POSITIVE_INFINITY;
		
		// 3. Iterate over all intersections until update potential of every intersection
		// is under precision.
		do {
			// 3.1 For each intersection
			for (Intersection node : network.intersections) {
				
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
		} while (abovePrecision());
	}
	
	protected boolean abovePrecision() {
		for (Intersection intersection : network.intersections) {
			if (intersection.potential > precision)
				return true;
		}
		
		return false;
	}
}
