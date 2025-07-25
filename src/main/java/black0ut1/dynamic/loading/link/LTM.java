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
	
	public double psi;
	public double phi;
	
	public LTM(int index, double stepSize, int timeSteps, double length,
			   double capacity, double jamDensity, double freeFlowSpeed,
			   double backwardWaveSpeed) {
		super(index, stepSize, timeSteps, length, capacity,
				jamDensity, freeFlowSpeed, backwardWaveSpeed);
	}
	
	public void computeReceivingFlow(int time) {
		double t1 = time + 1 - length / backwardWaveSpeed / stepSize;
		// t1 should not be larger than time unless time step is too large
		if (t1 < 0)
			t1 = 0;
		
		double psi = t1 - (int) t1;
		double interpolatedOutflow = (1 - psi) * cumulativeOutflow[(int) t1] + psi * cumulativeOutflow[(int) t1 + 1];
		this.receivingFlow = Math.min(capacity * stepSize,
				Math.max(0, interpolatedOutflow - cumulativeInflow[time] + jamDensity * length));
		
		this.psi = (t1 > time) ? psi : 0;
	}
	
	public void computeSendingFlow(int time) {
		double t2 = time + 1 - length / freeFlowSpeed / stepSize;
		// t2 should not be larger than time unless time step is too large
		if (t2 < 0)
			t2 = 0;
		
		double phi = t2 - (int) t2;
		double interpolatedInflow = (1 - phi) * cumulativeInflow[(int) t2] + phi * cumulativeInflow[(int) t2 + 1];
		this.sendingFlow = Math.min(capacity * stepSize,
				Math.max(0, interpolatedInflow - cumulativeOutflow[time]));
		
		this.phi = (t2 > time) ? phi : 0;
	}
	
	/**
	 * Recreated example from Transportation Network Analysis, p. 381.
	 */
	public static void main(String[] args) {
		LTM ltm = new LTM(0, 1, 21, 3, 10, 30, 1, 3.0 / 4);
		
		System.out.println(" t | d(t)  R(t) Inflow | N^(t) Nv(t) | S(t) Outflow Vehicles on link");
		
		double[] flowSent = {10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0};
		for (int t = 0; t <= 20; t++) {
			
			ltm.computeReceivingFlow(t);
			ltm.computeSendingFlow(t);
			
			if (ltm.getReceivingFlow() < flowSent[t]) {
				MixtureFlow mf = new MixtureFlow(ltm.getReceivingFlow(), new int[0], new double[0], 0);
				
				ltm.inflow[t] = mf;
				ltm.cumulativeInflow[t + 1] = ltm.cumulativeInflow[t] + mf.totalFlow;
				
				// carry over unsent flow
				flowSent[t + 1] += (flowSent[t] - ltm.getReceivingFlow());
			} else {
				MixtureFlow mf = new MixtureFlow(flowSent[t], new int[0], new double[0], 0);
				
				ltm.inflow[t] = mf;
				ltm.cumulativeInflow[t + 1] = ltm.cumulativeInflow[t] + mf.totalFlow;
			}
			
			double flowExited = Math.min(10, ltm.getSendingFlow());
			if (t < 10) // red light until time 10
				flowExited = 0;
			MixtureFlow mf = new MixtureFlow(flowExited, new int[0], new double[0], 0);
			
			ltm.outflow[t] = mf;
			ltm.cumulativeOutflow[t + 1] = ltm.cumulativeOutflow[t] + mf.totalFlow;
			
			System.out.printf("%2d | %4.1f  %4.1f %6.1f | %4.1f %5.1f  | %4.1f %6.1f %8.1f %n",
					t, flowSent[t], ltm.getReceivingFlow(), ltm.inflow[t].totalFlow,
					ltm.cumulativeInflow[t], ltm.cumulativeOutflow[t],
					ltm.sendingFlow, ltm.outflow[t].totalFlow,
					ltm.cumulativeInflow[t] - ltm.cumulativeOutflow[t]);
		}
	}
}
