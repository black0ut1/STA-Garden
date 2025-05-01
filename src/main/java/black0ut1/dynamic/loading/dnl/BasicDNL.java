package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.Node;
import black0ut1.dynamic.loading.node.Origin;

/**
 * The basic type of DNL which alternates between executing link
 * models and executing node models.
 */
public class BasicDNL extends DynamicNetworkLoading {
	
	public BasicDNL(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
		super(network, odm, stepSize, steps);
	}
	
	@Override
	protected void loadForTime(int t) {
		
		// execute link models
		for (Link link : network.allLinks)
			link.computeReceivingAndSendingFlows(t);
		
		// execute node models and shift flows
		for (Node node : network.allNodes) {
			var pair = node.computeOrientedMixtureFlows(t);
			
			if (!(node instanceof Origin)) { // origins have null incomingLinks
				for (int i = 0; i < node.incomingLinks.length; i++) {
					Link incomingLink = node.incomingLinks[i];
					MixtureFlow incomingFlow = pair.first()[i];
					
					incomingLink.outflow[t] = incomingFlow;
					incomingLink.cumulativeOutflow[t + 1] = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
				}
			}
			
			if (!(node instanceof Destination)) { // destinations have null outgoingLinks
				for (int j = 0; j < node.outgoingLinks.length; j++) {
					Link outgoingLink = node.outgoingLinks[j];
					MixtureFlow outgoingFlow = pair.second()[j];
					
					outgoingLink.inflow[t] = outgoingFlow;
					outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
				}
			}
		}
		
	}
}
