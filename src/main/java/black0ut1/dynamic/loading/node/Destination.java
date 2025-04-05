package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.link.Link;

import java.util.Vector;

/**
 * Represents destination - a virtual intersection that consumes traffic
 * from the network - a dynamic flow sink.
 * Destination has only one incoming link and none outgoing. All of
 * the flow from the incoming link is consumed during a time step.
 */
public class Destination extends Node {
	
	public final Vector<MixtureFlow> inflow = new Vector<>();
	
	public Destination(int index, Link incomingLink) {
		super(index, new Link[]{incomingLink}, null);
	}
	
	@Override
	public void shiftOrientedMixtureFlows(int time) {
		Link incomingLink = incomingLinks[0];
		double S = incomingLink.getSendingFlow();
		
		MixtureFlow exited = incomingLink.exitFlow(S);
		inflow.add(exited);
	}
	
	public void reset() {
		inflow.clear();
	}
}
