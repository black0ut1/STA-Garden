package black0ut1.dynamic.loading.node.models.inm;

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
	
	public ApproximateINMC(INM inm, DemandConstraintFunction demandConstraints) {
		super(inm, demandConstraints);
	}
	
	@Override
	public Pair<double[], double[]> computeTotalInflowsOutflows(
			DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		
		// 1. Calculate working point A
		// 1. (a) q_A = INM(Delta, Sigma)
		var pairA = inm.computeTotalInflowsOutflows(totalTurningFractions, sendingFlows, receivingFlows);
		double[] inflowsA = pairA.first();
		double[] outflowsA = pairA.second();
		
		// 1. (b) Delta_A = min{Delta, Delta-hat(q_A)}
		double[] sendingFlowsA = new double[sendingFlows.length];
		double[] sendingFlowsConstraints = demandConstraints.demand(inflowsA,  outflowsA);
		for (int i = 0; i < sendingFlows.length; i++)
			sendingFlowsA[i] = Math.min(sendingFlows[i], sendingFlowsConstraints[i]);
		
		// 2. Calculate working point B
		// 2. (a) q_B = INM(Delta_A, Sigma)
		var pairB = inm.computeTotalInflowsOutflows(totalTurningFractions, sendingFlowsA, receivingFlows);
		double[] inflowsB = pairB.first();
		double[] outflowsB = pairB.second();
		
		// 2. (b) Delta_B = min{Delta, Delta-hat(q_B)}
		double[] sendingFlowsB = new double[receivingFlows.length];
		sendingFlowsConstraints = demandConstraints.demand(inflowsB,  outflowsB);
		for (int i = 0; i < sendingFlows.length; i++)
			sendingFlowsB[i] = Math.min(sendingFlows[i], sendingFlowsConstraints[i]);
		
		// 3. Solve linearized model
		double lambda = 1;
		for (int i = 0; i < sendingFlows.length; i++)
			if (!(sendingFlowsA[i] == inflowsA[i] && sendingFlowsB[i] == inflowsB[i])) {
				double numerator = sendingFlowsB[i] - inflowsB[i];
				double denominator = (sendingFlowsB[i] - inflowsB[i]) - (sendingFlowsA[i] - inflowsA[i]);
				lambda = Math.min(lambda, numerator / denominator);
			}
		
		double[] inflows = new double[sendingFlows.length];
		for (int i = 0; i < sendingFlows.length; i++)
			inflows[i] = inflowsB[i] + lambda * (inflowsA[i] - inflowsB[i]);
		
		double[] outflows = new double[receivingFlows.length];
		for (int j = 0; j < receivingFlows.length; j++)
			outflows[j] = outflowsB[j] + lambda * (outflowsA[j] - outflowsB[j]);
		
		return new Pair<>(inflows, outflows);
	}
}
