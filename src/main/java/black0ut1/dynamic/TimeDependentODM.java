package black0ut1.dynamic;

import black0ut1.data.DoubleMatrix;

import java.util.Arrays;

/**
 * Time-dependent OD matrix, where each OD totalFlow is represented
 * by a time sequence.
 */
public class TimeDependentODM {
	
	/** 3D array, where first index is origin, second is destination
	 *  and third is the time interval. */
	private final double[][][] flow;
	
	public final int zones;
	public final int timeSteps;
	
	public TimeDependentODM(double[][][] flow, int timeSteps) {
		this.flow = flow;
		this.zones = flow.length;
		this.timeSteps = timeSteps;
	}
	
	public double getFlow(int origin, int destination, int time) {
		if (time >= timeSteps)
			return 0;
		
		return flow[origin][destination][time];
	}
	
	public static TimeDependentODM fromStaticODM(DoubleMatrix odm, int timeSteps) {
		double[][][] flow = new double[odm.n][odm.n][timeSteps];
		
		for (int i = 0; i < odm.n; i++) {
			for (int j = 0; j < odm.n; j++) {
				double uniformFlow = odm.get(i, j) / timeSteps;
				Arrays.fill(flow[i][j], uniformFlow);
			}
		}
		
		return new TimeDependentODM(flow, timeSteps);
	}
}
