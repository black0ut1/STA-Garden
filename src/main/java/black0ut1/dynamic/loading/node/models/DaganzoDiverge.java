package black0ut1.dynamic.loading.node.models;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;

/**
 * Basic node model representing one link diverging into multiple:
 *     /-->
 * -->O -->
 *     \-->
 * Bibliography:
 * - (Daganzo, 1995a)
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.2.3
 */
public class DaganzoDiverge implements NodeModel {
	
	@Override
	public Pair<double[], double[]> computeTotalInflowsOutflows(
			DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		double[] inflows = new double[1];
		double[] outflows = new double[receivingFlows.length];
		
		// its sending flow
		double S = sendingFlows[0];
		
		// 1. Compute the portion of sending flow actually sent (theta)
		double theta = 1;
		for (int j = 0; j < receivingFlows.length; j++) {
			
			double R = receivingFlows[j];
			theta = Math.min(theta, R / (S * totalTurningFractions.get(0, j)));
		}
		
		// 2. Compute outgoing flows
		for (int j = 0; j < receivingFlows.length; j++) {
			double outgoingFlow = theta * S * totalTurningFractions.get(0, j);
			outflows[j] = outgoingFlow;
			inflows[0] += outgoingFlow;
		}
		
		return new Pair<>(inflows, outflows);
	}
}
