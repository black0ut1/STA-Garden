package black0ut1.dynamic.equilibrium;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.node.Intersection;

import java.util.Arrays;

public class StaticRouteChoice {
	
	protected final DynamicNetwork network;
	protected final Bush[] destinationBushes;
	
	public StaticRouteChoice(DynamicNetwork network, Bush[] destinationBushes) {
		this.network = network;
		this.destinationBushes = destinationBushes;
	}
	
	public MixtureFractions[][] computeTurningFractions(int timeSteps) {
		MixtureFractions[][] result = new MixtureFractions[network.intersections.length][timeSteps];
		
		for (Intersection intersection : network.intersections)
			result[intersection.index][0] = createMixtureFractionsForIntersection(intersection);
		
		// copy the first mixture fraction to each time step
		for (int i = 0; i < network.intersections.length; i++)
			Arrays.fill(result[i], result[i][0]);
		
		return result;
	}
	
	protected MixtureFractions createMixtureFractionsForIntersection(Intersection intersection) {
		
		int len = 0;
		int[] destinations = new int[network.intersections.length];
		DoubleMatrix[] turningFractions = new DoubleMatrix[network.intersections.length];
		
		for (int destination = 0; destination < destinationBushes.length; destination++) {
			DoubleMatrix destinationTurningFractions = new DoubleMatrix(intersection.incomingLinks.length, intersection.outgoingLinks.length);
			
			if (intersection.index == destination) { // intersection is the destination
				
				// all traffic will leave to the virtual desination using the connector
				// (which is first outgoing link)
				for (int i = 0; i < intersection.incomingLinks.length; i++)
					destinationTurningFractions.set(i, 0, 1);
				
			} else {
				Bush bush = destinationBushes[destination];
				
				double outgoingFlow = 0;
				for (Link outgoingLink : intersection.outgoingLinks) {
					if (!bush.edgeExists(outgoingLink.index))
						continue;
					
					outgoingFlow += bush.getEdgeFlow(outgoingLink.index);
				}
				
				if (outgoingFlow == 0) // this node is not used by destination flow
					continue;
				
				if (intersection.index < network.origins.length) { // intersection is origin
					
					// compute flow originating in this origin
					double incomingFlow = 0;
					for (Link incomingLink : intersection.incomingLinks) {
						if (!bush.edgeExists(incomingLink.index))
							continue;
						
						incomingFlow += bush.getEdgeFlow(incomingLink.index);
					}
					
					double originFlow = outgoingFlow - incomingFlow;
					
					// all of this flow will enter from the virtual origin using the connector
					// (which is first incoming link)
					for (int j = 0; j < intersection.outgoingLinks.length; j++) {
						int outgoingLinkIndex = intersection.outgoingLinks[j].index;
						double fraction = originFlow * bush.getEdgeFlow(outgoingLinkIndex) / outgoingFlow;
						destinationTurningFractions.set(0, j, fraction);
					}
				}
				
				for (int i = 0; i < intersection.incomingLinks.length; i++) {
					if (intersection.index < network.origins.length && i == 0)
						continue; // if origin, skip the connector (which is handled above)
					
					int incomingLinkIndex = intersection.incomingLinks[i].index;
					if (!bush.edgeExists(incomingLinkIndex))
						continue;
					
					for (int j = 0; j < intersection.outgoingLinks.length; j++) {
						int outgoingLinkIndex = intersection.outgoingLinks[j].index;
						if (!bush.edgeExists(outgoingLinkIndex))
							continue;
						
						double fraction = bush.getEdgeFlow(incomingLinkIndex) * bush.getEdgeFlow(outgoingLinkIndex) / outgoingFlow;
						destinationTurningFractions.set(i, j, fraction);
					}
				}
			}
			
			turningFractions[len] = destinationTurningFractions;
			destinations[len] = destination;
			len++;
		}
		
		return new MixtureFractions(destinations, turningFractions, len);
	}
}
