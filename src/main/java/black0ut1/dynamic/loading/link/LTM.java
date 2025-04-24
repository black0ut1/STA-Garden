package black0ut1.dynamic.loading.link;


import black0ut1.dynamic.loading.mixture.MixtureFlow;

/**
 * Link transmission model.
 * Bibliography:
 * - (Yperman et al., 2005) THE LINK TRANSMISSION MODEL: AN EFFICIENT
 * IMPLEMENTATION OF THE KINEMATIC WAVE THEORY IN TRAFFIC NETWORKS
 * - (Yperman, 2007) PhD thesis
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.5.2
 */
public class LTM extends Link {
	
	public LTM(int index, double stepSize, int timeSteps, double length,
			   double capacity, double jamDensity, double freeFlowSpeed,
			   double backwardWaveSpeed) {
		super(index, stepSize, timeSteps, length, capacity,
				jamDensity, freeFlowSpeed, backwardWaveSpeed);
	}
	
	@Override
	public void computeReceivingAndSendingFlows(int time) {
		// TODO do not floor, interpolate, also precompute
		int t1 = (int) (time + 1 - length / backwardWaveSpeed / stepSize);
		if (t1 < 0)
			t1 = 0;
		
		this.receivingFlow = Math.min(capacity * stepSize,
				cumulativeOutflow[t1] - cumulativeInflow[time] + jamDensity * length);
		
		
		int t2 = (int) (time + 1 - length / freeFlowSpeed / stepSize);
		if (t2 < 0)
			t2 = 0;
		
		this.sendingFlow = Math.min(capacity * stepSize,
				cumulativeInflow[t2] - cumulativeOutflow[time]);
	}
	
	/**
	 * Recreated example from Transportation Network Analysis, p. 381.
	 */
	public static void main(String[] args) {
		LTM ltm = new LTM(0, 1, 21, 3, 10, 30, 1, 3.0 / 4);
		
		System.out.println(" t | d(t)  R(t) Inflow | N^(t) Nv(t) | S(t) Outflow Vehicles on link");
		
		double[] flowSent = {10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0};
		for (int t = 0; t <= 20; t++) {
			
			ltm.computeReceivingAndSendingFlows(t);
			
			if (ltm.getReceivingFlow() < flowSent[t]) {
				MixtureFlow mf = new MixtureFlow(ltm.getReceivingFlow(), new int[0], new double[0], 0);
				ltm.enterFlow(t, mf);
				
				// carry over unsent flow
				flowSent[t + 1] += (flowSent[t] - ltm.getReceivingFlow());
			} else {
				MixtureFlow mf = new MixtureFlow(flowSent[t], new int[0], new double[0], 0);
				ltm.enterFlow(t, mf);
			}
			
			double flowExited = Math.min(10, ltm.getSendingFlow());
			if (t < 10) // red light until time 10
				flowExited = 0;
			ltm.exitFlow(t, flowExited);
			
			System.out.printf("%2d | %4.1f  %4.1f %6.1f | %4.1f %5.1f  | %4.1f %6.1f %8.1f %n",
					t, flowSent[t], ltm.getReceivingFlow(), ltm.inflow[t].totalFlow,
					ltm.cumulativeInflow[t], ltm.cumulativeOutflow[t],
					ltm.sendingFlow, ltm.outflow[t].totalFlow,
					ltm.cumulativeInflow[t] - ltm.cumulativeOutflow[t]);
		}
	}
}
