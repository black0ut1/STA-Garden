package black0ut1.dynamic.loading.link;

public class Connector extends Link {
	
	public Connector(int index, int timeSteps) {
		super(index, timeSteps, Double.POSITIVE_INFINITY, 10_000, Double.POSITIVE_INFINITY, 1, 1);
	}
	
	@Override
	public void computeReceivingAndSendingFlows(int time) {
		this.receivingFlow = Double.POSITIVE_INFINITY;
		
		this.sendingFlow = cumulativeUpstreamCount[time]
				- cumulativeDownstreamCount[time];
	}
}
