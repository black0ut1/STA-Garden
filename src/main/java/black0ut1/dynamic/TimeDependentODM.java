package black0ut1.dynamic;

import black0ut1.data.DoubleMatrix;

/**
 * Time-dependent OD matrix, where each OD entry is a dicrete-time sequence of flows.
 */
public class TimeDependentODM {
	
	/** 3D array, where first index is origin, second is destination
	 *  and third is the time interval. */
	protected final double[] flow;
	
	public final int zones;
	public final int timeSteps;
	
	protected TimeDependentODM(double[] flow, int zones, int timeSteps) {
		this.flow = flow;
		this.zones = zones;
		this.timeSteps = timeSteps;
	}
	
	public double getFlow(int origin, int destination, int time) {
		if (time >= timeSteps)
			return 0;
		
		return flow[time * zones * zones + origin * zones + destination];
	}
	
	public static TimeDependentODM fromStaticODM(DoubleMatrix odm, int timeSteps) {
		double[] flow = new double[odm.n * odm.n * timeSteps];
		
		for (int i = 0; i < odm.n; i++)
			for (int j = 0; j < odm.n; j++) {
				double uniformFlow = odm.get(i, j) / timeSteps;
				
				for (int t = 0; t < timeSteps; t++)
					flow[t * odm.n * odm.n + i * odm.n + j] = uniformFlow;
			}
		
		return new TimeDependentODM(flow, odm.n, timeSteps);
	}
}
