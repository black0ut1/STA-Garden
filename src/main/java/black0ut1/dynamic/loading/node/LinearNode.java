package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;

/**
 * Basic node model representing links in series:
 *   -->O-->
 */
public class LinearNode extends Node {
	
	public LinearNode(int index, Link incomingLink, Link outgoingLink) {
		super(index, new Link[] {incomingLink}, new Link[] {outgoingLink});
	}
	
	@Override
	public void shiftOrientedMixtureFlows(int time, int destinations) {
		Link incoming = incomingLinks[0];
		Link outgoing = outgoingLinks[0];
		
		double flow = Math.min(incoming.getSendingFlow(), outgoing.getReceivingFlow());
		MixtureFlow exited = incoming.exitFlow(time, flow);
		outgoing.enterFlow(time, exited);
	}
}
