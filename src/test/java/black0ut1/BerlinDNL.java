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
import black0ut1.dynamic.loading.routing.MixtureFractions;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class BerlinDNL {
	
	@Test
	void test() {
		String map = "BerlinCenter";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		double stepSize = 1;
		int maxSteps = 620;
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
		
		TimeDependentODM odm = TimeDependentODM.fromStaticUniform(pair.second(), 10);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, stepSize, maxSteps);
		
		DynamicNetworkLoading dnl = new ILTM_DNL(network, odm, stepSize, maxSteps, 1e-8);
		MixtureFractions[][] initialRouteChoice = destinationBushes(pair.first(), pair.second(), network);
		dnl.setTurningFractions(initialRouteChoice);
		
		long tick = System.currentTimeMillis();
		dnl.loadNetwork();
		long tock = System.currentTimeMillis();
		System.out.println((tock - tick) + "ms");
	}
	
	MixtureFractions[][] destinationBushes(Network network, DoubleMatrix odm, DynamicNetwork dNetwork) {
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
