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
	
	protected final double[][][][] destinationTurningFractions;
	public final int intersections;
	public final int destinations;
	public final int timeSteps;
	
	public MixtureOutgoingFractions(DynamicNetwork network, int timeSteps) {
		this.destinationTurningFractions = new double[network.routedIntersections.length][timeSteps][network.zones.length][];
		for (int n = 0; n < network.routedIntersections.length; n++)
			for (int t = 0; t < timeSteps; t++)
				for (int i = 0; i < network.zones.length; i++)
					destinationTurningFractions[n][t][i] = new double[network.routedIntersections[n].outgoingLinks.length];
		
		this.intersections = network.routedIntersections.length;
		this.destinations = network.zones.length;
		this.timeSteps = timeSteps;
	}
	
	public double getFraction(int n, int t, int d, int j) {
		return destinationTurningFractions[n][t][d][j];
	}
	
	public void setFraction(int n, int t, int d, int j, double val) {
		destinationTurningFractions[n][t][d][j] = val;
	}
}
