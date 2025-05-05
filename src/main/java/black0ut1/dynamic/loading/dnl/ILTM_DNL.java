package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.node.Origin;

import java.util.Arrays;

public class ILTM_DNL extends DynamicNetworkLoading {
	
	protected final double precision = 1e-8;
	
	public ILTM_DNL(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
		super(network, odm, stepSize, steps);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		// Algorithm part 1: Execute origins
		for (Origin origin : network.origins) {
			var pair = origin.computeOrientedMixtureFlows(t);
			
			Link outgoing = origin.outgoingLinks[0];
			var Xad = outgoing.cumulativeInflow[t] + pair.second()[0].totalFlow;
			
			outgoing.inflow[t] = pair.second()[0];
			outgoing.cumulativeInflow[t + 1] = Xad;
		}
		
		
		double[] deltas = new double[network.intersections.length];
		Arrays.fill(deltas, Double.POSITIVE_INFINITY);
		
		int it;
		for (it = 0; abovePrecision(deltas); it++) {
			
			for (Link link : network.allLinks)
				link.computeReceivingAndSendingFlows(t);
			
			for (Intersection node : network.intersections) {
				var pair = node.computeOrientedMixtureFlows(t);
				
				for (int i = 0; i < node.incomingLinks.length; i++) {
					Link incomingLink = node.incomingLinks[i];
					MixtureFlow incomingFlow = pair.first()[i];
					
					double Xad = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
					
					if (incomingLink instanceof LTM) {
						double Vi = incomingLink.cumulativeOutflow[t + 1];
						deltas[incomingLink.tail.index] += ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
					}
					
					incomingLink.outflow[t] = incomingFlow;
					incomingLink.cumulativeOutflow[t + 1] = Xad;
				}
				
				for (int j = 0; j < node.outgoingLinks.length; j++) {
					Link outgoingLink = node.outgoingLinks[j];
					MixtureFlow outgoingFlow = pair.second()[j];
					
					double Xbd = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
					
					if (outgoingLink instanceof LTM) {
						double Ui = outgoingLink.cumulativeInflow[t + 1];
						deltas[outgoingLink.head.index] += ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					}
					
					outgoingLink.inflow[t] = outgoingFlow;
					outgoingLink.cumulativeInflow[t + 1] = Xbd;
				}
				
				deltas[node.index] = 0;
			}
		}
		
		
		// Algorithm part 3: Execute destination
		for (Destination destination : network.destinations) {
			var pair = destination.computeOrientedMixtureFlows(t);
			
			Link incoming = destination.incomingLinks[0];
			var Xad = incoming.cumulativeOutflow[t] + pair.first()[0].totalFlow;
			
			incoming.outflow[t] = pair.first()[0];
			incoming.cumulativeOutflow[t + 1] = Xad;
		}
		
		
		System.out.println("ILTM iterations: " + it);
	}
	
	protected boolean abovePrecision(double[] deltas) {
		for (double delta : deltas)
			if (delta > precision)
				return true;
		
		return false;
	}
}
