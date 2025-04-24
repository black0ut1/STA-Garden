package black0ut1.dynamic.loading.link;

/**
 * Spatial queue model.
 * Bibliography:
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.1.3
 */
public class SpatialQueue extends Link {
	
	public SpatialQueue(int index, double stepSize, int timeSteps,
						double length, double capacity, double jamDensity,
						double freeFlowSpeed, double backwardWaveSpeed) {
		super(index, stepSize, timeSteps, length, capacity,
				jamDensity, freeFlowSpeed, backwardWaveSpeed);
	}
	
	@Override
	public void computeReceivingAndSendingFlows(int time) {
		this.receivingFlow = Math.min(capacity * stepSize,
				cumulativeOutflow[time] - cumulativeInflow[time] + jamDensity * length);
		
		
		int t2 = (int) (time + 1 - length / freeFlowSpeed / stepSize);
		if (t2 < 0)
			t2 = 0;
		
		this.sendingFlow = Math.min(capacity * stepSize,
				cumulativeInflow[t2] - cumulativeOutflow[time]);
	}
}
