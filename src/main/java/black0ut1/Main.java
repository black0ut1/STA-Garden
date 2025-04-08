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
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = loadData(networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double smallestFreeFlowTime = Double.POSITIVE_INFINITY;
		for (Network.Edge edge : pair.first().getEdges())
			smallestFreeFlowTime = Math.min(smallestFreeFlowTime, Math.max(edge.freeFlow, 0.1));
		System.out.println("Smallest free flow time: " + smallestFreeFlowTime);
		
		// The ODM will generate flow for only first 10 time steps
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), 10);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, smallestFreeFlowTime);
		
		DestinationAON aon = new DestinationAON(pair.first(), network, pair.second());
		var mfs = aon.computeTurningFractions(10);
		
		DynamicNetworkLoading DNL = new DynamicNetworkLoading(network, odm, smallestFreeFlowTime, 10);
		DNL.setTurningFractions(mfs);
		DNL.loadNetwork();
		DNL.checkDestinationInflows();
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