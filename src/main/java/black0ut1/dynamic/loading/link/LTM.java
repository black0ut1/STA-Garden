package black0ut1.dynamic.loading.link;


/**
 * Link transmission model.
 * Bibliography:
 * - (Yperman et al., 2005) THE LINK TRANSMISSION MODEL: AN EFFICIENT
 * IMPLEMENTATION OF THE KINEMATIC WAVE THEORY IN TRAFFIC NETWORKS
 * - (Yperman, 2007) PhD thesis
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.5.2
 */
public class LTM extends Link {
	
	protected final double timeStep;
	
	public LTM(int index, int timeSteps, double length, double capacity,
			   double jamDensity, double freeFlowSpeed, double backwardWaveSpeed,
			   double timeStep) {
		super(index, timeSteps, length, capacity, jamDensity, freeFlowSpeed, backwardWaveSpeed);
		this.timeStep = timeStep;
	}
	
	@Override
	public void computeReceivingAndSendingFlows(int time) {
		// TODO do not floor, interpolate, also precompute
		int t1 = (int) (time + 1 - length / backwardWaveSpeed / timeStep);
		if (t1 < 0)
			t1 = 0;
		
		this.receivingFlow = Math.min(
				capacity * timeStep,
				cumulativeDownstreamCount[t1]
						- cumulativeUpstreamCount[time]
						+ jamDensity * length
		);
		
		
		int t2 = (int) (time + 1 - length / freeFlowSpeed / timeStep);
		if (t2 < 0)
			t2 = 0;
		
		this.sendingFlow = Math.min(
				capacity * timeStep,
				cumulativeUpstreamCount[t2]
						- cumulativeDownstreamCount[time]
		);
	}
}
