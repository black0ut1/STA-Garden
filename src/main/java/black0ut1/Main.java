package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.DestinationAON;
import black0ut1.dynamic.loading.DynamicNetworkLoading;
import black0ut1.io.TNTP;


public class Main {
	
	public static void main(String[] argv) {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = loadData(networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double smallestFreeFlowTime = Double.POSITIVE_INFINITY;
		for (Network.Edge edge : pair.first().getEdges())
			if (edge.freeFlow > 0)
				smallestFreeFlowTime = Math.min(smallestFreeFlowTime, edge.freeFlow);
		System.out.println("Smallest free flow time: " + smallestFreeFlowTime);
		
		double timeStep = 0.12;
		int odmSteps = 10;
		int totalSteps = 2000;
		
		// The ODM will generate flow for only first 10 time steps
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, totalSteps);
		
		// The route choice model
		DestinationAON aon = new DestinationAON(pair.first(), network, pair.second());
		var mfs = aon.computeTurningFractions(totalSteps);
		
		
		DynamicNetworkLoading DNL = new DynamicNetworkLoading(network, odm, timeStep, totalSteps);
		DNL.setTurningFractions(mfs);
		
		long startTime = System.currentTimeMillis();
		int finalAmountOfSteps = DNL.loadNetwork();
		long endTime = System.currentTimeMillis();
		System.out.println("DNL took " + (endTime - startTime) + "ms");
		
		DNL.checkDestinationInflows(finalAmountOfSteps);
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