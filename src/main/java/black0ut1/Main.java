package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.STARouteChoice;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;


public class Main {
	
	public static void main(String[] argv) {
		String[] maps = {"SiouxFalls", "ChicagoSketch", "BerlinCenter"};
		double[] stepSizes = {1, 0.5, 1};
		int[] timeSteps = {45, 320, 620};
		
		for (int i = 0; i < 3; i++) {
			System.out.println("============= " + maps[i] + " =============");
			
			for (int j = 0; j < 7; j++) {
				String networkFile = "data/" + maps[i] + "/" + maps[i] + "_net.tntp";
				String odmFile = "data/" + maps[i] + "/" + maps[i] + "_trips.tntp";
				String nodeFile = "data/" + maps[i] + "/" + maps[i] + "_node.tntp";
				
				var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
				
				TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), 10);
				DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, stepSizes[i], timeSteps[i]);
				
				var destinationBushes = destinationBushes(pair.first(), pair.second());
				StaticRouteChoice routeChoice = new STARouteChoice(network, timeSteps[i], destinationBushes);
				var mfs = routeChoice.computeInitialMixtureFractions();
				
				DynamicNetworkLoading dnl = new ILTM_DNL(network, odm, stepSizes[i], timeSteps[i], 1e-8);
				dnl.setTurningFractions(mfs);
				
				long tick = System.currentTimeMillis();
				dnl.loadNetwork();
				long tock = System.currentTimeMillis();
				System.out.println((tock - tick) + "ms");
				
				dnl.checkDestinationInflows(false);
			}
		}
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