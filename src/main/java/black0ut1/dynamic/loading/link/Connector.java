package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.Clock;

public class Connector extends Link {
	
	public Connector(int index, Clock clock) {
		super(index, clock, Double.POSITIVE_INFINITY, 10_000,
				Double.POSITIVE_INFINITY, 1, 1);
	}
	
	@Override
	public void computeReceivingAndSendingFlows() {
		this.receivingFlow = Double.POSITIVE_INFINITY;
		
		this.sendingFlow = cumulativeUpstreamCount.get(clock.getCurrentStep())
				- cumulativeDownstreamCount.get(clock.getCurrentStep());
	}
}
