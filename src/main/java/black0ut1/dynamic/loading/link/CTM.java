package black0ut1.dynamic.loading.link;


import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.util.Util;

public class CTM extends Link {
	
	protected final double stepSize;
	protected final double[] cells;
	protected final double[] cellFlow;
	
	protected final double delta;
	protected final double cellMaxVeh;
	
	public CTM(int index, int timeSteps, double length, double capacity,
			   double jamDensity, double freeFlowSpeed, double backwardWaveSpeed, double stepSize) {
		super(index, timeSteps, length, capacity, jamDensity, freeFlowSpeed, backwardWaveSpeed);
		
		int cellsNum = (int) (this.length / (this.freeFlowSpeed / stepSize));
		double cellSize = this.length / cellsNum;
		
		this.cells = new double[cellsNum];
		this.cellFlow = new double[cellsNum + 1];
		
		this.stepSize = stepSize;
		this.delta = this.backwardWaveSpeed / this.freeFlowSpeed;
		this.cellMaxVeh = this.jamDensity * cellSize;
	}
	
	@Override
	public void enterFlow(int time, MixtureFlow flow) {
		cellFlow[0] = flow.totalFlow;
		
		super.enterFlow(time, flow);
	}
	
	@Override
	public MixtureFlow exitFlow(int time, double flow) {
		cellFlow[cellFlow.length - 1] = flow;
		
		return super.exitFlow(time, flow);
	}
	
	@Override
	public void computeReceivingAndSendingFlows(int time) {
		
		for (int x = 0; x < cells.length; x++) {
			cells[x] = cells[x] + cellFlow[x] - cellFlow[x + 1];
		}
		
		for (int x = 1; x < cellFlow.length - 1; x++) {
			cellFlow[x] = Util.min(
					cells[x - 1],
					capacity * stepSize,
					delta * (cellMaxVeh - cells[x]));
		}
		
		this.receivingFlow = Math.min(capacity * stepSize, delta * (cellMaxVeh - cells[0]));
		this.sendingFlow = Math.min(capacity * stepSize, cells[cells.length - 1]);
	}
	
	/**
	 * Exactly recreated example from Transportation Network
	 * Analysis, p. 378.
	 */
	public static void main(String[] args) {
		CTM ctm = new CTM(0, 21, 3, 10, 30, 1, 2.0 / 3, 1);
		
		System.out.println(" t | d(t)  R(t) | y(0, t) n(0, t) y(1, t) n(1, t) y(2, t) n(2, t) | S(t) y(3, t)");
		
		double[] flowSent = {10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0};
		for (int t = 0; t <= 20; t++) {
			
			ctm.computeReceivingAndSendingFlows(t);
			
			if (ctm.getReceivingFlow() < flowSent[t]) {
				MixtureFlow mf = new MixtureFlow(ctm.getReceivingFlow(), new int[0], new double[0], 0);
				ctm.enterFlow(t, mf);
				
				// carry over unsent flow
				flowSent[t + 1] += (flowSent[t] - ctm.getReceivingFlow());
			} else {
				MixtureFlow mf = new MixtureFlow(flowSent[t], new int[0], new double[0], 0);
				ctm.enterFlow(t, mf);
			}
			
			// red light until time 10
			if (t >= 10) {
				double flowExited = Math.min(10, ctm.getSendingFlow());
				ctm.exitFlow(t, flowExited);
			}
			
			System.out.format("%2d | %4.1f  %4.1f | %6.1f  %6.1f  %6.1f  %6.1f  %6.1f  %6.1f  | %4.1f %6.1f %n",
					t, flowSent[t], ctm.getReceivingFlow(),
					ctm.cellFlow[0], ctm.cells[0], ctm.cellFlow[1],
					ctm.cells[1], ctm.cellFlow[2], ctm.cells[2],
					ctm.getSendingFlow(), ctm.cellFlow[3]);
		}
	}
}
