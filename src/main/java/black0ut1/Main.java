package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.STARouteChoice;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.*;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;


public class Main {
	
	public static void main(String[] argv) {
		int maxSteps = 6_000;
		
		String map = argv[0];
		double stepSize = Double.parseDouble(argv[1]);
		String dnlScheme = argv[2];
		
		System.out.println("==========================");
		System.out.println("Network: " + map);
		System.out.println("Step size: " + stepSize);
		System.out.println("DNL Scheme: " + dnlScheme);
		System.out.println("==========================");
		
		
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
		
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), 10);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, stepSize, maxSteps);
		
		
		DynamicNetworkLoading dnl = switch (dnlScheme) {
			case "Basic" -> new BasicDNL(network, odm, stepSize, maxSteps);
			case "ILTM" -> new ILTM_DNL(network, odm, stepSize, maxSteps, 1e-8);
			case "FS-ILTM" -> new FS_ILTM_DNL(network, odm, stepSize, maxSteps, 1e-8);
			case "PQ-ILTM" -> new PQFS_ILTM_DNL(network, odm, stepSize, maxSteps, 1e-8);
			default -> throw new RuntimeException("Unsupported DNL Scheme: " + dnlScheme);
		};
		
		var mfs = destinationBushes(pair.first(), pair.second(), network);
		dnl.setTurningFractions(mfs);
		
		long tick = System.currentTimeMillis();
		int t = dnl.loadNetwork();
		long tock = System.currentTimeMillis();
		System.out.println((tock - tick) + "ms");
		
		double totalFlow = dnl.getTotalFlowOnNetwork(t);
		System.out.println("Total flow on network: " + totalFlow);
		System.out.println("Steps taken: " + t);
		System.out.println("Node updates: " + dnl.getNodeUpdates());
		dnl.checkDestinationInflows(false, t);
		
		System.out.println('\n');
	}
	
	
	private static MixtureFractions[][] destinationBushes(Network network, DoubleMatrix odm, DynamicNetwork dNetwork) {
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
		
		StaticRouteChoice routeChoice = new STARouteChoice(dNetwork, 6_000, destinationBushes);
		return routeChoice.computeInitialMixtureFractions();
	}
}