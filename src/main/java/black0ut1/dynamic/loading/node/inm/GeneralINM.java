package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;

/**
 * A generalization of the {@link BasicINM} solver for arbitrary priority functions. It
 * uses Euler's method to solve the problem (14)-(16) in (Flotterod and Rohde, 2011).
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public class GeneralINM extends INM {
	
	protected static final double H = 10;
	protected final PriorityFunction[] priorities;
	
	public GeneralINM(int index, Link[] incomingLinks, Link[] outgoingLinks, PriorityFunction[] priorities) {
		super(index, incomingLinks, outgoingLinks);
		this.priorities = priorities;
	}
	
	@Override
	Pair<double[], double[]> computeInflowsOutflows(DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		double[] inflows = new double[incomingLinks.length];
		double[] outflows = new double[outgoingLinks.length];
		
		BitSet32 D = determineUnconstrainedLinks(
				totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		
		while (!D.isClear()) {
			
			// psi_in is the derivative of inflows, psi_out is the derivative of outflows
			double[] psi_in = new double[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				if (D.get(i)) // if i not in D => psi_in[i] = 0
					psi_in[i] = priorities[i].priority(inflows, outflows);
			
			double[] psi_out = new double[outgoingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				for (int j = 0; j < outgoingLinks.length; j++)
					psi_out[j] += totalTurningFractions.get(i, j) * psi_in[i];
			
			// update inflows and outflows using Euler's method
			for (int i = 0; i < incomingLinks.length; i++)
				inflows[i] += H * psi_in[i];
			for (int j = 0; j < outgoingLinks.length; j++)
				outflows[j] += H * psi_out[j];
			
			// TODO optimize to check only active links
			D = determineUnconstrainedLinks(
					totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		}
		
		return new Pair<>(inflows, outflows);
	}
}
