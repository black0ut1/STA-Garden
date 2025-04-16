package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
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
public class DaganzoDiverge extends Intersection {
	
	public DaganzoDiverge(int index, Link incomingLink, Link[] outgoingLinks) {
		super(index, new Link[]{incomingLink}, outgoingLinks);
	}
	
	@Override
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions) {
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		
		// the single incoming link
		Link incomingLink = incomingLinks[0];
		// its sending flow
		double S = incomingLink.getReceivingFlow();
		
		// 1. Compute the portion of sending flow actually sent (theta)
		double theta = 1;
		for (int j = 0; j < outgoingLinks.length; j++) {
			
			double R = outgoingLinks[j].getReceivingFlow();
			theta = Math.min(theta, R / (S * totalTurningFractions.get(0, j)));
		}
		
		// 2. Compute outgoing flows
		for (int j = 0; j < outgoingLinks.length; j++) {
			double outgoingFlow = theta * S * totalTurningFractions.get(0, j);
			orientedFlows.set(0, j, outgoingFlow);
		}
		
		return orientedFlows;
	}
}
