package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.RoutedIntersection;

/**
 * The basic Incremental Node Model as described in (Flotterod and Rohde, 2011),
 * Algorithm 1. Here, basic means that priorities of incoming links are constants
 * independent of the flows.
 * Bibliography:
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public class BasicINM extends RoutedIntersection {
	
	protected final double[] priorities;
	
	public BasicINM(int index, Link[] incomingLinks, Link[] outgoingLinks, double[] priorities) {
		super(index, incomingLinks, outgoingLinks);
		this.priorities = priorities;
	}
	
	/** This constructor uses link capacities as priorities making the instance equivalent
	 * to {@link black0ut1.dynamic.loading.node.TampereUnsignalized}. */
	public BasicINM(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		this(index, incomingLinks, outgoingLinks, new double[incomingLinks.length]);
		for (int i = 0; i < incomingLinks.length; i++)
			this.priorities[i] = incomingLinks[i].capacity;
	}
	
	@Override
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions) {
		// 2. Compute initial flows according to (15)
		double[] inflows = new double[incomingLinks.length];
		double[] outflows = new double[outgoingLinks.length];
		
		// 3. Compute initial set D according to (18)
		BitSet32 D = determineUnconstrainedLinks(totalTurningFractions, inflows, outflows);
		
		// 4.
		while (!D.isClear()) {
			
			// (a)
			double[] psi_in = new double[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				psi_in[i] = (D.get(i) ? 1 : 0) * priorities[i];
			
			double[] psi_out = new double[outgoingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				for (int j = 0; j < outgoingLinks.length; j++)
					psi_out[j] += totalTurningFractions.get(i, j) * psi_in[i];
			
			// (b)
			double theta = Double.POSITIVE_INFINITY;
			for (int i = 0; i < incomingLinks.length; i++)
				if (D.get(i)) {
					double factor = (incomingLinks[i].getSendingFlow() - inflows[i]) / psi_in[i];
					theta = Math.min(theta, factor);
				}
			for (int j = 0; j < outgoingLinks.length; j++)
				if (D.get(incomingLinks.length + j)) {
					double factor = (outgoingLinks[j].getReceivingFlow() - outflows[j]) / psi_out[j];
					theta = Math.min(theta, factor);
				}
			
			// (c)
			for (int i = 0; i < incomingLinks.length; i++)
				inflows[i] += theta * psi_in[i];
			for (int j = 0; j < outgoingLinks.length; j++)
				outflows[j] += theta * psi_out[j];
			
			// (d)
			D = determineUnconstrainedLinks(totalTurningFractions, inflows, outflows);
		}
		
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedFlows.set(i, j, inflows[i] * totalTurningFractions.get(i, j));
		
		return orientedFlows;
	}
	
	/** Creates the set D(q) as defined in (18). */
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
