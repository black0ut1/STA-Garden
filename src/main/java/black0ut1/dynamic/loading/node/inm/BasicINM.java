package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
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
	
	public BasicINM(int index, Link[] incomingLinks, Link[] outgoingLinks, double[] priorities) {
		super(index, incomingLinks, outgoingLinks);
		this.priorities = priorities;
	}
	
	/**
	 * This constructor uses link capacities as priorities making the instance equivalent
	 * to {@link black0ut1.dynamic.loading.node.TampereUnsignalized}.
	 */
	public BasicINM(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		this(index, incomingLinks, outgoingLinks, new double[incomingLinks.length]);
		for (int i = 0; i < incomingLinks.length; i++)
			this.priorities[i] = incomingLinks[i].capacity;
	}
	
	@Override
	DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows) {
		// 2. Compute initial flows according to (15)
		double[] inflows = new double[incomingLinks.length];
		double[] outflows = new double[outgoingLinks.length];
		
		// 3. Compute initial set D according to (18)
		BitSet32 D = determineUnconstrainedLinks(
				totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		
		// 4. While D != {}
		while (!D.isClear()) {
			
			// (a) Compute psi(q) according to (19)
			double[] psi_in = new double[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				psi_in[i] = (D.get(i) ? 1 : 0) * priorities[i];
			
			double[] psi_out = new double[outgoingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				for (int j = 0; j < outgoingLinks.length; j++)
					psi_out[j] += totalTurningFractions.get(i, j) * psi_in[i];
			
			// (b) Compute theta according to (24)
			double theta = Double.POSITIVE_INFINITY;
			for (int i = 0; i < incomingLinks.length; i++)
				if (D.get(i)) {
					double factor = (sendingFlows[i] - inflows[i]) / psi_in[i];
					theta = Math.min(theta, factor);
				}
			for (int j = 0; j < outgoingLinks.length; j++)
				if (D.get(incomingLinks.length + j)) {
					double factor = (receivingFlows[j] - outflows[j]) / psi_out[j];
					theta = Math.min(theta, factor);
				}
			
			// (c) q = q + theta * psi(q) according to (23)
			for (int i = 0; i < incomingLinks.length; i++)
				inflows[i] += theta * psi_in[i];
			for (int j = 0; j < outgoingLinks.length; j++)
				outflows[j] += theta * psi_out[j];
			
			// (d) D = D(q) according to (18)
			D = determineUnconstrainedLinks(
					totalTurningFractions, inflows, outflows, sendingFlows, receivingFlows);
		}
		
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++)
				orientedFlows.set(i, j, inflows[i] * totalTurningFractions.get(i, j));
		
		return orientedFlows;
	}
}
