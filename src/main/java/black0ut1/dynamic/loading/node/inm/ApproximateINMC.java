package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;

/**
 * This class represent the approximate solver of the INMC problem as described in
 * (Flotterod and Rohde, 2011), Algorithm 3.
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public class ApproximateINMC extends INMC {
	
	public ApproximateINMC(INM inm, DemandConstraintFunction[] demandConstraints) {
		super(inm, demandConstraints);
	}
	
	@Override
	protected Pair<double[], double[]> computeInflowsOutflows(DoubleMatrix totalTurningFractions) {
		// 1. Calculate working point A
		double[] sendingFlows = new double[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			sendingFlows[i] = incomingLinks[i].getSendingFlow();
		
		double[] receivingFlows = new double[outgoingLinks.length];
		for (int i = 0; i < outgoingLinks.length; i++)
			receivingFlows[i] = outgoingLinks[i].getReceivingFlow();
		
		// 1. (a) q_A = INM(Delta, Sigma)
		var pairA = inm.computeOrientedFlows(totalTurningFractions, sendingFlows, receivingFlows);
		double[] inflowsA = pairA.first();
		double[] outflowsA = pairA.second();
		
		// 1. (b) Delta_A = min{Delta, Delta-hat(q_A)}
		double[] sendingFlowsA = new double[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			sendingFlowsA[i] = Math.min(sendingFlows[i], demandConstraints[i].demand(pairA.first(),  pairA.second()));
		
		// 2. Calculate working point B
		// 2. (a) q_B = INM(Delta_A, Sigma)
		var pairB = inm.computeOrientedFlows(totalTurningFractions, sendingFlowsA, receivingFlows);
		double[] inflowsB = pairB.first();
		double[] outflowsB = pairB.second();
		
		// 2. (b) Delta_B = min{Delta, Delta-hat(q_B)}
		double[] sendingFlowsB = new double[outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			sendingFlowsB[i] = Math.min(sendingFlows[i], demandConstraints[i].demand(pairB.first(),  pairB.second()));
		
		// 3. Solve linearized model
		double lambda = 1;
		for (int i = 0; i < incomingLinks.length; i++)
			if (!(sendingFlowsA[i] == inflowsA[i] && sendingFlowsB[i] == inflowsB[i])) {
				double numerator = sendingFlowsB[i] - inflowsB[i];
				double denominator = (sendingFlowsB[i] - inflowsB[i]) - (sendingFlowsA[i] - inflowsA[i]);
				lambda = Math.min(lambda, numerator / denominator);
			}
		
		double[] inflows = new double[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			inflows[i] = inflowsB[i] + lambda * (inflowsA[i] - inflowsB[i]);
			
		double[] outflows = new double[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++)
			outflows[j] = outflowsB[j] + lambda * (outflowsA[j] - outflowsB[j]);
		
		return new Pair<>(inflows, outflows);
	}
}
