package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.IntSet;
import black0ut1.dynamic.loading.link.Link;

/**
 * Node model for general intersection with arbitrary number of
 * incoming and outgoing links.
 * Bibliography:
 * - (Tampere et al., 2011) A generic class of first order node models
 * for dynamic macroscopic simulation of traffic flows
 */
public class TampereUnsignalized extends Intersection {
	
	public TampereUnsignalized(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix turningFractions) {
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		
		
		// 1. Retrieve link constraints and initialize supplies and sets
		
		// initialize oriented sending flow for each pair of incoming
		// and outgoing link
		DoubleMatrix orientedSendingFlow = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedSendingFlow.set(i, j, turningFractions.get(i, j) * incomingLinks[i].getSendingFlow());
		
		// initialize receiving flow of each outgoing link
		double[] R = new double[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++)
			R[j] = outgoingLinks[j].getReceivingFlow();
		
		// for each outgoing link j, initialize set Uj of all incoming
		// links that compete for Rj
		IntSet[] U = new IntSet[outgoingLinks.length];
		
		// set of outgoing links j, towards which nonzero sending flow
		// is directed
		IntSet J = new IntSet(outgoingLinks.length);
		
		for (int j = 0; j < outgoingLinks.length; j++) {
			U[j] = new IntSet(incomingLinks.length);
			
			// add all i competing for Rj to initial set Uj
			for (int i = 0; i < incomingLinks.length; i++)
				if (orientedSendingFlow.get(i, j) > 0) {
					U[j].add(i);
					J.add(j);
				}
		}
		
		
		// 2. Determine oriented capacities
		DoubleMatrix orientedCapacities = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++) {
				orientedCapacities.set(i, j,
						orientedSendingFlow.get(i, j)
								/ incomingLinks[i].getSendingFlow()
								* incomingLinks[i].capacity);
			}
		
		
		double[] a = new double[outgoingLinks.length]; // level of reduction
		while (!J.isEmpty()) {
			
			// 3. Determine most restrictive constraint
			double minA = Double.POSITIVE_INFINITY;
			int minJ = -1;
			
			for (int j = 0; j < J.size; j++) {
				if (J.contains(j)) {
					double denominator = 0;
					
					for (int i = 0; i < U[j].size; i++) {
						if (U[j].contains(i))
							denominator += orientedCapacities.get(i, j);
					}
					a[j] = R[j] / denominator;
					
					if (a[j] <= minA) {
						minA = a[j];
						minJ = j;
					}
				}
			}
			
			// 4. Determine flows of corresponding set U[minJ] and
			// recalculate Rj
			boolean exists = false;
			for (int i = 0; i < U[minJ].size; i++)
				if (U[minJ].contains(i)) {
					
					if (incomingLinks[i].getSendingFlow() <= minA * incomingLinks[i].capacity) {
						exists = true;
						break;
					}
				}
			
			// (a) at least one i in U[minJ] is constrained
			if (exists) {
				
				for (int i = 0; i < U[minJ].size; i++)
					if (U[minJ].contains(i)) {
						
						if (incomingLinks[i].getSendingFlow() <= minA * incomingLinks[i].capacity) {
							
							for (int j = 0; j < outgoingLinks.length; j++)
								orientedFlows.set(i, j,
										orientedSendingFlow.get(i, j));
							
							for (int j = 0; j < J.size; j++)
								if (J.contains(j)) {
									R[j] -= orientedSendingFlow.get(i, j);
									U[j].remove(i);
									
									if (U[j].isEmpty()) {
										a[j] = 1;
										J.remove(j);
									}
								}
						}
					}
				
			} else {
				
				for (int i = 0; i < U[minJ].size; i++)
					if (U[minJ].contains(i)) {
						
						for (int j = 0; j < outgoingLinks.length; j++)
							orientedFlows.set(i, j,
									minA * orientedCapacities.get(i, j));
						
						for (int j = 0; j < J.size; j++)
							if (J.contains(j)) {
								
								R[j] -= minA * orientedCapacities.get(i, j);
								
								if (j != minJ) {
									U[j].removeAll(U[minJ]);
									
									if (U[j].isEmpty()) {
										a[j] = 1;
										J.remove(j);
									}
								} else {
									a[j] = minA;
									J.remove(j);
								}
							}
					}
			}
		}
		
		return orientedFlows;
	}
}
