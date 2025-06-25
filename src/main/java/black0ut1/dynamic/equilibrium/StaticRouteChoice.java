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
		// Creates turning fractions for each destination
		
		int len = 0;
		int[] destinations = new int[network.intersections.length];
		DoubleMatrix[] turningFractions = new DoubleMatrix[network.intersections.length];
		
		for (int destination = 0; destination < destinationBushes.length; destination++) {
			DoubleMatrix destinationTurningFractions = new DoubleMatrix(intersection.incomingLinks.length, intersection.outgoingLinks.length);
			
			// 1a) Intersection is the destination
			if (intersection.index == destination) {
				
				// traffic from all incoming links will leave using the connector (which
				// is the first outgoing link)
				for (int i = 0; i < intersection.incomingLinks.length; i++)
					destinationTurningFractions.set(i, 0, 1);
				
			} // 1b) Intersection is not the destination
			else {
				Bush bush = destinationBushes[destination];
				
				// All flow going through this intersection
				double outgoingFlow = 0;
				for (Link outgoingLink : intersection.outgoingLinks) {
					if (outgoingLink.index == -1 || !bush.edgeExists(outgoingLink.index))
						continue;
					
					outgoingFlow += bush.getEdgeFlow(outgoingLink.index);
				}
				
				// Destination flow do not use this intersection -> the destination will
				// be excluded from the mixture fractions
				if (outgoingFlow == 0)
					continue;
				
				for (int j = 0; j < intersection.outgoingLinks.length; j++) {
					
					int outgoingLinkIndex = intersection.outgoingLinks[j].index;
					double fraction = (outgoingLinkIndex == -1)
							? 0 // outgoing link is connector to some other destination
							: bush.getEdgeFlow(outgoingLinkIndex) / outgoingFlow;
					
					for (int i = 0; i < intersection.incomingLinks.length; i++)
						destinationTurningFractions.set(i, j, fraction);
				}
			}
			
			turningFractions[len] = destinationTurningFractions;
			destinations[len] = destination;
			len++;
		}
		
		return new MixtureFractions(destinations, turningFractions, len);
	}
}
