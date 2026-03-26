package black0ut1.dynamic.equilibrium;

import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.Convergence;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.tdsp.DestinationShortestPaths;

public class MSA {
	
	protected final DynamicNetwork network;
	protected final TimeDependentODM odm;
	/** Maximum number of iterations for the main cycle of DTA. */
	protected final int maxIterations;
	/** The route choice for initial turning fractions (like AON initialization in STA). */
	protected final StaticRouteChoice initialRouteChoice;
	/** The DNL scheme used throughout the dynamic assignment. */
	protected final DynamicNetworkLoading dnl;
	
	protected final DestinationShortestPaths shortestPaths;
	protected final double stepSize;
	
	public MSA(DynamicNetwork network, TimeDependentODM odm, StaticRouteChoice initialRouteChoice,
			   DynamicNetworkLoading dnl, DestinationShortestPaths tdsp, int maxIterations, double stepSize) {
		this.network = network;
		this.odm = odm;
		this.initialRouteChoice = initialRouteChoice;
		this.dnl = dnl;
		this.maxIterations = maxIterations;
		this.shortestPaths = tdsp;
		this.stepSize = stepSize;
	}
	
	public void run() {
		Convergence convergence = new Convergence(odm);
		
		MixtureFractions[][] mfs = initialRouteChoice.computeInitialMixtureFractions();
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
//		dnl.checkDestinationInflows(false);
		
		var costs = shortestPaths.shortestPathCosts();
		
		double tstt = convergence.totalSystemTravelTime(network, stepSize);
		System.out.println("[DUE] Total system travel time: " + tstt);
		double sptt = convergence.shortestPathTravelTime(costs);
		System.out.println("[DUE] Shortest path travel time: " + sptt);
		double aec = convergence.averageExcessCost(tstt, sptt);
		System.out.println("[DUE] Average excess cost: " + aec);

		
		for (int i = 0; i < maxIterations; i++) {
			double lambda = 1.0 / (i + 2);
			
			MixtureFractions[][] targetMfs = shortestPaths.shortestPathMixtureFractions(costs);
			
			for (int n = 0; n < targetMfs.length; n++)
				for (int t = 0; t < targetMfs[n].length; t++) {
					MixtureFractions mf1 = mfs[n][t];
					MixtureFractions mf2 = targetMfs[n][t];
					
					for (int d = 0; d < mf2.destinationTurningFractions.length; d++) {
						DoubleMatrix tf1 = mf1.destinationTurningFractions[d];
						DoubleMatrix tf2 = mf2.destinationTurningFractions[d];
						
						DoubleMatrix result = tf1.plus(tf2.plus(tf1.scale(-1)).scale(lambda));
						
						mfs[n][t].destinationTurningFractions[d] = result;
					}
				}
			
			dnl.loadNetwork();
			
			costs = shortestPaths.shortestPathCosts();
			
			tstt = convergence.totalSystemTravelTime(network, stepSize);
			System.out.println("[DUE] Total system travel time: " + tstt);
			sptt = convergence.shortestPathTravelTime(costs);
			System.out.println("[DUE] Shortest path travel time: " + sptt);
			aec = convergence.averageExcessCost(tstt, sptt);
			System.out.println("[DUE] Average excess cost: " + aec);
		}
		
//		dnl.checkDestinationInflows(false);
	}
}
