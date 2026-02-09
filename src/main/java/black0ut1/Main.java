package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.MSA;
import black0ut1.dynamic.equilibrium.StaticAONRouteChoice;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.BasicDNL;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.tdsp.DestinationShortestPaths;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;


public class Main {
	
	public static void main(String[] argv) {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double stepSize = 2;
		int odmSteps = 10;
		int timeSteps = 150;
		
		// The ODM will generate flow for only first 10 time steps
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, stepSize, timeSteps);
		
		double smallestFreeFlowTime = Double.POSITIVE_INFINITY;
		for (Link link : network.links)
			smallestFreeFlowTime = Math.min(smallestFreeFlowTime, link.length / link.freeFlowSpeed);
		System.out.println("Smallest free flow time: " + smallestFreeFlowTime);
		
//		var destinationBushes = destinationBushes(pair.first(), pair.second());
		
		StaticRouteChoice routeChoice = new StaticAONRouteChoice(pair.first(), network, pair.second(), timeSteps);
		DynamicNetworkLoading dnl = new BasicDNL(network, odm, stepSize, timeSteps);
		DestinationShortestPaths tdsp = new DestinationShortestPaths(network, stepSize, timeSteps);
		
		MSA msa = new MSA(network, odm, routeChoice, dnl, tdsp, 100, stepSize);
		msa.run();
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