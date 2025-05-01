package black0ut1.dynamic.loading;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.Node;
import black0ut1.dynamic.loading.node.Origin;

/**
 * Class that wraps up the whole functionality of dynamic network
 * loading. Use it to set turning fractions for each intersection,
 * load the network by executing the node and link models or check the
 * consistency of loaded flows.
 */
public class DynamicNetworkLoading {
	
	/** The network to be loaded. */
	protected final DynamicNetwork network;
	/** Time dependent origin-destination matrix. Its flows will be
	 *  loaded onto the network. */
	protected final TimeDependentODM odm;
	/** The period of one time step [h]. */
	protected final double stepSize;
	/** The number of time steps the DNL will take. */
	protected final int steps;
	
	public DynamicNetworkLoading(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
		this.network = network;
		this.odm = odm;
		this.stepSize = stepSize;
		this.steps = steps;
	}
	
	public int loadNetwork() {
		int t;
		for (t = 0; t < steps; t++) {
			System.out.println("========= Time " + t + " =========");
			
			// execute link models
			for (Link link : network.allLinks)
				link.computeReceivingAndSendingFlows(t);
			
			// execute node models and shift flows
			for (Node node : network.allNodes) {
				var pair = node.computeOrientedMixtureFlows(t);
				
				if (!(node instanceof Origin)) { // origins have null incomingLinks
					for (int i = 0; i < node.incomingLinks.length; i++) {
						Link incomingLink = node.incomingLinks[i];
						MixtureFlow incomingFlow = pair.first()[i];
						
						incomingLink.outflow[t] = incomingFlow;
						incomingLink.cumulativeOutflow[t + 1] = incomingLink.cumulativeOutflow[t] + incomingFlow.totalFlow;
					}
				}
				
				if (!(node instanceof Destination)) { // destinations have null outgoingLinks
					for (int j = 0; j < node.outgoingLinks.length; j++) {
						Link outgoingLink = node.outgoingLinks[j];
						MixtureFlow outgoingFlow = pair.second()[j];
						
						outgoingLink.inflow[t] = outgoingFlow;
						outgoingLink.cumulativeInflow[t + 1] = outgoingLink.cumulativeInflow[t] + outgoingFlow.totalFlow;
					}
				}
			}
			
			var pair = network.getTotalInflowOutflow(t);
			System.out.println("Inflow of all links: " + pair.first());
			System.out.println("Outflow of all links: " + pair.second());
			
			if (pair.first() == 0 && pair.second() == 0)
				break;
		}
		return t;
	}
	
	/**
	 * Sets mixture fractions for each intersection and for each time
	 * step.
	 * @param turningFractions 2D array of mixture flows, where first
	 * index represents intersection and the second represents the
	 * time step - {@code turningFractions[i][t]} are MixtureFractions used by
	 * intersection i during time step t.
	 */
	public void setTurningFractions(MixtureFractions[][] turningFractions) {
		for (int i = 0; i < network.intersections.length; i++)
			network.intersections[i].setTurningFractions(turningFractions[i]);
	}
	
	/**
	 * Resets the network to its original state making it ready to be
	 * loaded again.
	 */
	public void resetNetwork() {
		for (Link link : network.allLinks)
			link.reset();
		
		for (Destination destination : network.destinations)
			destination.reset();
	}
	
	/**
	 * Checks the consistency of flows that arrived to destinations.
	 * For this method to work properly, every flow must arrive to
	 * some destination. It checks if the total flow arrived to a
	 * destination is different from what ODM tells should arrive. It
	 * also checks if the MixtureFlow contains only the one
	 * destination.
	 * @param steps The total number of steps the DNL took to finish,
	 * as returned by {@code loadNetwork()}.
	 */
	public void checkDestinationInflows(int steps) {
		System.out.println("============ Checking arrived flows ============");
		double[] odmDestinationInflow = new double[network.destinations.length];
		
		for (int destination = 0; destination < odm.zones; destination++)
			for (int origin = 0; origin < odm.zones; origin++)
				for (int t = 0; t < odm.timeSteps; t++)
					odmDestinationInflow[destination] += odm.getFlow(origin, destination, t);
		
		double[] networkDestinationInflow = new double[network.destinations.length];
		for (int destination = 0; destination < network.destinations.length; destination++) {
			
			var destiantionInflow = network.destinations[destination].inflow;
			for (int t = 0; t < steps; t++) {
				
				for (int d = 0; d < destiantionInflow[t].destinations.length; d++) {
					int destination1 = destiantionInflow[t].destinations[d];
					
					if (destination1 != destination)
						System.out.println("Mixture flow arrived to destination " + destination +
								" contains a portion belonging to other destination " + destination1);
				}
				
				networkDestinationInflow[destination] += destiantionInflow[t].totalFlow;
			}
		}
		
		for (int i = 0; i < network.destinations.length; i++) {
			if (Math.abs(odmDestinationInflow[i] - networkDestinationInflow[i]) > 1e-5) {
				System.out.println("The total inflow into destination " + i + " is different from ODM values. " +
						"The total flow arrived is " + networkDestinationInflow[i] + ", but ODM says "
						+ odmDestinationInflow[i] + " should arrive.");
			}
		}
		
		double odmTotal = 0;
		for (double v : odmDestinationInflow)
			odmTotal += v;
		System.out.println("Sum of all ODM values: " + odmTotal);
		
		double networkTotal = 0;
		for (double v : networkDestinationInflow)
			networkTotal += v;
		System.out.println("Sum of all flows arrived at every destination: " + networkTotal);
		
		double avgDifference = 0;
		for (int i = 0; i < network.destinations.length; i++)
			avgDifference += Math.abs(odmDestinationInflow[i] - networkDestinationInflow[i]);
		System.out.println("Average difference: " + avgDifference / network.destinations.length);
	}
}
