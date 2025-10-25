package black0ut1.dynamic;

import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.mixture.MixtureFractions;

/**
 * This class combines Dynamic Network Loading, Time Dependent Shortest Path and Dynamic
 * User Equilibrium submodels into Dynamic Traffic Assignment as a whole.
 * @author Petr Pernicka
 */
public class DynamicTrafficAssignment {
	
	/** Maximum number of iterations for the main cycle of DTA. */
	protected final int maxIterations;
	/** The route choice for initial turning fractions (like AON initialization in STA). */
	protected final StaticRouteChoice initialRouteChoice;
	/** The DNL scheme used throughout the dynamic assignment. */
	protected final DynamicNetworkLoading dnl;
	
	public DynamicTrafficAssignment(StaticRouteChoice initialRouteChoice, DynamicNetworkLoading dnl, int maxIterations) {
		this.initialRouteChoice = initialRouteChoice;
		this.dnl = dnl;
		this.maxIterations = maxIterations;
	}
	
	public void run() {
		// 1. Initialize turning fractions using STA
		MixtureFractions[][] mfs = initialRouteChoice.computeTurningFractions();
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
		
		// 2. Run the dynamic assignment
		for (int i = 0; i < maxIterations; i++) {
			// TODO complete, when TDSP and dynamic route choice are implemented
			// 2.1. Compute time dependent shortest paths from cumulative flows from DNL
			// 2.2. Compute turning fractions from TDSPs using DUE
			// 2.3. Load the network using turning fractions and DNL
		}
	}
}
