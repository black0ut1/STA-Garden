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
	
	/** Queue of waiting flows, should the outgoingLink be congested. */
	protected final Deque<MixtureFlow> waitingFlows;
	
	public Origin(int index, Link outgoingLink, TimeDependentODM odm) {
		super(index, null, new Link[]{outgoingLink});
		this.odm = odm;
		this.waitingFlows = new ArrayDeque<>();
	}
	
	@Override
	public void shiftOrientedMixtureFlows(int timeStep) {
		Link outgoingLink = outgoingLinks[0];
		double R = outgoingLink.getReceivingFlow();
		
		MixtureFlow mf = createMixtureFlowFromODM(timeStep);
		
		if (waitingFlows.isEmpty() && R >= mf.totalFlow()) {
			// outgoingLink is uncongested
			outgoingLink.enterFlow(mf);
			
		} else {
			// outgoingLink is congested, we must first process the
			// flows that are waiting
			waitingFlows.addLast(mf);
			
			MixtureFlow totalOutgoing = new MixtureFlow(0, new HashMap<>());
			while (!waitingFlows.isEmpty() && R > 0) {
				MixtureFlow mf2 = waitingFlows.removeFirst();
				
				if (mf2.totalFlow() > R) {
					var pair = mf2.splitFlow(R);
					MixtureFlow first = pair.first();
					MixtureFlow second = pair.second();
					
					waitingFlows.addFirst(second);
					mf2 = first;
				}
				
				totalOutgoing = totalOutgoing.plus(mf2);
				R -= mf2.totalFlow();
			}
			
			outgoingLink.enterFlow(totalOutgoing);
		}
	}
	
	@Override
	protected double[][] computeOrientedFlows(double[][] totalTurningFractions) {
		return null;
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
