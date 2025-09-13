package black0ut1.dynamic.loading.dnl;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.RoutedIntersection;

/**
 * Abstract class that wraps the functionality of dynamic network
 * loading. Subclasses of this class would differ by their strategy of
 * loading the traffic each time step.
 * <p>
 * The typical usage would be:										  <br>
 * 1. Determine turning fractions using DUE							  <br>
 * 2. dnl.setTurningFractions(turningFractions)						  <br>
 * 3. dnl.loadNetwork()												  <br>
 * 4. Determine time-dependent travel times							  <br>
 * 5. dnl.resetNetwork()											  <br>
 * 6. Go to 1.														  <br>
 */
public abstract class DynamicNetworkLoading {
	
	/** The network to be loaded. */
	protected final DynamicNetwork network;
	/**
	 * Time dependent origin-destination matrix. Its flows will be
	 * loaded onto the network.
	 */
	protected final TimeDependentODM odm;
	/** The period of one time step [h]. */
	protected final double stepSize;
	/** The number of time steps the DNL will take. */
	protected final int steps;
	
	public DynamicNetworkLoading(DynamicNetwork network, TimeDependentODM odm, double stepSize, int steps) {
		this.network = network;
		this.odm = odm;
		this.stepSize = stepSize;
		this.steps = steps;
	}
	
	/**
	 * Launches the network loading. Can end before taking {@link #steps} steps if there
	 * is no flow on the network.
	 * @return The final number of steps taken which can be lower than {@link #steps}
	 * (but not higher).
	 */
	public int loadNetwork() {
		int t;
		for (t = 0; t < steps; t++) {
			System.out.println("========= Time " + t + " =========");
			
			loadForTime(t);
			
			double totalFlow = getTotalFlowOnNetwork(t);
			System.out.println("Total flow on network: " + totalFlow);
			if (totalFlow < 1e-5)
				break;
		}
		
		return t + 1;
	}
	
	protected abstract void loadForTime(int t);
	
	/**
	 * Computes the sum of flows contained on all links at the end of t-th time step.
	 * @param t Time step.
	 * @return Sum of all flows on network.
	 */
	public double getTotalFlowOnNetwork(int t) {
		double totalFlow = 0;
		
		for (Link link : network.allLinks)
			totalFlow += link.cumulativeInflow[t + 1] - link.cumulativeOutflow[t + 1];
		
		return totalFlow;
	}
	
	/**
	 * Sets mixture fractions for each intersection and for each time
	 * step.
	 * @param turningFractions 2D array of mixture flows, where first
	 * index represents intersection and the second represents the
	 * time step - {@code turningFractions[i][t]} are MixtureFractions
	 * used by intersection i during time step t.
	 */
	public void setTurningFractions(MixtureFractions[][] turningFractions) {
		for (int i = 0; i < network.routedIntersections.length; i++)
			network.routedIntersections[i].setTurningFractions(turningFractions[i]);
	}
	
	/**
	 * Resets all the mutable states of network components to their
	 * initial values. Namely, links have zeroed flow variables and
	 * sending/receiving flows, destinations have zeroed inflow and
	 * intersection turning fractions are removed.
	 * <p>
	 * Repeated BasicDNL on a network that is not reset should yield
	 * the same results (because cumulativeInflow[0] and
	 * cumulativeOutflow[0] are always 0, the rest is overwritten). It
	 * would be different with ILTM_DNL, because it could benefit from
	 * the values from previous DNL (see the paper - warm start).
	 */
	public void resetNetwork() {
		for (Link link : network.allLinks)
			link.reset();
		
		for (Destination destination : network.destinations)
			destination.reset();
		
		for (RoutedIntersection intersection : network.routedIntersections)
			intersection.setTurningFractions(null);
	}
	
	/**
	 * Checks the consistency of flows that arrived to destinations.
	 * For this method to work properly, every flow must arrive to
	 * some destination. It checks if the total flow arrived to a
	 * destination is different from what ODM tells should arrive. It
	 * also checks if the MixtureFlow contains only the one
	 * destination.
	 * @param steps The total number of steps the DNL took to finish,
	 * as returned by {@code loadNetwork()}.
	 * @param verbose More information.
	 */
	public void checkDestinationInflows(int steps, boolean verbose) {
		System.out.println("============ Checking arrived flows ============");
		double[] odmDestinationInflow = new double[network.destinations.length];
		
		for (int destination = 0; destination < odm.zones; destination++)
			for (int origin = 0; origin < odm.zones; origin++)
				for (int t = 0; t < odm.timeSteps; t++)
					odmDestinationInflow[destination] += odm.getFlow(origin, destination, t);
		
		double[] networkDestinationInflow = new double[network.destinations.length];
		for (int destination = 0; destination < network.destinations.length; destination++) {
			
			var destiantionInflow = network.destinations[destination].inflow;
			for (int t = 0; t < steps; t++) {
				
				for (int d = 0; d < destiantionInflow[t].destinations.length; d++) {
					int destination1 = destiantionInflow[t].destinations[d];
					
					if (destination1 != destination)
						System.out.println("Mixture flow arrived to destination " + destination +
								" contains a portion belonging to other destination " + destination1);
				}
				
				networkDestinationInflow[destination] += destiantionInflow[t].totalFlow;
			}
		}
		
		if (verbose)
			for (int i = 0; i < network.destinations.length; i++) {
				if (Math.abs(odmDestinationInflow[i] - networkDestinationInflow[i]) > 1e-5) {
					System.out.println("The total inflow into destination " + i + " is different from ODM values. " +
							"The total flow arrived is " + networkDestinationInflow[i] + ", but ODM says "
							+ odmDestinationInflow[i] + " should arrive.");
				}
			}
		
		double odmTotal = 0;
		for (double v : odmDestinationInflow)
			odmTotal += v;
		System.out.println("Sum of all ODM values: " + odmTotal);
		
		double networkTotal = 0;
		for (double v : networkDestinationInflow)
			networkTotal += v;
		System.out.println("Sum of all flows arrived at every destination: " + networkTotal);
		
		double avgDifference = 0;
		for (int i = 0; i < network.destinations.length; i++)
			avgDifference += Math.abs(odmDestinationInflow[i] - networkDestinationInflow[i]);
		System.out.println("Difference between actual inflow to a destination and\n" +
				"inflow according to ODM (averaged over destinations): " + avgDifference / network.destinations.length);
	}
}
