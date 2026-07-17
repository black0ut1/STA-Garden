package black0ut1.dynamic.loading.dnl;

import black0ut1.data.PriorityQueue;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.routing.MixtureFlow;

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
		
		// 1. Warm/cold-start the cumulative values that will be set during this time step.
		// Warm-starting uses the value of cumulative inflow/outflow from the same time
		// of previous run. Cold-starting uses the value from the previous time step of
		// the current run.
		for (Link link : network.links) {
			link.cumulativeInflow[t + 1] = Math.max(link.cumulativeInflow[t + 1], link.cumulativeInflow[t]);
			link.cumulativeOutflow[t + 1] = Math.max(link.cumulativeOutflow[t + 1], link.cumulativeOutflow[t]);
		}
		
		// 2. Create priority queue and add every intersection into it with priority being
		// infinity (here, it's -infinity because this is max-priority queue).
		PriorityQueue pq = new PriorityQueue(network.intersections.length, 0);
		for (int i = 0; i < network.intersections.length; i++)
			pq.add(i, -Double.POSITIVE_INFINITY);
		
		// 3. Iteratively update the intersections with the highest potential until the
		// highest potential is under precision.
		while (-pq.getMinPriority() > precision) {
			nodeUpdates++;
			
			int index = pq.popMin();
			Intersection node = network.intersections[index];
			
			// 3.1 Update sending flow of each incoming link.
			for (Link incomingLink : node.incomingLinks)
				incomingLink.computeSendingFlow(t);
			
			// 2.1.2 Update receiving flow of each outgoing link.
			for (Link outgoingLink : node.outgoingLinks)
				outgoingLink.computeReceivingFlow(t);
			
			// 3.3 Compute oriented mixture flows using routing model.
			var pair = node.computeMixtureInflowsOutflows(t);
			
			// 3.4 Increase outflows of incoming links.
			for (int i = 0; i < node.incomingLinks.length; i++) {
				Link incomingLink = node.incomingLinks[i];
				MixtureFlow incomingFlow = pair.first()[i];
				
				double Xad = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
				
				// Increase update potential of the link tail.
				if (incomingLink instanceof LTM) {
					double Vi = incomingLink.cumulativeOutflow[t + 1];
					double potentialIncrease = ((LTM) incomingLink).psi * Math.abs(Xad - Vi);
					
					if (potentialIncrease > 0)
						pq.decreasePriority(incomingLink.tail.index, potentialIncrease);
				}
				
				incomingLink.outflow[t] = incomingFlow;
				incomingLink.cumulativeOutflow[t + 1] = Xad;
			}
			
			// 3.5 Increase inflows of outgoing links.
			for (int j = 0; j < node.outgoingLinks.length; j++) {
				Link outgoingLink = node.outgoingLinks[j];
				MixtureFlow outgoingFlow = pair.second()[j];
				
				double Xbd = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
				
				// Increase update potential of the link head.
				if (outgoingLink instanceof LTM) {
					double Ui = outgoingLink.cumulativeInflow[t + 1];
					double potentialIncrease = ((LTM) outgoingLink).phi * Math.abs(Xbd - Ui);
					
					if (potentialIncrease > 0)
						pq.decreasePriority(outgoingLink.head.index, potentialIncrease);
				}
				
				outgoingLink.inflow[t] = outgoingFlow;
				outgoingLink.cumulativeInflow[t + 1] = Xbd;
			}
			
			// 3.6 This intersection was just updated, thus potential is 0.
			pq.add(index, 0);
		}
	}
}
