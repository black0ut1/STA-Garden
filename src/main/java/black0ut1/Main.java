package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.DestinationAON;
import black0ut1.dynamic.loading.Clock;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Node;
import black0ut1.io.TNTP;


public class Main {
	
	public static void main(String[] argv) {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = loadData(networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double smallestFreeFlow = Double.POSITIVE_INFINITY;
		for (Network.Edge edge : pair.first().getEdges())
			smallestFreeFlow = Math.min(smallestFreeFlow, edge.freeFlow);
		System.out.println("Smallest free flow time: " + smallestFreeFlow);
		
		
		Clock clock = new Clock(smallestFreeFlow, 10);
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), clock.steps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), clock, odm);
		
		DestinationAON aon = new DestinationAON(pair.first(), network, pair.second());
		var mfs = aon.computeTurningFractions(clock.steps);
		
		for (int i = 0; i < network.intersections.length; i++)
			network.intersections[i].setTurningFractions(mfs[i]);
		
		while (clock.ticking()) {
			System.out.println(clock.getCurrentStep());
			
			// execute link models
			for (Link link : network.originConnectors)
				link.computeReceivingAndSendingFlows();
			for (Link link : network.destinationConnectors)
				link.computeReceivingAndSendingFlows();
			for (Link link : network.links)
				link.computeReceivingAndSendingFlows();
			
			// execute node models
			for (Node node : network.origins)
				node.shiftOrientedMixtureFlows(clock.getCurrentStep());
			for (Node node : network.destinations)
				node.shiftOrientedMixtureFlows(clock.getCurrentStep());
			for (Node node : network.intersections)
				node.shiftOrientedMixtureFlows(clock.getCurrentStep());
			
			clock.nextStep();
		}
		// TODO abstract class for Intersection
		// TODO implement connector link
	}
	
	private static Pair<Network, DoubleMatrix> loadData(String networkFile, String odmFile, String nodeFile) {
		System.out.print("Loading network... ");
		long startTime = System.currentTimeMillis();
		Network network = TNTP.parseNetwork(networkFile, nodeFile);
		long endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		System.out.print("Loading OD matrix... ");
		startTime = System.currentTimeMillis();
		DoubleMatrix odMatrix = TNTP.parseODMatrix(odmFile);
		endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		return new Pair<>(network, odMatrix);
	}
}