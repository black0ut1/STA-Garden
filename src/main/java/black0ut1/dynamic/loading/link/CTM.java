package black0ut1.dynamic.loading.link;


import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.util.Util;

public class CTM extends Link {
	
	protected final double[] cells;
	protected final double[] cellFlow;
	
	protected final double delta;
	protected final double cellMaxVeh;
	
	public CTM(int index, double stepSize, int timeSteps, double length,
			   double capacity, double jamDensity, double freeFlowSpeed,
			   double backwardWaveSpeed) {
		super(index, stepSize, timeSteps, length, capacity,
				jamDensity, freeFlowSpeed, backwardWaveSpeed);
		
		int cellsNum = (int) (this.length / (this.freeFlowSpeed / stepSize));
		double cellSize = this.length / cellsNum;
		
		this.cells = new double[cellsNum];
		this.cellFlow = new double[cellsNum + 1];
		
		this.delta = this.backwardWaveSpeed / this.freeFlowSpeed;
		this.cellMaxVeh = this.jamDensity * cellSize;
	}
	
	@Override
	public void computeReceivingFlow(int time) {
		this.receivingFlow = Math.min(capacity * stepSize, delta * (cellMaxVeh - cells[0]));
	}
	
	@Override
	public void computeSendingFlow(int time) {
		this.sendingFlow = Math.min(capacity * stepSize, cells[cells.length - 1]);
	}
	
	public void advanceFlow(int time) {
		if (time > 0) {
			cellFlow[0] = inflow[time - 1].totalFlow;
			cellFlow[cellFlow.length - 1] = outflow[time - 1].totalFlow;
		}
		
		for (int x = 0; x < cells.length; x++)
			cells[x] = cells[x] + cellFlow[x] - cellFlow[x + 1];
		
		for (int x = 1; x < cellFlow.length - 1; x++)
			cellFlow[x] = Util.min(cells[x - 1], capacity * stepSize, delta * (cellMaxVeh - cells[x]));
	}
	
	/**
	 * Recreated example from Transportation Network Analysis, p. 378.
	 */
	public static void main(String[] args) {
		CTM ctm = new CTM(0, 1, 21, 3, 10, 30, 1, 2.0 / 3);
		
		System.out.println(" t | d(t)  R(t) | y(0, t) n(0, t) y(1, t) n(1, t) y(2, t) n(2, t) | S(t) y(3, t)");
		
		double[] flowSent = {10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0};
		for (int t = 0; t <= 20; t++) {
			
			ctm.advanceFlow(t);
			ctm.computeSendingFlow(t);
			ctm.computeReceivingFlow(t);
			
			if (ctm.getReceivingFlow() < flowSent[t]) {
				MixtureFlow mf = new MixtureFlow(ctm.getReceivingFlow(), new int[0], new double[0], 0);
				
				ctm.inflow[t] = mf;
				ctm.cumulativeInflow[t + 1] = ctm.cumulativeInflow[t] + mf.totalFlow;
				
				// carry over unsent flow
				flowSent[t + 1] += (flowSent[t] - ctm.getReceivingFlow());
			} else {
				MixtureFlow mf = new MixtureFlow(flowSent[t], new int[0], new double[0], 0);
				
				ctm.inflow[t] = mf;
				ctm.cumulativeInflow[t + 1] = ctm.cumulativeInflow[t] + mf.totalFlow;
			}
			
			double flowExited = Math.min(10, ctm.getSendingFlow());
			if (t < 10) // red light until time 10
				flowExited = 0;
			MixtureFlow mf = new MixtureFlow(flowExited, new int[0], new double[0], 0);
			
			ctm.outflow[t] = mf;
			ctm.cumulativeOutflow[t + 1] = ctm.cumulativeOutflow[t] + mf.totalFlow;
			
			
			System.out.format("%2d | %4.1f  %4.1f | %6.1f  %6.1f  %6.1f  %6.1f  %6.1f  %6.1f  | %4.1f %6.1f %n",
					t, flowSent[t], ctm.getReceivingFlow(),
					ctm.cellFlow[0], ctm.cells[0], ctm.cellFlow[1],
					ctm.cells[1], ctm.cellFlow[2], ctm.cells[2],
					ctm.getSendingFlow(), ctm.cellFlow[3]);
		}
	}
}
