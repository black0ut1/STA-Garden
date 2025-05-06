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
import java.util.Comparator;


public class FastSweepingILTM_DNL extends ILTM_DNL {
	
	public FastSweepingILTM_DNL(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
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
		
		for (Link link : network.originConnectors)
			link.computeReceivingAndSendingFlows(t);
		
		
		Intersection[] intersections = network.intersections.clone();
		for (Intersection intersection : intersections)
			intersection.potential = Double.POSITIVE_INFINITY;
		
		do { // iterative scheme
			iterations++;
			
			// for each intersection
			for (Intersection node : intersections) {
				if (node.potential == 0)
					continue;
				nodeUpdates++;
				
				// update sending flow of incoming links
				for (Link incomingLink : node.incomingLinks) {
					if (incomingLink instanceof LTM)
						((LTM) incomingLink).computeSendingFlow(t);
				}
				
				// update receiving flow of outgoing links
				for (Link outgoingLink : node.outgoingLinks) {
					if (outgoingLink instanceof LTM)
						((LTM) outgoingLink).computeReceivingFlow(t);
				}
				
				// compute oriented flow
				var pair = node.computeOrientedMixtureFlows(t);
				
				
				for (int i = 0; i < node.incomingLinks.length; i++) {
					Link incomingLink = node.incomingLinks[i];
					MixtureFlow incomingFlow = pair.first()[i];
					
					double Xad = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
					
					if (incomingLink instanceof LTM) {
						double Vi = incomingLink.cumulativeOutflow[t + 1];
						network.intersections[incomingLink.tail.index].potential
								+= ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
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
						network.intersections[outgoingLink.head.index].potential
								+= ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					}
					
					outgoingLink.inflow[t] = outgoingFlow;
					outgoingLink.cumulativeInflow[t + 1] = Xbd;
				}
				
				node.potential = 0;
			}
			
			Arrays.sort(intersections, Comparator.comparingDouble(o -> -o.potential));
		} while (intersections[0].potential > precision);
		
		
		// Algorithm part 3: Execute destination
		for (Link link : network.destinationConnectors)
			link.computeReceivingAndSendingFlows(t);
		
		for (Destination destination : network.destinations) {
			var pair = destination.computeOrientedMixtureFlows(t);
			
			Link incoming = destination.incomingLinks[0];
			var Xad = incoming.cumulativeOutflow[t] + pair.first()[0].totalFlow;
			
			incoming.outflow[t] = pair.first()[0];
			incoming.cumulativeOutflow[t + 1] = Xad;
		}
	}
}
