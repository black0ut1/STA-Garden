package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;

/**
 * Base class for dynamic node models. Node models differ by their strategy for shifting
 * flow from incoming links into outgoing links.
 */
public abstract class Intersection {
	
	public final int index;
	public final Link[] incomingLinks;
	public final Link[] outgoingLinks;
	
	public Intersection(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		this.index = index;
		this.incomingLinks = incomingLinks;
		this.outgoingLinks = outgoingLinks;
	}
	
	/**
	 * Computes the mixture inflows from incoming links and mixture outflows to outgoing
	 * links.
	 * @param time The time step at which the shift occurs.
	 * @return Pair of arrays of mixture flows. {@code pair.first()[i]} contains the
	 * inflow from {@code this.incomingLinks[i]} (analogously for {@code pair.second()}).
	 */
	public abstract Pair<MixtureFlow[], MixtureFlow[]> computeMixtureInflowsOutflows(int time);
}
