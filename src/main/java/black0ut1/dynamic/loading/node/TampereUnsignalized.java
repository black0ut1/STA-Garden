package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.link.Link;

import java.util.HashSet;
import java.util.Vector;

/**
 * Node model for general intersection with arbitrary number of
 * incoming and outgoing links.
 * TODO optimize: use boolean[] instead of HashSet, OK because there are only a few incoming/outgoing links
 * TODO if sets aren't larger than 64/32/16, use long/int/short as a bitset
 * Bibliography:
 * - (Tampere et al., 2011) A generic class of first order node models
 * for dynamic macroscopic simulation of traffic flows
 */
public class TampereUnsignalized extends Node {
	
	public TampereUnsignalized(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	protected double[][] computeOrientedFlows(double[][] turningFractions) {
		double[][] orientedFlows = new double[incomingLinks.length][outgoingLinks.length];
		
		
		// 1. Retrieve link constraints and initialize supplies and sets
		
		// initialize oriented sending flow for each pair of incoming
		// and outgoing link
		double[][] orientedSendingFlow = new double[incomingLinks.length][outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedSendingFlow[i][j] = turningFractions[i][j] * incomingLinks[i].getSendingFlow();
		
		// initialize receiving flow of each outgoing link
		double[] R = new double[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++)
			R[j] = outgoingLinks[j].getReceivingFlow();
		
		// for each outgoing link j, initialize set Uj of all incoming
		// links that compete for Rj
		HashSet<Integer>[] U = new HashSet[outgoingLinks.length];
		
		// set of outgoing links j, towards which nonzero sending flow
		// is directed
		HashSet<Integer> J = new HashSet<>();
		
		for (int j = 0; j < outgoingLinks.length; j++) {
			U[j] = new HashSet<>();
			
			// add all i competing for Rj to initial set Uj
			for (int i = 0; i < incomingLinks.length; i++)
				if (orientedSendingFlow[i][j] > 0) {
					U[j].add(i);
					J.add(j);
				}
		}
		
		
		// 2. Determine oriented capacities
		// TODO orientedCapacities must be either 0 or inf (not nan)
		double[][] orientedCapacities = new double[incomingLinks.length][outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++) {
				orientedCapacities[i][j] = orientedSendingFlow[i][j]
								/ incomingLinks[i].getSendingFlow()
								* incomingLinks[i].capacity;
			}
		
		
		double[] a = new double[outgoingLinks.length]; // level of reduction
		while (!J.isEmpty()) {
			
			// 3. Determine most restrictive constraint
			for (int j : J) {
				double denominator = 0;
				for (int i : U[j])
					denominator += orientedCapacities[i][j];
				a[j] = R[j] / denominator;
			}
			
			// TODO merge ^ and v into one loop
			
			double minA = Double.POSITIVE_INFINITY;
			int minJ = -1;
			for (int j : J)
				if (a[j] <= minA) {
					minA = a[j];
					minJ = j;
				}
			
			// 4. Determine flows of corresponding set U[minJ] and
			// recalculate Rj
			boolean exists = false;
			for (int i : U[minJ])
				if (incomingLinks[i].getSendingFlow() <= minA * incomingLinks[i].capacity) {
					exists = true;
					break;
				}
			
			// (a) at least one i in U[minJ] is constrained
			if (exists) {
				
				for (int i : new HashSet<>(U[minJ])) {
					if (incomingLinks[i].getSendingFlow() <= minA * incomingLinks[i].capacity) {
						
						for (int j = 0; j < outgoingLinks.length; j++)
							orientedFlows[i][j] = orientedSendingFlow[i][j];
						
						Vector<Integer> remove = new Vector<>();
						for (int j : J) {
							R[j] -= orientedSendingFlow[i][j];
							U[j].remove(i);
							
							if (U[j].isEmpty()) {
								a[j] = 1;
								remove.add(j);
							}
						}
						remove.forEach(J::remove);
					}
				}
				
			} else {
				
				for (int i : U[minJ]) {
					for (int j = 0; j < outgoingLinks.length; j++)
						orientedFlows[i][j] = minA * orientedCapacities[i][j];
					
					Vector<Integer> remove = new Vector<>();
					for (int j : J) {
						R[j] -= minA * orientedCapacities[i][j];
						
						if (j != minJ) {
							U[j].removeAll(U[minJ]);
							
							if (U[j].isEmpty()) {
								a[j] = 1;
								remove.add(j);
							}
						} else {
							a[j] = minA;
							remove.add(j);
						}
					}
					remove.forEach(J::remove);
				}
			}
		}
		
		return orientedFlows;
	}
}
