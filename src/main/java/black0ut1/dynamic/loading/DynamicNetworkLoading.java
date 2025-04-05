package black0ut1.dynamic.loading;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.Node;

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
	
	public void loadNetwork() {
		for (int t = 0; t < steps; t++) {
			System.out.println("========= Time " + t + " =========");
			
			// execute link models
			for (Link link : network.originConnectors)
				link.computeReceivingAndSendingFlows(t);
			for (Link link : network.destinationConnectors)
				link.computeReceivingAndSendingFlows(t);
			for (Link link : network.links)
				link.computeReceivingAndSendingFlows(t);
			
			// execute node models
			for (Node node : network.origins)
				node.shiftOrientedMixtureFlows(t);
			for (Node node : network.destinations)
				node.shiftOrientedMixtureFlows(t);
			for (Node node : network.intersections)
				node.shiftOrientedMixtureFlows(t);
			
			if (!network.changed(t))
				break;
		}
	}
	
	public void setTurningFractions(MixtureFractions[][] turningFractions) {
		if (turningFractions.length != network.intersections.length)
			throw new RuntimeException("Number of turning fraction arrays must be equal to number of intersections");
		
		for (int i = 0; i < network.intersections.length; i++) {
			if (turningFractions[i].length != steps)
				throw new RuntimeException("Number of turning fractions must be equal to number of steps (node " + i + ")");
			
			network.intersections[i].setTurningFractions(turningFractions[i]);
		}
	}
	
	public void resetNetwork() {
		for (Link link : network.links)
			link.reset();
		for (Link link : network.originConnectors)
			link.reset();
		for (Link link : network.destinationConnectors)
			link.reset();
		
		for (Destination destination : network.destinations)
			destination.reset();
	}
	
	public void checkDestinationInflows() {
	
	}
}
