package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.Clock;


/**
 * Link transmission model.
 * Bibliography:
 * - (Yperman et al., 2005) THE LINK TRANSMISSION MODEL: AN EFFICIENT
 * IMPLEMENTATION OF THE KINEMATIC WAVE THEORY IN TRAFFIC NETWORKS
 * - (Yperman, 2007) PhD thesis
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.5.2
 */
public class LTM extends Link {
	
	public LTM(int index, Clock clock, double length,
			   double capacity, double jamDensity,
			   double freeFlowSpeed, double backwardWaveSpeed) {
		super(index, clock, length, capacity, jamDensity,
				freeFlowSpeed, backwardWaveSpeed);
	}
	
	@Override
	public void computeReceivingAndSendingFlows() {
		// TODO do not floor, interpolate, also precompute
		int t1 = (int) (clock.getCurrentStep() + 1 - length / backwardWaveSpeed / clock.timeStep);
		if (t1 < 0)
			t1 = 0;
		
		this.receivingFlow = Math.min(
				capacity * clock.timeStep,
				cumulativeDownstreamCount.get(t1)
						- cumulativeUpstreamCount.get(clock.getCurrentStep())
						+ jamDensity * length
		);
		
		
		int t2 = (int) (clock.getCurrentStep() + 1 - length / freeFlowSpeed / clock.timeStep);
		if (t2 < 0)
			t2 = 0;
		
		this.sendingFlow = Math.min(
				capacity * clock.timeStep,
				cumulativeUpstreamCount.get(t2)
						- cumulativeDownstreamCount.get(clock.getCurrentStep())
		);
	}
}
