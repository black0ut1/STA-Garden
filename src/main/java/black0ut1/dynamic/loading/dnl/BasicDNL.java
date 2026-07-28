package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.routing.MixtureFlow;
import black0ut1.dynamic.loading.node.Intersection;

/**
 * The basic dynamic network loading scheme. For each time in the
 * simulation, it does the following:								  <br>
 * 1. Load traffic from artificial origins onto their connectors.	  <br>
 * 2. Updates each intersection										  <br>
 * 2.1. Updates sending flow of each incoming link					  <br>
 * 2.2. Updates receiving flow of each outgoing link				  <br>
 * 2.3. Computes oriented flows using the intersection model		  <br>
 * 2.4. Removes oriented flows from incoming links					  <br>
 * 2.5. Load oriented flows onto outgoing links						  <br>
 * 3. Sinks traffic from connectors to destinations					  <br>
 * <p>
 * The step size for this scheme has upper limit imposed by the lowest
 * free flow time, i.e.: 											  <br>
 * stepSize <= min{ link.length / link.freeFlowSpeed }
 */
public class BasicDNL extends DynamicNetworkLoading {
	
	public BasicDNL(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
		super(network, odm, stepSize, steps);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		// For each intersection.
		for (Intersection node : network.intersections) {
			nodeUpdates++;
			
			// 1. Update sending flow of each incoming link.
			for (Link incomingLink : node.incomingLinks)
				incomingLink.computeSendingFlow(t);
			
			// 2. Update receiving flow of each outgoing link.
			for (Link outgoingLink : node.outgoingLinks)
				outgoingLink.computeReceivingFlow(t);
			
			// 3. Compute oriented mixture flows using routing model.
			var pair = node.computeMixtureInflowsOutflows(t);
			
			// 4. Increase outflows of incoming links.
			for (int i = 0; i < node.incomingLinks.length; i++) {
				Link incomingLink = node.incomingLinks[i];
				MixtureFlow incomingFlow = pair.first()[i];
				
				incomingLink.cumulativeOutflow[t + 1] = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
			}
			
			// 5. Increase inflows of outgoing links.
			for (int j = 0; j < node.outgoingLinks.length; j++) {
				Link outgoingLink = node.outgoingLinks[j];
				MixtureFlow outgoingFlow = pair.second()[j];
				
				outgoingLink.inflow[t] = outgoingFlow;
				outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
			}
		}
	}
}
