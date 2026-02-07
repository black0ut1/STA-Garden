package black0ut1.util;

import black0ut1.dynamic.loading.link.Link;

public class DynamicUtils {
	
	/**
	 * Computes the time it takes to cross the link under traffic conditions given by the
	 * cumulative inflow and outflow. The travel time is computed for each time t, i.e.
	 * travelTime[t] is the travel time experienced by a vehicle that entered the link
	 * just before t-th time step.
	 * <p>
	 * For each t, the travel time will not be lower than free flow time of the link. Also
	 * the travel time is not in discrete time steps, but rather is is interpolated from
	 * (discrete time) cumulative flows and is continuous.
	 * <p>
	 * The resulting array may contain positive infinities at the end, if the cumulative
	 * flows do not contain enough data for some of the last time steps (this will occur
	 * if the DNL wasn't ran until all the traffic arrived at destinations).
	 * @param cumulativeInflow Cumulative inflow of said link.
	 * @param cumulativeOutflow Cumulative outflow of said link.
	 * @param stepSize Size of a single time step.
	 * @param freeFlowTime Free flow time of said link.
	 * @return Array of travel times for each time t.
	 */
	public static double[] computeTravelTime(double[] cumulativeInflow, double[] cumulativeOutflow,
											 double stepSize, double freeFlowTime) {
		double[] travelTimes = new double[cumulativeInflow.length];
		
		for (int t = 0; t < travelTimes.length; t++) {
			// the number of a vehicle, that entered the link at time t
			double n = cumulativeInflow[t];
			
			double T = Double.POSITIVE_INFINITY; // the time this vehicle left the link
			for (int t2 = t; t2 < travelTimes.length; t2++) {
				double outflow2 = cumulativeOutflow[t2];
				
				if (Math.abs(n - outflow2) < 1e-8) { // the exit time is exactly integer
					T = t2;
					break;
				} else if (n < outflow2) { // the exit time must be interpolated
					// exit time will be interpolated between t1 and t2
					int t1 = t2 - 1;
					double outflow1 = cumulativeOutflow[t1];
					
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
	 * Computes travel time for arbitrary time t.
	 */
	public static double computeTravelTime(double t, Link link, double stepSize) {
		// compute the number n of vehicle that entered at time t
		double n;
		int rounded = (int) Math.round(t);
		if (Math.abs(t - rounded) < 1e-8) {
			n = link.cumulativeInflow[rounded];
		} else {
			int t0 = (int) t;
			double p = t - t0; // decimal part of t
			double A = link.cumulativeInflow[t0];
			double B = link.cumulativeInflow[t0 + 1];
			n = (1 - p) * A + p * B; // interpolated value of cumulative inflow at t
		}
		
		// compute the time of departure of n-th vehicle
		double T = Double.POSITIVE_INFINITY;
		for (int t2 = (int) t; t2 < link.cumulativeOutflow.length; t2++) {
			double outflow2 = link.cumulativeOutflow[t2];
			
			if (Math.abs(n - outflow2) < 1e-8) {
				T = t2;
				break;
			} else if (n < outflow2) {
				int t1 = t2 - 1;
				double outflow1 = link.cumulativeOutflow[t2 - 1];
				
				T = t1 + (n - outflow1) / (outflow2 - outflow1);
				break;
			}
		}
		
		return Math.max(stepSize * (T - t), link.length / link.freeFlowSpeed);
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
