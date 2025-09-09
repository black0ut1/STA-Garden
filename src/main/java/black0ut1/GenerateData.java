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
	
	public static final String odms = "./data/training/odms/";
	public static final String odmFile = "./data/17_Sioux_Falls/demand.csv";
	public static final String networkFile = "./data/17_Sioux_Falls/link.csv";
	
	public static final double timeStep = 1;
	public static final int odmSteps = 1;
	public static final int totalSteps = 50;
	
	public static void main(String[] args) {
		DoubleMatrix odm = new CSV().parseODMatrix(odmFile);
		Network network = new CSV().parseNetwork(networkFile, null, odm.n);
		
		for (String odmFile : Objects.requireNonNull(new File(odms).list())) {
			odm = new CSV().parseODMatrix(odms + odmFile);
			Bush[] bushes = destinationBushes(network, odm);
			
			TimeDependentODM tdodm = TimeDependentODM.fromStaticODM(odm, odmSteps);
			DynamicNetwork dNetwork = DynamicNetwork.fromStaticNetwork(network, tdodm, timeStep, totalSteps);
			StaticRouteChoice routeChoice = new StaticRouteChoice(dNetwork, totalSteps, bushes);
			
			var mfs = routeChoice.computeTurningFractions();
			DynamicNetworkLoading DNL = new ILTM_DNL(dNetwork, tdodm, timeStep, totalSteps, 1e-8);
			DNL.setTurningFractions(mfs);
			
			int finalAmountOfSteps = DNL.loadNetwork();
			
			DNL.checkDestinationInflows(finalAmountOfSteps, false);
			
			String n = odmFile.split("_")[0];
			System.out.println("================== " + n + " ==================");
			new CSV().writeCumulativeFlows("./data/training/cumulative/" + n + "_cumulative.csv", dNetwork, finalAmountOfSteps);
			new CSV().writeTurningFractions("./data/training/turning/" + n + "_turning.csv", dNetwork);
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
