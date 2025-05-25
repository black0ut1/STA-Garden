package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.DestinationAON;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.io.TNTP;


public class Main {
	
	public static void main(String[] argv) {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = loadData(networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double timeStep = 0.4;
		int odmSteps = 10;
		int totalSteps = 2000;
		
		// The ODM will generate flow for only first 10 time steps
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, totalSteps);
		
		double smallestFreeFlowTime = Double.POSITIVE_INFINITY;
		for (Link link : network.links)
			smallestFreeFlowTime = Math.min(smallestFreeFlowTime, link.length / link.freeFlowSpeed);
		System.out.println("Smallest free flow time: " + smallestFreeFlowTime);
		
		// The route choice model
		DestinationAON aon = new DestinationAON(pair.first(), network, pair.second());
		long startTime = System.currentTimeMillis();
		var mfs = aon.computeTurningFractions(totalSteps);
		long endTime = System.currentTimeMillis();
		System.out.println("AON took " + (endTime - startTime) + "ms");
		
		// Dynamic network loading
		DynamicNetworkLoading DNL = new ILTM_DNL(network, odm, timeStep, totalSteps, 1e-8);
		DNL.setTurningFractions(mfs);
		
		startTime = System.currentTimeMillis();
		int finalAmountOfSteps = DNL.loadNetwork();
		endTime = System.currentTimeMillis();
		System.out.println("DNL took " + (endTime - startTime) + "ms");
		
		DNL.checkDestinationInflows(finalAmountOfSteps);
	}
	
	public static Pair<Network, DoubleMatrix> loadData(String networkFile, String odmFile, String nodeFile) {
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