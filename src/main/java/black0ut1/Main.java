package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.STARouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;


public class Main {
	
	public static void main(String[] argv) {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
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
		STARouteChoice routeChoice = new STARouteChoice(network, totalSteps, destinationBushes);
		long startTime = System.currentTimeMillis();
		var mfs = routeChoice.computeTurningFractions();
		long endTime = System.currentTimeMillis();
		System.out.println("AON took " + (endTime - startTime) + "ms");
		
		// Dynamic network loading
		DynamicNetworkLoading DNL = new ILTM_DNL(network, odm, timeStep, totalSteps, 1e-8);
		DNL.setTurningFractions(mfs);
		
		startTime = System.currentTimeMillis();
		DNL.loadNetwork();
		endTime = System.currentTimeMillis();
		System.out.println("DNL took " + (endTime - startTime) + "ms");
		
		DNL.checkDestinationInflows(false);
	}
	
	private static Bush[] destinationBushes(Network network, DoubleMatrix odm) {
		Settings settings = new Settings(network, odm, 20, new Convergence.Builder()
				.addCriterion(Convergence.Criterion.RELATIVE_GAP_1));
		ProjectedGradient pg = new ProjectedGradient(settings);
		pg.assignFlows();
		NetworkUtils.checkPathFlows(network, odm, pg.getPaths(), pg.getFlows());
		
		
		Bush[] destinationBushes = new Bush[network.zones];
		for (int dest = 0; dest < network.zones; dest++)
			destinationBushes[dest] = new Bush(network.edges, dest);
		
		var paths = pg.getPaths();
		for (int origin = 0; origin < network.zones; origin++)
			for (int destination = 0; destination < network.zones; destination++) {
				var odPaths = paths.get(origin, destination);
				if (odPaths == null)
					continue;
				
				for (Path path : odPaths)
					for (int index : path.edges) {
						destinationBushes[destination].addEdge(index);
						destinationBushes[destination].addFlow(index, path.flow);
					}
			}
		
		return destinationBushes;
	}
}