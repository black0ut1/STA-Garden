package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.node.routing.RoutedIntersection;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;

import java.util.Arrays;


public class Main {
	
	public static void main(String[] args) {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
//		new GUI(new AssignmentPanel(pair.first()));
		
		double stepSize = 0.5;
		int odmSteps = 30;
		int timeSteps = 350;
		
		// The ODM will generate flow for only first 10 time steps
		TimeDependentODM odm = TimeDependentODM.fromStaticUniform(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, stepSize, timeSteps);
		
		long sumOutlinks = 0;
		for (RoutedIntersection routedIntersection : network.routedIntersections)
			sumOutlinks += routedIntersection.outgoingLinks.length;
		System.out.printf("Average outdegree: %.2f%n", (double) sumOutlinks / network.routedIntersections.length);
		System.out.println("Number of routed intersections: " + network.routedIntersections.length);
		System.out.println("Number of zones: " + network.zones.length);
		System.out.println("Time steps: " + timeSteps);
		System.out.println();
		
		long numParameters = sumOutlinks * network.zones.length * timeSteps;
		System.out.println("Number of turning parameters: " + numParameters);
		System.out.println("Parameters in RAM: " + numParameters * 8 / (1000 * 1000) + " MB");
		
		int[] linksByOutlinkNum = new int[16];
		for (RoutedIntersection routedIntersection : network.routedIntersections)
			linksByOutlinkNum[routedIntersection.outgoingLinks.length]++;
		System.out.println("Numbers of intersections with specified amount of outgoing links: \n" + Arrays.toString(linksByOutlinkNum));
		
		double[][][][] a = new double[network.routedIntersections.length][network.zones.length][timeSteps][];
		for (int i = 0; i < network.routedIntersections.length; i++)
			for (int j = 0; j < network.zones.length; j++)
				for (int k = 0; k < timeSteps; k++)
					a[i][j][k] = new double[network.routedIntersections[i].outgoingLinks.length];
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