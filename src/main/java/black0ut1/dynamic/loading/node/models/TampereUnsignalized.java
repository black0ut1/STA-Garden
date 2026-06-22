package black0ut1.dynamic.loading.node.models;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;

/**
 * Node model for general intersection with arbitrary number of
 * incoming and outgoing links.
 * Bibliography:
 * - (Tampere et al., 2011) A generic class of first order node models
 * for dynamic macroscopic simulation of traffic flows
 */
public class TampereUnsignalized implements NodeModel {
	
	protected final double[] priorities;
	
	public TampereUnsignalized(double[] priorities) {
		this.priorities = priorities;
	}

	@Override
	public Pair<double[], double[]> computeTotalInflowsOutflows(
			DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		DoubleMatrix orientedFlows = new DoubleMatrix(sendingFlows.length, receivingFlows.length);
		
		
		// 1. Retrieve link constraints and initialize supplies and sets
		
		// initialize oriented sending flow for each pair of incoming
		// and outgoing link
		DoubleMatrix orientedSendingFlow = new DoubleMatrix(sendingFlows.length, receivingFlows.length);
		for (int i = 0; i < sendingFlows.length; i++)
			for (int j = 0; j < receivingFlows.length; j++)
				orientedSendingFlow.set(i, j, totalTurningFractions.get(i, j) * sendingFlows[i]);
		
		// initialize reduced receiving flow of each outgoing link
		double[] R = new double[receivingFlows.length];
		System.arraycopy(receivingFlows, 0, R, 0, receivingFlows.length);
		
		// for each outgoing link j, initialize set Uj of all incoming
		// links that compete for Rj
		BitSet32[] U = new BitSet32[receivingFlows.length];
		
		// set of outgoing links j, towards which nonzero sending flow
		// is directed
		BitSet32 J = new BitSet32(receivingFlows.length);
		
		for (int j = 0; j < receivingFlows.length; j++) {
			U[j] = new BitSet32(sendingFlows.length);
			
			// add all i competing for Rj to initial set Uj
			for (int i = 0; i < sendingFlows.length; i++)
				if (orientedSendingFlow.get(i, j) > 0) {
					U[j].set(i);
					J.set(j);
				}
		}
		
		
		// 2. Determine oriented capacities
		DoubleMatrix orientedCapacities = new DoubleMatrix(sendingFlows.length, receivingFlows.length);
		for (int i = 0; i < sendingFlows.length; i++)
			for (int j = 0; j < receivingFlows.length; j++) {
				orientedCapacities.set(i, j,
						totalTurningFractions.get(i, j) * priorities[i]);
			}
		
		
		double[] LOR = new double[receivingFlows.length]; // level of reduction
		while (!J.isClear()) {
			
			// 3. Determine most restrictive constraint
			double minLOR = Double.POSITIVE_INFINITY;
			int minJ = -1;
			
			for (int j = 0; j < J.size; j++) {
				if (J.get(j)) {
					double denominator = 0;
					
					for (int i = 0; i < U[j].size; i++) {
						if (U[j].get(i))
							denominator += orientedCapacities.get(i, j);
					}
					LOR[j] = R[j] / denominator;
					
					if (LOR[j] <= minLOR) {
						minLOR = LOR[j];
						minJ = j;
					}
				}
			}
			
			// 4. Determine flows of corresponding set U[minJ] and
			// recalculate Rj
			
			// (a) at least one i in U[minJ] is sending flow (demand)
			// constrained
			boolean anyDemandConstrained = false;
			for (int i = 0; i < U[minJ].size; i++)
				if (U[minJ].get(i))
					if (sendingFlows[i] <= minLOR * priorities[i]) {
						anyDemandConstrained = true;
						
						for (int j = 0; j < receivingFlows.length; j++)
							orientedFlows.set(i, j,
									orientedSendingFlow.get(i, j));
						
						for (int j = 0; j < J.size; j++)
							if (J.get(j)) {
								R[j] -= orientedSendingFlow.get(i, j);
								U[j].clear(i);
								
								if (U[j].isClear()) {
									LOR[j] = 1;
									J.clear(j);
								}
							}
					}
			
			// (b) all links of U[minJ] are constrained by receiving
			// flow (supply) of link minJ
			if (!anyDemandConstrained)
				for (int i = 0; i < U[minJ].size; i++)
					if (U[minJ].get(i)) {
						
						for (int j = 0; j < receivingFlows.length; j++)
							orientedFlows.set(i, j, minLOR * orientedCapacities.get(i, j));
						
						for (int j = 0; j < J.size; j++)
							if (J.get(j)) {
								
								R[j] -= minLOR * orientedCapacities.get(i, j);
								
								if (j != minJ) {
									U[j].clearAll(U[minJ]);
									
									if (U[j].isClear()) {
										LOR[j] = 1;
										J.clear(j);
									}
								} else {
									LOR[j] = minLOR;
									J.clear(j);
								}
							}
					}
		}
		
		double[] inflows = new double[sendingFlows.length];
		double[] outflows = new double[receivingFlows.length];
		for (int i = 0; i < sendingFlows.length; i++)
			for (int j = 0; j < receivingFlows.length; j++) {
				inflows[i] += orientedFlows.get(i, j);
				outflows[j] += orientedFlows.get(i, j);
			}
		
		return new Pair<>(inflows, outflows);
	}
}
