package black0ut1.dynamic.loading.node.models.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;

/**
 * The basic INM solver as described in (Flotterod and Rohde, 2011), Algorithm 1. Here,
 * basic means that priorities of incoming links are constants independent of the flows.
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public class BasicINM extends INM {
	
	protected final double[] priorities;
	
	public BasicINM(double[] priorities) {
		this.priorities = priorities;
	}
	
	@Override
	public Pair<double[], double[]> computeTotalInflowsOutflows(
			DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		// 2. Compute initial flows according to (15)
		double[] inflows = new double[sendingFlows.length];
		double[] outflows = new double[receivingFlows.length];
		
		// 3. Compute initial set D according to (18)
		BitSet32 D = determineUnconstrainedLinks(
				totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		
		// 4. While D != {}
		while (!D.isClear()) {
			
			// (a) Compute psi(q) according to (19)
			double[] psi_in = new double[sendingFlows.length];
			for (int i = 0; i < sendingFlows.length; i++)
				psi_in[i] = (D.get(i) ? 1 : 0) * priorities[i];
			
			double[] psi_out = new double[receivingFlows.length];
			for (int i = 0; i < sendingFlows.length; i++)
				for (int j = 0; j < receivingFlows.length; j++)
					psi_out[j] += totalTurningFractions.get(i, j) * psi_in[i];
			
			// (b) Compute theta according to (24)
			double theta = Double.POSITIVE_INFINITY;
			for (int i = 0; i < sendingFlows.length; i++)
				if (D.get(i)) {
					double factor = (sendingFlows[i] - inflows[i]) / psi_in[i];
					theta = Math.min(theta, factor);
				}
			for (int j = 0; j < receivingFlows.length; j++)
				if (D.get(sendingFlows.length + j)) {
					double factor = (receivingFlows[j] - outflows[j]) / psi_out[j];
					theta = Math.min(theta, factor);
				}
			
			// (c) q = q + theta * psi(q) according to (23)
			for (int i = 0; i < sendingFlows.length; i++)
				inflows[i] += theta * psi_in[i];
			for (int j = 0; j < receivingFlows.length; j++)
				outflows[j] += theta * psi_out[j];
			
			// (d) D = D(q) according to (18)
			D = determineUnconstrainedLinks(
					totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		}
		
		return new Pair<>(inflows, outflows);
	}
}
