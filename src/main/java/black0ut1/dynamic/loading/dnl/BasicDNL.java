package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.node.Origin;

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
		
		// 1. Load traffic from each origin onto the connector
		for (Origin origin : network.origins) {
			Link outgoingLink = origin.outgoingLinks[0];
			outgoingLink.computeReceivingFlow(t);
			
			var pair = origin.computeOrientedMixtureFlows(t);
			
			MixtureFlow outgoingFlow = pair.second()[0];
			outgoingLink.inflow[t] = outgoingFlow;
			outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
		}
		
		// 2. For each intersection
		for (Intersection node : network.routedIntersections) {
			
			// 2.1 Update sending flow of each incoming link
			for (Link incomingLink : node.incomingLinks)
				incomingLink.computeSendingFlow(t);
			
			// 2.2 Update receiving flow of each outgoing link
			for (Link outgoingLink : node.outgoingLinks)
				outgoingLink.computeReceivingFlow(t);
			
			// 2.3 Compute oriented flows using intersection model
			var pair = node.computeOrientedMixtureFlows(t);
			
			// 2.4 Remove oriented flows from incoming links
			for (int i = 0; i < node.incomingLinks.length; i++) {
				Link incomingLink = node.incomingLinks[i];
				MixtureFlow incomingFlow = pair.first()[i];
				
				incomingLink.outflow[t] = incomingFlow;
				incomingLink.cumulativeOutflow[t + 1] = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
			}
			
			// 2.5 Load oriented flows onto outgoing links
			for (int j = 0; j < node.outgoingLinks.length; j++) {
				Link outgoingLink = node.outgoingLinks[j];
				MixtureFlow outgoingFlow = pair.second()[j];
				
				outgoingLink.inflow[t] = outgoingFlow;
				outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
			}
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
