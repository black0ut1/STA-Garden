package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;

/**
 * Basic node model representing one link diverging into multiple:
 *     /-->
 * -->O -->
 *     \-->
 * Bibliography:
 * - (Daganzo, 1995a)
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.2.3
 */
public class DaganzoDiverge extends RoutedIntersection {
	
	public DaganzoDiverge(int index, Link incomingLink, Link[] outgoingLinks) {
		super(index, new Link[]{incomingLink}, outgoingLinks);
	}
	
	@Override
	protected Pair<double[], double[]> computeInflowsOutflows(DoubleMatrix totalTurningFractions) {
		double[] inflows = new double[1];
		double[] outflows = new double[outgoingLinks.length];
		
		// the single incoming link
		Link incomingLink = incomingLinks[0];
		// its sending flow
		double S = incomingLink.getSendingFlow();
		
		// 1. Compute the portion of sending flow actually sent (theta)
		double theta = 1;
		for (int j = 0; j < outgoingLinks.length; j++) {
			
			double R = outgoingLinks[j].getReceivingFlow();
			theta = Math.min(theta, R / (S * totalTurningFractions.get(0, j)));
		}
		
		// 2. Compute outgoing flows
		for (int j = 0; j < outgoingLinks.length; j++) {
			double outgoingFlow = theta * S * totalTurningFractions.get(0, j);
			outflows[j] = outgoingFlow;
			inflows[0] += outgoingFlow;
		}
		
		return new Pair<>(inflows, outflows);
	}
}
