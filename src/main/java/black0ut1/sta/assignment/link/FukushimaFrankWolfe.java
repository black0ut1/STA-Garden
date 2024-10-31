package black0ut1.sta.assignment.link;

import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.AON;

import java.util.ArrayDeque;
import java.util.Queue;

public class FukushimaFrankWolfe extends FrankWolfe {
	
	protected static final int L = 3;
	protected int k = 1;
	
	protected final Queue<double[]> aonQueue = new ArrayDeque<>(L);
	
	public FukushimaFrankWolfe(Algorithm.Parameters parameters) {
		super(parameters);
	}
	
	@Override
	protected double[] calculateTarget() {
		double[] aonFlows = new double[network.edges];
		AON.assign(network, odMatrix, costs, aonFlows);
		aonQueue.add(aonFlows);
		
		if (k++ < L) // queue must contain L past AON flows
			return aonFlows;
		// TODO this is not exactly as described in paper - in the paper,
		// the weighted sum of past AONs starts immediately
		// TODO using queue is not optimal - use array as circular queue
		
		double weight = 1.0 / L;
		
		double[] target = new double[network.edges]; // sum of past L weighted AONs
		for (double[] AONs : aonQueue) {
			for (int i = 0; i < AONs.length; i++)
				target[i] += weight * AONs[i];
		}
		
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
		
		aonQueue.poll();
		
//		System.out.println(derivative1 + " " + derivative2 + " -> " +
//				((derivative1 <= derivative2) ? "Fukushima direction" : "FW direction"));
		if (derivative1 <= derivative2)
			return target;
		else
			return aonFlows;
	}
}
