package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;

/**
 * Base class for dynamic node models. Node models differ by their
 * strategy for shifting flow from incoming links into outgoing links.
 */
public abstract class Node {
	
	public final int index;
	public final Link[] incomingLinks;
	public final Link[] outgoingLinks;
	
	public Node(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		this.index = index;
		this.incomingLinks = incomingLinks;
		this.outgoingLinks = outgoingLinks;
	}
	
	public abstract Pair<MixtureFlow[], MixtureFlow[]> computeOrientedMixtureFlows(int time);
}
