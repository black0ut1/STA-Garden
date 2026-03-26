package black0ut1.dynamic.loading.link;

/**
 * A link model for virtual links - used mainly for connecting origins
 * to zone intersections and zone intersections to destinations.
 * <p>
 * The receiving flow of this link model is infinite.				  <br>
 * The sending flow is equal to all traffic on the link.			  <br>
 * If the origin load the traffic onto a connector in time t, then the
 * traffic can leave the connector in t+1.							  <br>
 */
public class Connector extends Link {
	
	public Connector(int index, int timeSteps) {
		super(index, 0, timeSteps, Double.POSITIVE_INFINITY,
				10_000, Double.POSITIVE_INFINITY, 1, 1);
	}
	
	@Override
	public void computeReceivingFlow(int time) {
		this.receivingFlow = Double.POSITIVE_INFINITY;
	}
	
	@Override
	public void computeSendingFlow(int time) {
		this.sendingFlow = cumulativeInflow[time] - cumulativeOutflow[time];
	}
}
