package black0ut1.dynamic.equilibrium;

import black0ut1.dynamic.Convergence;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.dynamic.tdsp.DOT;

public class MSA {
	
	protected final DynamicNetwork network;
	protected final TimeDependentODM odm;
	/** Maximum number of iterations for the main cycle of DTA. */
	protected final int maxIterations;
	/** The route choice for initial turning fractions (like AON initialization in STA). */
	protected final StaticRouteChoice initialRouteChoice;
	/** The DNL scheme used throughout the dynamic assignment. */
	protected final DynamicNetworkLoading dnl;
	
	protected final DOT tdsp;
	protected final double stepSize;
	
	public MSA(DynamicNetwork network, TimeDependentODM odm, StaticRouteChoice initialRouteChoice,
	           DynamicNetworkLoading dnl, DOT tdsp, int maxIterations, double stepSize) {
		this.network = network;
		this.odm = odm;
		this.initialRouteChoice = initialRouteChoice;
		this.dnl = dnl;
		this.maxIterations = maxIterations;
		this.tdsp = tdsp;
		this.stepSize = stepSize;
	}
	
	public void run() {
		Convergence convergence = new Convergence(odm);
		
		var mfs = initialRouteChoice.computeInitialMixtureFractions();
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
//		dnl.checkDestinationInflows(false);
		
		var pair = tdsp.shortestPaths(mfs);
		double[][][] costs = pair.first();
		MixtureOutgoingFractions.Indices shortestOugoingLinks = pair.second();
		
		double tstt = convergence.totalSystemTravelTime(network, stepSize);
		System.out.println("[DUE] Total system travel time:  " + tstt);
		double sptt = convergence.shortestPathTravelTime(costs);
		System.out.println("[DUE] Shortest path travel time: " + sptt);
		double aec = convergence.averageExcessCost(tstt, sptt);
		System.out.println("[DUE] Average excess cost: " + aec);
		double rg = convergence.relativeGap(tstt, sptt);
		System.out.println("[DUE] Relative gap:        " + rg);

		
		for (int i = 0; i < maxIterations; i++) {
			System.out.println("[DUE] Iteration: " + i);
			
			double lambda = 1.0 / (i + 2);
			
			for (int n = 0; n < mfs.intersections; n++)
				for (int t = 0; t < mfs.timeSteps; t++) {
					for (int d = 0; d < network.zones.length; d++) {
						for (int j = 0; j < network.routedIntersections[n].outgoingLinks.length; j++) {
							double fraction = mfs.getFraction(n, t, d, j);
							
							double newFraction;
							if (shortestOugoingLinks.getIndex(n, t, d) == j) {
								newFraction = (1 - lambda) * fraction + lambda;
							} else {
								newFraction = (1 - lambda) * fraction;
							}
							
							mfs.setFraction(n, t, d, j, newFraction);
						}
					}
				}
			
			dnl.loadNetwork();
			
			pair = tdsp.shortestPaths(mfs);
			costs = pair.first();
			shortestOugoingLinks = pair.second();
			
			tstt = convergence.totalSystemTravelTime(network, stepSize);
			System.out.println("[DUE] Total system travel time:  " + tstt);
			sptt = convergence.shortestPathTravelTime(costs);
			System.out.println("[DUE] Shortest path travel time: " + sptt);
			aec = convergence.averageExcessCost(tstt, sptt);
			System.out.println("[DUE] Average excess cost: " + aec);
			rg = convergence.relativeGap(tstt, sptt);
			System.out.println("[DUE] Relative gap:        " + rg);
		}
		
//		dnl.checkDestinationInflows(false);
	}
}
