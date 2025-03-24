package black0ut1.dynamic.loading;

import java.util.HashMap;

/**
 * Class for defining, how MixtureFlow turns at an intersection. It
 * decomposes turning fractions of some intersection by destinations.
 * Turning fraction tf[i][j] is a number from interval [0, 1] that
 * expresses portion of flow entering intersection from incoming
 * link i, that exits using outgoing link j.
 * @param destinationTurningFractions Map from destination to turning
 * fractions of flow heading to that destination.
 */
public record MixtureFractions(
		HashMap<Integer, double[][]> destinationTurningFractions
) {
	public double[][] getDestinationFractions(int destination) {
		return destinationTurningFractions.get(destination);
	}
	
	public void checkPartialFractions() {
		for (int destination : destinationTurningFractions.keySet()) {
			double[][] tf = getDestinationFractions(destination);
			
			double[] sumIncoming = new double[tf.length];
			for (int i = 0; i < tf.length; i++)
				for (int j = 0; j < tf[0].length; j++)
					sumIncoming[i] += tf[i][j];
			
			for (int i = 0; i < tf.length; i++) {
				if (sumIncoming[i] != 1) {
					System.err.printf("Turning fractions of flows coming from " +
									"incoming link %d don't sum up to 1: %f (destination %d)%n",
							i, sumIncoming[i], destination);
				}
			}
		}
	}
}
