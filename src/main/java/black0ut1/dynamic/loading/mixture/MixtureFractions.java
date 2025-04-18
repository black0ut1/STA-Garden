package black0ut1.dynamic.loading.mixture;

import black0ut1.data.DoubleMatrix;

import java.util.Arrays;

/**
 * Class for defining, how MixtureFlow turns at an intersection. It
 * decomposes turning fractions of some intersection by destinations.
 * Turning fraction tf[i][j] is a number from interval [0, 1] that
 * expresses portion of flow entering intersection from incoming
 * link i, that exits using outgoing link j.
 */
public class MixtureFractions {
	
	/** Map from destination to turning fractions of flow heading to
	 * that destination. */
	public final int[] destinations;
	public final DoubleMatrix[] destinationTurningFractions;
	
	public MixtureFractions(int[] destinations, DoubleMatrix[] destinationTurningFractions, int len) {
		this.destinations = new int[len];
		this.destinationTurningFractions = new DoubleMatrix[len];
		System.arraycopy(destinations, 0, this.destinations, 0, len);
		System.arraycopy(destinationTurningFractions, 0, this.destinationTurningFractions, 0, len);
	}
	
	public DoubleMatrix getDestinationFractions(int destination) {
		int i = Arrays.binarySearch(destinations, destination);
		if (i < 0)
			throw new ArrayIndexOutOfBoundsException(destination);
		
		return destinationTurningFractions[i];
	}
	
	public void checkPartialFractions() {
		for (int a = 0; a < destinations.length; a++) {
			int destination = destinations[a];
			DoubleMatrix tf = destinationTurningFractions[a];
			
			double[] sumIncoming = new double[tf.m];
			for (int i = 0; i < tf.m; i++)
				for (int j = 0; j < tf.n; j++)
					sumIncoming[i] += tf.get(i, j);
			
			for (int i = 0; i < tf.m; i++) {
				if (sumIncoming[i] != 1) {
					System.err.printf("Turning fractions of flows coming from " +
									"incoming link %d don't sum up to 1: %f (destination %d)%n",
							i, sumIncoming[i], destination);
				}
			}
		}
	}
}
