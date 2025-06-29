package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.bush.B;
import black0ut1.static_.cost.BPR;
import black0ut1.util.NetworkUtils;

import java.util.Vector;


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
		
		var destinationBushes = destinationBushes(pair.first(), pair.second());
		
		// The route choice model
		StaticRouteChoice routeChoice = new StaticRouteChoice(network, destinationBushes);
		long startTime = System.currentTimeMillis();
		var mfs = routeChoice.computeTurningFractions(totalSteps);
		long endTime = System.currentTimeMillis();
		System.out.println("AON took " + (endTime - startTime) + "ms");
		
		// Dynamic network loading
		DynamicNetworkLoading DNL = new ILTM_DNL(network, odm, timeStep, totalSteps, 1e-8);
		DNL.setTurningFractions(mfs);
		
		startTime = System.currentTimeMillis();
		int finalAmountOfSteps = DNL.loadNetwork();
		endTime = System.currentTimeMillis();
		System.out.println("DNL took " + (endTime - startTime) + "ms");
		
		DNL.checkDestinationInflows(finalAmountOfSteps, false);
	}
	
	private static Bush[] destinationBushes(Network network, DoubleMatrix odm) {
		B alg = new B(network, odm, new BPR(), 20, new Convergence.Builder()
						.addCriterion(Convergence.Criterion.RELATIVE_GAP_1));
		alg.assignFlows();
		NetworkUtils.checkBushFlows(network, odm, alg.getBushes(), alg.getFlows());
		
		Bush[] destinationBushes = new Bush[network.zones];
		for (int dest = 0; dest < network.zones; dest++)
			destinationBushes[dest] = new Bush(network.edges, dest);
		
		Vector<Path> paths = NetworkUtils.calculatePathsFromBushes(network, odm, alg.getBushes());
		for (Path path : paths) {
			int lastEdgeIndex = path.edges[path.edges.length - 1];
			int destination = network.getEdges()[lastEdgeIndex].head;
			
			for (int index : path.edges) {
				destinationBushes[destination].addEdge(index);
				destinationBushes[destination].addFlow(index, path.flow);
			}
		}
		
		return destinationBushes;
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