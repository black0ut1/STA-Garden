package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.RoutedIntersection;

/**
 * A generalization of {@link BasicINM} for arbitrary priority functions. It uses Euler's
 * method to solve the problem (14)-(15).
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public class GeneralINM extends RoutedIntersection {
	
	protected static final double H = 10;
	protected final PriorityFunction[] priorities;
	
	public GeneralINM(int index, Link[] incomingLinks, Link[] outgoingLinks, PriorityFunction[] priorities) {
		super(index, incomingLinks, outgoingLinks);
		this.priorities = priorities;
	}
	
	@Override
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions) {
		double[] inflows = new double[incomingLinks.length];
		double[] outflows = new double[outgoingLinks.length];
		
		BitSet32 D = determineUnconstrainedLinks(totalTurningFractions, inflows, outflows);
		
		while (!D.isClear()) {
			
			// psi_in is the derivative of inflows, psi_out is the derivative of outflows
			double[] psi_in = new double[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				psi_in[i] = (D.get(i) ? 1 : 0) * priorities[i].priority(inflows, outflows);
			
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
			D = determineUnconstrainedLinks(totalTurningFractions, inflows, outflows);
		}
		
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedFlows.set(i, j, inflows[i] * totalTurningFractions.get(i, j));
		
		return orientedFlows;
	}
	
	protected BitSet32 determineUnconstrainedLinks(DoubleMatrix totalTurningFractions, double[] inflows, double[] outflows) {
		BitSet32 D = new BitSet32(incomingLinks.length + outgoingLinks.length);
		
		for (int i = 0; i < incomingLinks.length; i++) {
			boolean sendingFlowConstrained = inflows[i] >= incomingLinks[i].getSendingFlow();
			
			boolean receivingFlowConstrained = false;
			for (int j = 0; j < outgoingLinks.length; j++)
				if (totalTurningFractions.get(i, j) > 0)
					if (outflows[j] >= outgoingLinks[j].getReceivingFlow()) {
						receivingFlowConstrained = true;
						break;
					}
			
			if (!sendingFlowConstrained && !receivingFlowConstrained)
				D.set(i);
		}
		
		for (int j = 0; j < outgoingLinks.length; j++) {
			boolean receivingFlowConstrained = outflows[j] >= outgoingLinks[j].getReceivingFlow();
			
			boolean sendingFlowConstrained = true;
			for (int i = 0; i < incomingLinks.length; i++)
				if (D.get(i))
					if (totalTurningFractions.get(i, j) > 0) {
						sendingFlowConstrained = false;
						break;
					}
			
			if (!receivingFlowConstrained && !sendingFlowConstrained)
				D.set(incomingLinks.length + j);
		}
		
		return D;
	}
}
