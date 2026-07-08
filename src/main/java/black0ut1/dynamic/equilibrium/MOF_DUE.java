package black0ut1.dynamic.equilibrium;

import black0ut1.dynamic.Convergence;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.tdsp.DOT;

/**
 * Abstract class for Dynamic User Equilibrium algorithms, which use Mixture Outgoing
 * Fractions as route choice representation.
 */
public abstract class MOF_DUE {
	
	protected final DynamicNetwork network;
	protected final TimeDependentODM odm;
	protected final double stepSize;
	
	/** Maximum number of iterations for the main cycle of DTA. */
	protected final int maxIterations;
	/** The route choice for initial turning fractions (like AON initialization in STA). */
	protected final StaticRouteChoice initialRouteChoice;
	/** The DNL scheme used throughout the dynamic assignment. */
	protected final DynamicNetworkLoading dnl;
	/** The version of Decreasing Order of Time algorithm used for the TDSP. */
	protected final DOT tdsp;
	
	protected final Convergence convergence;
	
	public MOF_DUE(DynamicNetwork network, TimeDependentODM odm, StaticRouteChoice initialRouteChoice,
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
	
	public abstract void run();
}
