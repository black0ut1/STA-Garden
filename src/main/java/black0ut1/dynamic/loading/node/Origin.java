package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

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
	public void shiftOrientedMixtureFlows(int time) {
		Link outgoingLink = outgoingLinks[0];
		
		MixtureFlow mf = createMixtureFlowFromODM(time);
		outgoingLink.enterFlow(time, mf);
	}
	
	protected MixtureFlow createMixtureFlowFromODM(int time) {
		double originFlow = 0;
		HashMap<Integer, Double> portions = new HashMap<>();
		
		for (int dest = 0; dest < odm.zones; dest++)
			originFlow += odm.getFlow(this.index, dest, time);
		
		for (int dest = 0; dest < odm.zones; dest++) {
			double flow = odm.getFlow(this.index, dest, time);
			
			if (flow > 0)
				portions.put(dest, flow / originFlow);
		}
		
		return new MixtureFlow(originFlow, portions);
	}
}
