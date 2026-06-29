package black0ut1.dynamic.loading.routing;

import black0ut1.dynamic.DynamicNetwork;

/**
 * Class for defining, how MixtureFlow turns at an intersection. It
 * decomposes turning fractions of some intersection by destinations.
 * Turning fraction tf[i][j] is a number from interval [0, 1] that
 * expresses portion of flow entering intersection from incoming
 * link i, that exits using outgoing link j.
 */
public class MixtureOutgoingFractions {
	
	protected final double[][][] destinationTurningFractions;
	public final int destinations;
	public final int timeSteps;
	
	public MixtureOutgoingFractions(DynamicNetwork network, int n, int timeSteps) {
		this.destinationTurningFractions = new double[timeSteps][network.zones.length][];
		for (int t = 0; t < timeSteps; t++)
			for (int i = 0; i < network.zones.length; i++)
				destinationTurningFractions[t][i] = new double[network.routedIntersections[n].outgoingLinks.length];
		
		this.destinations = network.zones.length;
		this.timeSteps = timeSteps;
	}
	
	public double getFraction(int t, int d, int j) {
		return destinationTurningFractions[t][d][j];
	}
	
	public void setFraction(int t, int d, int j, double val) {
		destinationTurningFractions[t][d][j] = val;
	}
}
