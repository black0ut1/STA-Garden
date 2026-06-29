package black0ut1.dynamic;

import black0ut1.data.DoubleMatrix;

/**
 * Time-dependent OD matrix, where each OD entry is a dicrete-time sequence of flows.
 */
public class TimeDependentODM {
	
	/**
	 * 3D array, where first index is origin, second is destination
	 * and third is the time interval.
	 */
	protected final double[] demand;
	
	/** The number of zones */
	public final int zones;
	
	/**
	 * The number of time steps for which are values in the ODM defined. That is, the
	 * {@link #getDemand} method will return 0 for any {@code time >= this.timeSteps}.
	 */
	public final int timeSteps;
	
	protected TimeDependentODM(double[] demand, int zones, int timeSteps) {
		this.demand = demand;
		this.zones = zones;
		this.timeSteps = timeSteps;
	}
	
	/**
	 * Returns the traffic demand from specified origin to destination, with the flow
	 * departing from origin at specified time.
	 * @param origin Origin intersection index.
	 * @param destination Destination intersection index.
	 * @param time Time step.
	 * @return Traffic demand.
	 */
	public double getDemand(int origin, int destination, int time) {
		if (time >= timeSteps)
			return 0;
		
		return demand[time * zones * zones + origin * zones + destination];
	}
	
	public TimeDependentODM scale(double scale) {
		double[] scaledDemand = new double[demand.length];
		for (int i = 0; i < demand.length; i++)
			scaledDemand[i] = demand[i] * scale;
		
		return new TimeDependentODM(scaledDemand, zones, timeSteps);
	}
	
	/**
	 * Creates a time-dependent ODM from a static ODM, such that an original value (i, j)
	 * is distrubuted uniformly over time.
	 * @param odm Matrix of doubles representing the static ODM.
	 * @param timeSteps Number of time steps for which the TD-ODM is defined.
	 * @return Time-dependent ODM.
	 */
	public static TimeDependentODM fromStaticUniform(DoubleMatrix odm, int timeSteps) {
		double[] demand = new double[odm.n * odm.n * timeSteps];
		
		for (int i = 0; i < odm.n; i++)
			for (int j = 0; j < odm.n; j++) {
				double uniformDemand = odm.get(i, j) / timeSteps;
				
				for (int t = 0; t < timeSteps; t++)
					demand[t * odm.n * odm.n + i * odm.n + j] = uniformDemand;
			}
		
		return new TimeDependentODM(demand, odm.n, timeSteps);
	}
	
	/**
	 * Creates a time-dependent ODM from a static ODM, such that an original value (i, j)
	 * is distrubuted as a pseudo-gaussian distribution over time.
	 * @param odm Matrix of doubles representing the static ODM.
	 * @param timeSteps Number of time steps for which the TD-ODM is defined.
	 * @return Time-dependent ODM.
	 */
	public static TimeDependentODM fromStaticGaussian(DoubleMatrix odm, int timeSteps) {
		// Create pseudo-gaussian distribution.
		double[] gaussian = new double[timeSteps];
		final double std = timeSteps / 6.0;
		final double mean = (timeSteps - 1) / 2.0;
		
		double sum = 0;
		for (int i = 0; i < timeSteps; i++) {
			double x = i - mean;
			gaussian[i] = Math.exp(-(x * x) / (2 * std * std));
			sum += gaussian[i];
		}
		
		for (int i = 0; i < timeSteps; i++)
			gaussian[i] /= sum;
		
		return fromStaticCustom(odm, gaussian);
	}
	
	/**
	 * Creates a time-dependent ODM from a static ODM, such that an original value (i, j)
	 * is distrubuted over time according to the specified distribution.
	 * @param odm Matrix of doubles representing the static ODM.
	 * @param distribution The distribution of static demand over time (must sum up to 1).
	 * The length of the array is the number of time steps for which the TD-ODM is defined.
	 * @return Time-dependent ODM.
	 */
	public static TimeDependentODM fromStaticCustom(DoubleMatrix odm, double[] distribution) {
		double[] demand = new double[odm.n * odm.n * distribution.length];
		
		for (int i = 0; i < odm.n; i++)
			for (int j = 0; j < odm.n; j++) {
				double staticDemand = odm.get(i, j);
				
				for (int t = 0; t < distribution.length; t++)
					demand[t * odm.n * odm.n + i * odm.n + j] = staticDemand * distribution[t];
			}
		
		return new TimeDependentODM(demand, odm.n, distribution.length);
	}
}
