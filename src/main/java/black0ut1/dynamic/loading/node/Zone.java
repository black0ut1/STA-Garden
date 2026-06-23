package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.routing.MixtureFlow;

import java.util.Arrays;

public class Zone extends Intersection {
	
	protected final TimeDependentODM odm;
	public final MixtureFlow[] inflow;
	
	public Zone(int index, Link[] incomingLinks, Link[] outgoingLinks, TimeDependentODM odm, int timeSteps) {
		super(index, incomingLinks, outgoingLinks);
		this.odm = odm;
		this.inflow = new MixtureFlow[timeSteps];
	}
	
	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeMixtureInflowsOutflows(int time) {
		Link incomingLink = incomingLinks[0];
		double S = incomingLink.getSendingFlow();
		
		MixtureFlow incomingMixtureFlow = incomingLink
				.getOutgoingMixtureFlow(time)
				.copyWithFlow(S);
		
		inflow[time] = incomingMixtureFlow;
		
		
		MixtureFlow outgoingMixtureFlow = createMixtureFlowFromODM(time);
		return new Pair<>(
				new MixtureFlow[]{incomingMixtureFlow},
				new MixtureFlow[]{outgoingMixtureFlow}
		);
	}
	
	protected MixtureFlow createMixtureFlowFromODM(int time) {
		double originFlow = 0;
		for (int dest = 0; dest < odm.zones; dest++)
			originFlow += odm.getDemand(this.index, dest, time);
		
		if (originFlow == 0)
			return MixtureFlow.ZERO;
		
		int len = 0;
		int[] destinations = new int[odm.zones];
		double[] portions = new double[odm.zones];
		
		for (int dest = 0; dest < odm.zones; dest++) {
			double flow = odm.getDemand(this.index, dest, time);
			
			if (flow > 0) {
				destinations[len] = dest;
				portions[len] = flow / originFlow;
				len++;
			}
		}
		
		return new MixtureFlow(originFlow, destinations, portions, len);
	}
	
	public void reset() {
		// release objects
		Arrays.fill(inflow, null);
	}
}
