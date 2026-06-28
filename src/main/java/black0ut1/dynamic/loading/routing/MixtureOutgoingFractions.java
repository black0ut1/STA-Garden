package black0ut1.dynamic.loading.routing;

/**
 * Class for defining, how MixtureFlow turns at an intersection. It
 * decomposes turning fractions of some intersection by destinations.
 * Turning fraction tf[i][j] is a number from interval [0, 1] that
 * expresses portion of flow entering intersection from incoming
 * link i, that exits using outgoing link j.
 */
public class MixtureOutgoingFractions {
	
	protected final double[][] destinationTurningFractions;
	public final int destinations;
	
	public MixtureOutgoingFractions(double[][] destinationTurningFractions) {
		this.destinationTurningFractions = destinationTurningFractions;
		this.destinations = destinationTurningFractions.length;
	}
	
	public double getFraction(int d, int j) {
		return destinationTurningFractions[d][j];
	}
	
	public void setFraction(int d, int j, double val) {
		destinationTurningFractions[d][j] = val;
	}
}
