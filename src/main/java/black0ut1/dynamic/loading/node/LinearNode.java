package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

/**
 * Basic node model representing links in series:
 *   -->O-->
 */
public class LinearNode extends Node {
	
	public LinearNode(int index, Link incomingLink, Link outgoingLink) {
		super(index, new Link[] {incomingLink}, new Link[] {outgoingLink});
	}
	
	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeOrientedMixtureFlows(int time) {
		Link incoming = incomingLinks[0];
		Link outgoing = outgoingLinks[0];
		
		double flow = Math.min(incoming.getSendingFlow(), outgoing.getReceivingFlow());
		MixtureFlow mixtureFlow = incoming
				.getOutgoingMixtureFlow(time)
				.copyWithFlow(flow);
		
		MixtureFlow[] incomingMixtureFlows = new MixtureFlow[] {mixtureFlow};
		MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[] {mixtureFlow};
		return new Pair<>(incomingMixtureFlows, outgoingMixtureFlows);
	}
}
