package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

/**
 * Represents origin - a virtual node that loads traffic onto the
 * network - a dynamic flow source. Origin has only one outgoing link
 * and none incoming.
 */
public class Origin extends Node {
	
	protected final TimeDependentODM odm;
	
	public Origin(int index, Link outgoingLink, TimeDependentODM odm) {
		super(index, null, new Link[]{outgoingLink});
		this.odm = odm;
	}
	
	@Override
	public void shiftOrientedMixtureFlows(int time, int destinationsNum) {
		Link outgoingLink = outgoingLinks[0];
		
		MixtureFlow mf = createMixtureFlowFromODM(time, destinationsNum);
		outgoingLink.enterFlow(time, mf);
	}
	
	protected MixtureFlow createMixtureFlowFromODM(int time, int destinationsNum) {
		
		double originFlow = 0;
		for (int dest = 0; dest < odm.zones; dest++)
			originFlow += odm.getFlow(this.index, dest, time);
		
		int len = 0;
		int[] destinations = new int[destinationsNum];
		double[] portions = new double[destinationsNum];
		
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
