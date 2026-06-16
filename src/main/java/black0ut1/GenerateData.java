package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.io.CSV;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;

import java.io.File;
import java.util.Objects;

public class GenerateData {
	
	public static void main(String[] args) {
		if (args.length == 0 || !args[0].equals("SiouxFalls") && !args[0].equals("ChicagoSketch"))
			throw new IllegalArgumentException("First argument must be either 'SiouxFalls' or 'ChicagoSketch'.");
		
		String map = args[0];
		
		double timeStep = 1;
		int odmSteps = 1;
		int totalSteps = (map.equals("SiouxFalls")) ? 50 : 200;
		double relativeGap = (map.equals("SiouxFalls")) ? 1e-10 : 1e-6;
		
		System.out.println("==========================");
		System.out.println("Network: " + map);
		System.out.println("==========================");
		
		
		String networkFile = "./data/" + map + "/link.csv";
		String canonODM = "./data/" + map + "/demand.csv";
		String odms = "./data/" + map + "/odms/";
		
		DoubleMatrix odm = new CSV().parseODMatrix(canonODM);
		Network network = new CSV().parseNetwork(networkFile, null, odm.n);
		
		String[] files = Objects.requireNonNull(new File(odms).list());
		for (String odmFile : files) {
			
			String n = odmFile.split("_")[0];
			System.out.print(n + "/" + files.length);
			long tick = System.currentTimeMillis();
			
			////////////////////////////
			odm = new CSV().parseODMatrix(odms + odmFile);
			Bush[] bushes = destinationBushes(network, odm, relativeGap);
			
			TimeDependentODM tdodm = TimeDependentODM.fromStaticODM(odm, odmSteps);
			DynamicNetwork dNetwork = DynamicNetwork.fromStaticNetwork(network, tdodm, timeStep, totalSteps);
			StaticRouteChoice routeChoice = new StaticRouteChoice(dNetwork, totalSteps, bushes);
			
			var mfs = routeChoice.computeTurningFractions();
			DynamicNetworkLoading DNL = new ILTM_DNL(dNetwork, tdodm, timeStep, totalSteps, 1e-8);
			DNL.setTurningFractions(mfs);
			
			int finalAmountOfSteps = DNL.loadNetwork();
			////////////////////////////
			
			long tock = System.currentTimeMillis();
			System.out.println("(" + (tock - tick) + "ms, " + finalAmountOfSteps + " steps)");
			
			new CSV().writeFlows("./data/" + map + "/flows/" + n + "_flows.txt", dNetwork);
			new CSV().writeTurningFractions("./data/" + map + "/fractions/" + n + "_fractions.txt", dNetwork);
		}
	}
	
	private static Bush[] destinationBushes(Network network, DoubleMatrix odm, double relativeGap) {
		Settings settings = new Settings(network, odm, 100, new Convergence.Builder()
				.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, relativeGap));
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
