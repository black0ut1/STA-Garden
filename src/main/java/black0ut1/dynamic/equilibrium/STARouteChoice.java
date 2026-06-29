package black0ut1.dynamic.equilibrium;

import black0ut1.data.network.Bush;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.dynamic.loading.node.routing.RoutedIntersection;

import java.util.Arrays;

public class STARouteChoice implements StaticRouteChoice {
	
	protected final DynamicNetwork network;
	protected final int maxSteps;
	protected final Bush[] destinationBushes;
	
	public STARouteChoice(DynamicNetwork network, int maxSteps, Bush[] destinationBushes) {
		this.network = network;
		this.maxSteps = maxSteps;
		this.destinationBushes = destinationBushes;
	}
	
	public MixtureOutgoingFractions[][] computeInitialMixtureFractions() {
		MixtureOutgoingFractions[][] result = new MixtureOutgoingFractions[network.routedIntersections.length][maxSteps];
		
		for (RoutedIntersection intersection : network.routedIntersections)
			result[intersection.index][0] = createMixtureFractionsForIntersection(intersection);
		
		// copy the first mixture fraction to each time step
		for (int i = 0; i < network.routedIntersections.length; i++)
			Arrays.fill(result[i], result[i][0]);
		
		return result;
	}
	
	protected MixtureOutgoingFractions createMixtureFractionsForIntersection(RoutedIntersection intersection) {
		// Creates turning fractions for each destination
		
		MixtureOutgoingFractions fractions = new MixtureOutgoingFractions(network, intersection.index);
		
		for (int destination = 0; destination < destinationBushes.length; destination++) {
			
			// 1a) Intersection is the destination
			if (intersection.index == destination) {
				
				// traffic from all incoming links will leave using the connector (which
				// is the first outgoing link)
				fractions.setFraction(destination, 0, 1);
				
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
				
				// Destination flow do not use this intersection -> fractions will be
				// uniformly distributed
				if (outgoingFlow == 0) {
					double fraction = 1.0 / intersection.outgoingLinks.length;
					for (int j = 0; j < intersection.outgoingLinks.length; j++)
						fractions.setFraction(destination, j, fraction);
				}
				else {
					for (int j = 0; j < intersection.outgoingLinks.length; j++) {
						
						int outgoingLinkIndex = intersection.outgoingLinks[j].index;
						double fraction = (outgoingLinkIndex == -1)
								? 0 // outgoing link is connector to some other destination
								: bush.getEdgeFlow(outgoingLinkIndex) / outgoingFlow;
						
						fractions.setFraction(destination, j, fraction);
					}
				}
			}
		}
		
		return fractions;
	}
}
