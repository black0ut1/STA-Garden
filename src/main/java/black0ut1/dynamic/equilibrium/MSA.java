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
	
	protected final Convergence convergence;
	
	public MSA(DynamicNetwork network, TimeDependentODM odm, StaticRouteChoice initialRouteChoice,
	           DynamicNetworkLoading dnl, DOT tdsp, int maxIterations, double stepSize, Convergence convergence) {
		this.network = network;
		this.odm = odm;
		this.initialRouteChoice = initialRouteChoice;
		this.dnl = dnl;
		this.maxIterations = maxIterations;
		this.tdsp = tdsp;
		this.stepSize = stepSize;
		this.convergence = convergence;
	}
	
	public void run() {
		var mfs = initialRouteChoice.computeInitialMixtureFractions();
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
//		dnl.checkDestinationInflows(false);
		
		var pair = tdsp.shortestPaths(mfs);
		MixtureOutgoingFractions.Costs costs = pair.first();
		MixtureOutgoingFractions.Indices shortestOugoingLinks = pair.second();
		
		double[] criterions = convergence.computeAll(costs);
		System.out.println("[DUE] TTST: " + criterions[0]);
		System.out.println("[DUE] SPTT: " + criterions[1]);
		System.out.println("[DUE] AEC:  " + criterions[2]);
		System.out.println("[DUE] RG:   " + criterions[3]);

		
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
			
			criterions = convergence.computeAll(costs);
			System.out.println("[DUE] TTST: " + criterions[0]);
			System.out.println("[DUE] SPTT: " + criterions[1]);
			System.out.println("[DUE] AEC:  " + criterions[2]);
			System.out.println("[DUE] RG:   " + criterions[3]);
		}
		
//		dnl.checkDestinationInflows(false);
	}
}
