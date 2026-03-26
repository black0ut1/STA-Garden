package black0ut1.dynamic.loading.mixture;

import black0ut1.data.DoubleMatrix;

/**
 * Class for defining, how MixtureFlow turns at an intersection. It
 * decomposes turning fractions of some intersection by destinations.
 * Turning fraction tf[i][j] is a number from interval [0, 1] that
 * expresses portion of flow entering intersection from incoming
 * link i, that exits using outgoing link j.
 */
public class MixtureFractions {
	
	public final DoubleMatrix[] destinationTurningFractions;
	
	public MixtureFractions(DoubleMatrix[] destinationTurningFractions) {
		this.destinationTurningFractions = destinationTurningFractions;
	}
	
	public DoubleMatrix getDestinationFractions(int destination) {
		return destinationTurningFractions[destination];
	}
	
	public void checkPartialFractions() {
		for (int d = 0; d < destinationTurningFractions.length; d++) {
			DoubleMatrix tf = destinationTurningFractions[d];
			
			double[] sumIncoming = new double[tf.m];
			for (int i = 0; i < tf.m; i++)
				for (int j = 0; j < tf.n; j++)
					sumIncoming[i] += tf.get(i, j);
			
			for (int i = 0; i < tf.m; i++) {
				if (sumIncoming[i] != 1) {
					System.err.printf("Turning fractions of flows coming from " +
									"incoming link %d don't sum up to 1: %f (destination %d)%n",
							i, sumIncoming[i], d);
				}
			}
		}
	}
}
