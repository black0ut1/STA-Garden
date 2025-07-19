package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

public class FukushimaFrankWolfe extends FrankWolfe {
	
	protected final int L;
	protected int currL = 0;
	protected int queueEnd = 0;
	
	protected final double[][] queue;
	
	public FukushimaFrankWolfe(Network network, DoubleMatrix odMatrix,
							   CostFunction costFunction, int maxIterations,
							   Convergence.Builder convergenceBuilder) {
		this(network, odMatrix, costFunction, maxIterations, convergenceBuilder, 2);
	}
	
	public FukushimaFrankWolfe(Network network, DoubleMatrix odMatrix,
							   CostFunction costFunction, int maxIterations,
							   Convergence.Builder convergenceBuilder, int L) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
		this.L = L;
		this.queue = new double[L][];
	}
	
	
	@Override
	protected double[] calculateTarget() {
		double[] aonFlows = new double[network.edges];
		AON.assign(network, odMatrix, costs, aonFlows);
		
		// add the AON flow to queue
		if (currL < L) { // the queue is filling up
			queue[currL++] = aonFlows;
		} else { // the queue if filled up, we replace last element
			queue[queueEnd] = aonFlows;
			queueEnd = (queueEnd + 1) % L;
		}
		
		// compute arithmetic mean of flows in queue
		double[] target = new double[network.edges];
		for (int j = 0; j < network.edges; j++) {
			for (int i = 0; i < currL; i++)
				target[j] += queue[i][j];
			target[j] /= currL;
		}
		
		// determine if target or aonFlows is better direction
		double derivative1Numer = 0, derivative1Denom = 0;
		double derivative2Numer = 0, derivative2Denom = 0;
		for (int i = 0; i < network.edges; i++) {
			derivative1Numer += costs[i] * (target[i] - flows[i]);
			derivative1Denom += (target[i] - flows[i]) * (target[i] - flows[i]);
			
			derivative2Numer += costs[i] * (aonFlows[i] - flows[i]);
			derivative2Denom += (aonFlows[i] - flows[i]) * (aonFlows[i] - flows[i]);
		}
		double derivative1 = derivative1Numer / derivative1Denom;
		double derivative2 = derivative2Numer / derivative2Denom;
		
		
		System.out.println(derivative1 + " " + derivative2 + " -> " +
				((derivative1 < derivative2) ? "Fukushima direction" : "FW direction"));
		if (derivative1 < derivative2)
			return target;
		else
			return aonFlows;
	}
}
