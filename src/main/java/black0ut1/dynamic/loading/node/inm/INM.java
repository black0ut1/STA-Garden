package black0ut1.dynamic.loading.node.inm;

import black0ut1.data.BitSet32;
import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.RoutedIntersection;

/**
 * A base class for different algorithms solving the flows using Incremental Node Model
 * (INM). This class essentially represents a solver for the problem (25) in
 * (Flotterod and Rohde, 2011).
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public abstract class INM extends RoutedIntersection {
	
	public INM(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	/** The INM without node supply constraints. Uses default sending and receiving flows. */
	protected Pair<double[], double[]> computeInflowsOutflows(DoubleMatrix totalTurningFractions) {
		double[] sendingFlows = new double[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			sendingFlows[i] = incomingLinks[i].getSendingFlow();
		
		double[] receivingFlows = new double[outgoingLinks.length];
		for (int i = 0; i < outgoingLinks.length; i++)
			receivingFlows[i] = outgoingLinks[i].getReceivingFlow();
		
		return computeInflowsOutflows(totalTurningFractions, sendingFlows, receivingFlows);
	}
	
	/**
	 * This method represents the INM parametrized with sending flows and receiving flows
	 * (equation (25) in (Flotterod and Rohde, 2011)).
	 */
	abstract Pair<double[], double[]> computeInflowsOutflows(DoubleMatrix totalTurningFractions,
															 double[] sendingFlows, double[] receivingFlows);
	
	/** Creates the set D(q) as defined in (18). */
	protected BitSet32 determineUnconstrainedLinks(DoubleMatrix totalTurningFractions,
												   double[] inflows, double[] outflows,
												   double[] sendingFlows, double[] receivingFlows) {
		BitSet32 D = new BitSet32(incomingLinks.length + outgoingLinks.length);
		
		for (int i = 0; i < incomingLinks.length; i++) {
			boolean sendingFlowConstrained = inflows[i] >= sendingFlows[i];
			
			boolean receivingFlowConstrained = false;
			for (int j = 0; j < outgoingLinks.length; j++)
				if (totalTurningFractions.get(i, j) > 0)
					if (outflows[j] >= receivingFlows[j]) {
						receivingFlowConstrained = true;
						break;
					}
			
			if (!sendingFlowConstrained && !receivingFlowConstrained)
				D.set(i);
		}
		
		for (int j = 0; j < outgoingLinks.length; j++) {
			boolean receivingFlowConstrained = outflows[j] >= receivingFlows[j];
			
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
