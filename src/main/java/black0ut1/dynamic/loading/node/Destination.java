package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

import java.util.Arrays;
import java.util.Vector;

/**
 * Represents destination - a virtual intersection that consumes traffic
 * from the network - a dynamic flow sink.
 * Destination has only one incoming link and none outgoing. All of
 * the flow from the incoming link is consumed during a time step.
 */
public class Destination extends Node {
	
	public final MixtureFlow[] inflow;
	
	public Destination(int index, int timeSteps, Link incomingLink) {
		super(index, new Link[]{incomingLink}, null);
		this.inflow = new MixtureFlow[timeSteps];
	}
	
	@Override
	public void shiftOrientedMixtureFlows(int time) {
		Link incomingLink = incomingLinks[0];
		double S = incomingLink.getSendingFlow();
		
		MixtureFlow exited = incomingLink.exitFlow(time, S);
		inflow[time] = exited;
	}
	
	public void reset() {
		Arrays.fill(inflow, null);
	}
}
