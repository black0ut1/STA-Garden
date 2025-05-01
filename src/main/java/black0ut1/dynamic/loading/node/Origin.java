package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

/**
 * Represents origin - a virtual node that loads traffic onto the
 * network - a dynamic flow source. Origin has only one outgoing link
 * and none incoming. The outgoing link must be able to accomodate for
 * all of the flow outgoing out of this origin.
 */
public class Origin extends Node {
	
	protected final TimeDependentODM odm;
	
	public Origin(int index, Link outgoingLink, TimeDependentODM odm) {
		super(index, null, new Link[]{outgoingLink});
		this.odm = odm;
	}
	
	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeOrientedMixtureFlows(int time) {
		
		MixtureFlow outgoingMixtureFlow = createMixtureFlowFromODM(time);
		
		MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[] {outgoingMixtureFlow};
		return new Pair<>(null, outgoingMixtureFlows);
	}
	
	protected MixtureFlow createMixtureFlowFromODM(int time) {
		
		double originFlow = 0;
		for (int dest = 0; dest < odm.zones; dest++)
			originFlow += odm.getFlow(this.index, dest, time);
		
		if (originFlow == 0)
			return MixtureFlow.ZERO;
		
		int len = 0;
		int[] destinations = new int[odm.zones];
		double[] portions = new double[odm.zones];
		
		for (int dest = 0; dest < odm.zones; dest++) {
			double flow = odm.getFlow(this.index, dest, time);
			
			if (flow > 0) {
				destinations[len] = dest;
				portions[len] = flow / originFlow;
				len++;
			}
		}
		
		return new MixtureFlow(originFlow, destinations, portions, len);
	}
}
