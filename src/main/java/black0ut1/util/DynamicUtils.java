package black0ut1.util;

public class DynamicUtils {
	
	/**
	 * Computes the time it takes to cross the link under traffic conditions given by the
	 * cumulative inflow and outflow. The travel time is computed for each time t, i.e.
	 * travelTime[t] is the travel time experienced by a vehicle that entered the link
	 * during (t-1)-th time step.
	 * <p>
	 * For each t, the travel time will not be lower than free flow time of the link. Also
	 * the travel time is not in discrete time steps, but rather is is interpolated from
	 * (discrete) cumulative flows and is continuous.
	 * <p>
	 * The resulting array may contain NaNs at the end, if the cumulative flows do not
	 * contain enough data for some of the last time steps (this will occur if the DNL
	 * wasn't ran until all the traffic arrived at destinations).
	 * @param cumulativeInflow Cumulative inflow of said link.
	 * @param cumulativeOutflow Cumulative outflow of said link.
	 * @param stepSize Size of a single time step.
	 * @param freeFlowTime Free flow time of said link.
	 * @return Array of travel times for each time t.
	 */
	public static double[] computeTravelTime(double[] cumulativeInflow, double[] cumulativeOutflow, double stepSize, double freeFlowTime) {
		double[] travelTimes = new double[cumulativeInflow.length];
		
		for (int t = 0; t < travelTimes.length; t++) {
			double n = cumulativeInflow[t];
			
			double T = Double.NaN;
			for (int t2 = t; t2 < travelTimes.length; t2++) {
				double outflow2 = cumulativeOutflow[t2];
				
				if (Math.abs(n - outflow2) < 1e-8) {
					T = t2;
					break;
				} else if (n < outflow2) {
					int t1 = t2 - 1;
					double outflow1 = cumulativeOutflow[t2 - 1];
					
					T = t1 + (n - outflow1) / (outflow2 - outflow1);
					break;
				}
			}
			
			// travel time cannot be lower than free flow time
			travelTimes[t] = Math.max(stepSize * (T - t), freeFlowTime);
		}
		
		return travelTimes;
	}
	
	/**
	 * Computes the number of vehicles on link for each time t.
	 * @param cumulativeInflow Cumulative inflow of said link.
	 * @param cumulativeOutflow Cumulative outflow of said link.
	 * @return Array of numbers of vehicle on link for each time t.
	 */
	public static double[] computeVehiclesOnLink(double[] cumulativeInflow, double[] cumulativeOutflow) {
		double[] vehiclesOnLink = new double[cumulativeInflow.length];
		
		for (int t = 0; t < vehiclesOnLink.length; t++)
			vehiclesOnLink[t] = cumulativeInflow[t] - cumulativeOutflow[t];
		
		return vehiclesOnLink;
	}
}
