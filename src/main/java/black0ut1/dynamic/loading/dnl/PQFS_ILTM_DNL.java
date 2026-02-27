package black0ut1.dynamic.loading.dnl;

import black0ut1.data.PriorityQueue;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.RoutedIntersection;
import black0ut1.dynamic.loading.node.Origin;

/**
 * This is an alternative to {@code FastSweepingILTM_DNL} which does
 * not use inner iterations in the same way. It uses priority queue to
 * select the intersection with largest update potential and updates
 * it. This should be more powerful in reducing the amount of node
 * updates than the ordinary {@code FastSweepingILTM_DNL}.
 */
public class PQFS_ILTM_DNL extends ILTM_DNL {
	
	public PQFS_ILTM_DNL(DynamicNetwork network, TimeDependentODM odm,
						 double stepSize, int steps, double precision) {
		super(network, odm, stepSize, steps, precision);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		for (Link link : network.allLinks) {
			link.cumulativeInflow[t + 1] = Math.max(link.cumulativeInflow[t + 1], link.cumulativeInflow[t]);
			link.cumulativeOutflow[t + 1] = Math.max(link.cumulativeOutflow[t + 1], link.cumulativeOutflow[t]);
		}
		
		// 1. Load traffic from each origin onto the connector
		for (Origin origin : network.origins) {
			Link outgoingLink = origin.outgoingLinks[0];
			outgoingLink.computeReceivingFlow(t);
			
			var pair = origin.computeOrientedMixtureFlows(t);
			
			MixtureFlow outgoingFlow = pair.second()[0];
			outgoingLink.inflow[t] = outgoingFlow;
			outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
		}
		
		
		PriorityQueue pq = new PriorityQueue(network.routedIntersections.length, 0);
		for (int i = 0; i < network.routedIntersections.length; i++)
			pq.add(i, -Double.POSITIVE_INFINITY);
		
		// 2. Iterate until update potential of every intersection of
		// is under precision
		while (-pq.getMinPriority() > precision) {
			nodeUpdates++;
			
			int index = pq.popMin();
			RoutedIntersection node = network.routedIntersections[index];
			
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
				
				// increase update potential of the incoming link tail
				if (incomingLink instanceof LTM) {
					double Vi = incomingLink.cumulativeOutflow[t + 1];
					double potentialIncrease = ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
					
					if (potentialIncrease > 0)
						pq.decreasePriority(incomingLink.tail.index, potentialIncrease);
				}
				
				incomingLink.outflow[t] = incomingFlow;
				incomingLink.cumulativeOutflow[t + 1] = Xad;
			}
			
			// 2.1.5 Load oriented flows onto outgoing links
			for (int j = 0; j < node.outgoingLinks.length; j++) {
				Link outgoingLink = node.outgoingLinks[j];
				MixtureFlow outgoingFlow = pair.second()[j];
				
				double Xbd = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
				
				// increase update potential of the outgoing link head
				if (outgoingLink instanceof LTM) {
					double Ui = outgoingLink.cumulativeInflow[t + 1];
					double potentialIncrease = ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					
					if (potentialIncrease > 0)
						pq.decreasePriority(outgoingLink.head.index, potentialIncrease);
				}
				
				outgoingLink.inflow[t] = outgoingFlow;
				outgoingLink.cumulativeInflow[t + 1] = Xbd;
			}
			
			// 2.1.6 This intersection was just updated, thus
			// potential is 0
			pq.add(index, 0);
		}
		
		
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
