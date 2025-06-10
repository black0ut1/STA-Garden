package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.BitSet32;
import black0ut1.dynamic.loading.link.Link;

/**
 * Bibliography:													  <br>
 * - (Tampere et al., 2011) A generic class of first order node models
 * for dynamic macroscopic simulation of traffic flows
 */
public class TampereSignalized extends Intersection {
	
	/**
	 * Each value of this array contain a number from interval (0, 1]
	 * representing the fraction of total cycle time a phase takes.
	 * I.e., Phase p takes {@code (total cycle time) * alphas[p]}
	 * time.
	 * <p>
	 * By definition, sum of this array must equal to 1.
	 */
	protected final double[] alphas;
	/**
	 * Each set in this 2D array represents a set of outgoing links,
	 * for which turns are allowed for incoming link i during phase p.
	 * I.e., set {@code allowedLinks[p][i]} is a set of indices into
	 * array {@code outgoingLinks} of all links which can be used as
	 * exit for traffic incoming from i during phase p.
	 * <p>
	 * Each {@code IntSet} should be of size {@code outgoingLinks.length}.
	 * <p>
	 * The sets must be disjoint for the same i. I.e., a turning
	 * movement ij is allowed during only one phase.
	 */
	protected final BitSet32[][] allowedLinks;
	/**
	 * Each value in this 2D array is a fraction of the total capacity
	 * of incoming link i, that is available during phase p. I.e.,
	 * {@code betas[p][i]} is a fraction of C<sub>i</sub> that is
	 * available for all outgoing links {@code allowedLinks[p][i]}.
	 * <p>
	 * By definition, sum of {@code betas[p][i]} over p must equal to
	 * 1 for each i.
	 */
	protected final double[][] betas;
	
	/** Equation (33). */
	protected final double[][] nodeSupplyConstraints;
	
	
	public TampereSignalized(int index, Link[] incomingLinks, Link[] outgoingLinks,
							 double[] alphas, double[][] betas, BitSet32[][] allowedLinks) {
		super(index, incomingLinks, outgoingLinks);
		
		this.alphas = alphas;
		this.betas = betas;
		this.allowedLinks = allowedLinks;
		
		this.nodeSupplyConstraints = new double[alphas.length][incomingLinks.length];
		for (int p = 0; p < alphas.length; p++)
			for (int i = 0; i < incomingLinks.length; i++)
				nodeSupplyConstraints[p][i] = alphas[p] * betas[p][i] * incomingLinks[i].capacity;
	}
	
	@Override
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix turningFractions) {
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		
		
		// 1. Retrieve link constraints and initialize supplies and sets
		
		// initialize oriented sending flow for each pair of incoming
		// and outgoing link
		DoubleMatrix orientedSendingFlow = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedSendingFlow.set(i, j, turningFractions.get(i, j) * incomingLinks[i].getSendingFlow());
		
		// initialize reduced receiving flow of each outgoing link
		double[] R = new double[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++)
			R[j] = outgoingLinks[j].getReceivingFlow();
		
		// for each outgoing link j, initialize set Uj of all incoming
		// links that compete for Rj
		BitSet32[] U = new BitSet32[outgoingLinks.length];
		
		// set of outgoing links j, towards which nonzero sending flow
		// is directed
		BitSet32 J = new BitSet32(outgoingLinks.length);
		
		for (int j = 0; j < outgoingLinks.length; j++) {
			U[j] = new BitSet32(incomingLinks.length);
			
			// add all i competing for Rj to initial set Uj
			for (int i = 0; i < incomingLinks.length; i++)
				if (orientedSendingFlow.get(i, j) > 0) {
					U[j].set(i);
					J.set(j);
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
		
		
		// 3. Determine node supply level of reduction (Equation (34))
		// and reduced oriented capacities (Equation (35))
		double[][] nodeLOR = new double[alphas.length][incomingLinks.length];
		for (int p = 0; p < alphas.length; p++)
			for (int i = 0; i < incomingLinks.length; i++) {
				
				double denominator = 0;
				for (int j = 0; j < outgoingLinks.length; j++) {
					if (allowedLinks[p][i].get(j))
						denominator += orientedCapacities.get(i, j);
				}
				
				nodeLOR[p][i] = nodeSupplyConstraints[p][i] / denominator;
			}
		
		double[][] reducedCapacities = new double[incomingLinks.length][outgoingLinks.length];
		for (int p = 0; p < alphas.length; p++)
			for (int i = 0; i < incomingLinks.length; i++)
				for (int j = 0; j < outgoingLinks.length; j++)
					if (allowedLinks[p][i].get(j))
						reducedCapacities[i][j] = nodeLOR[p][i] * orientedCapacities.get(i, j);
		
		
		double[] LOR = new double[outgoingLinks.length]; // level of reduction
		while (!J.isClear()) {
			
			// 4. Determine most restrictive constraint
			double minLOR = Double.POSITIVE_INFINITY;
			int minJ = -1;
			
			for (int j = 0; j < J.size; j++) {
				if (J.get(j)) {
					double denominator = 0;
					
					for (int i = 0; i < U[j].size; i++) {
						if (U[j].get(i))
							denominator += reducedCapacities[i][j];
					}
					LOR[j] = R[j] / denominator;
					
					if (LOR[j] <= minLOR) {
						minLOR = LOR[j];
						minJ = j;
					}
				}
			}
			
			// 5. Determine flows of corresponding set U[minJ] and
			// recalculate Rj
			
			// (a) at least one i in U[minJ] is sending flow (demand)
			// constrained
			boolean anyDemandConstrained = false;
			for (int i = 0; i < U[minJ].size; i++)
				if (U[minJ].get(i)) {
					
					// retrieve the phase during which turning
					// movement i,minJ is allowed
					int p = -1;
					for (int p1 = 0; p1 < alphas.length; p1++)
						if (allowedLinks[p1][i].get(minJ)) {
							p = p1;
							break;
						}
					// TODO precompute ^^
					
					if (incomingLinks[i].getSendingFlow() <= minLOR * incomingLinks[i].capacity
							&& incomingLinks[i].getSendingFlow() <= nodeSupplyConstraints[p][i]) {
						anyDemandConstrained = true;
						
						for (int j = 0; j < outgoingLinks.length; j++)
							orientedFlows.set(i, j, orientedSendingFlow.get(i, j));
						
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
				}
			if (anyDemandConstrained)
				continue;
			
			// (b)
			boolean anyNodeConstrained = false;
			for (int i = 0; i < U[minJ].size; i++)
				if (U[minJ].get(i)) {
					
					// retrieve the phase during which turning
					// movement i,minJ is allowed
					int p = -1;
					for (int p1 = 0; p1 < alphas.length; p1++)
						if (allowedLinks[p1][i].get(minJ)) {
							p = p1;
							break;
						}
					
					if (nodeSupplyConstraints[p][i] <= minLOR * incomingLinks[i].capacity) {
						anyNodeConstrained = true;
						
						for (int j = 0; j < outgoingLinks.length; j++)
							orientedFlows.set(i, j, nodeLOR[p][i] * orientedCapacities.get(i, j));
						
						for (int j = 0; j < J.size; j++)
							if (J.get(j)) {
								R[j] -= nodeLOR[p][i] * orientedCapacities.get(i, j);
								U[j].clear(i);
								
								if (U[j].isClear()) {
									LOR[j] = 1;
									J.clear(j);
								}
							}
					}
				}
			if (anyNodeConstrained)
				continue;
			
			// (c) all links of U[minJ] are constrained by receiving
			// flow (supply) of link minJ
			for (int i = 0; i < U[minJ].size; i++)
				if (U[minJ].get(i)) {
					
					for (int j = 0; j < outgoingLinks.length; j++)
						orientedFlows.set(i, j, minLOR * reducedCapacities[i][j]);
					
					for (int j = 0; j < J.size; j++)
						if (J.get(j)) {
							
							R[j] -= minLOR * reducedCapacities[i][j];
							
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
		
		return orientedFlows;
	}
	
	public static TampereSignalized unsignalized(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		double[] alphas = new double[]{1};
		
		double[][] betas = new double[1][incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			betas[0][i] = 1;
		
		BitSet32[][] allowedLinks = new BitSet32[1][incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			
			allowedLinks[0][i] = new BitSet32(outgoingLinks.length);
			for (int j = 0; j < outgoingLinks.length; j++)
				allowedLinks[0][i].set(j);
		}
		
		return new TampereSignalized(index, incomingLinks,
				outgoingLinks, alphas, betas, allowedLinks);
	}
	
	public static TampereSignalized halfPermeable(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		double[] alphas = new double[]{0.5, 0.5};
		
		double[][] betas = new double[2][incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			betas[0][i] = 1;
		
		BitSet32[][] allowedLinks = new BitSet32[2][incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			
			allowedLinks[0][i] = new BitSet32(outgoingLinks.length);
			for (int j = 0; j < outgoingLinks.length; j++)
				allowedLinks[0][i].set(j);
			
			allowedLinks[1][i] = new BitSet32(outgoingLinks.length);
		}
		
		return new TampereSignalized(index, incomingLinks,
				outgoingLinks, alphas, betas, allowedLinks);
	}
}
