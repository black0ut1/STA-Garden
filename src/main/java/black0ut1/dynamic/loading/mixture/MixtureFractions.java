package black0ut1.dynamic.loading.mixture;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;

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
	protected final IntObjectHashMap<double[][]> destinationTurningFractions;
	
	public MixtureFractions(IntObjectHashMap<double[][]> destinationTurningFractions) {
		this.destinationTurningFractions = destinationTurningFractions;
	}
	
	public double[][] getDestinationFractions(int destination) {
		return destinationTurningFractions.get(destination);
	}
	
	public void forEach(Consumer consumer) {
		destinationTurningFractions.forEach((IntObjectProcedure<double[][]>) consumer::accept);
	}
	
	public void checkPartialFractions() {
		for (IntCursor c : destinationTurningFractions.keys()) {
			double[][] tf = getDestinationFractions(c.value);
			
			double[] sumIncoming = new double[tf.length];
			for (int i = 0; i < tf.length; i++)
				for (int j = 0; j < tf[0].length; j++)
					sumIncoming[i] += tf[i][j];
			
			for (int i = 0; i < tf.length; i++) {
				if (sumIncoming[i] != 1) {
					System.err.printf("Turning fractions of flows coming from " +
									"incoming link %d don't sum up to 1: %f (destination %d)%n",
							i, sumIncoming[i], c.value);
				}
			}
		}
	}
	
	@FunctionalInterface
	public interface Consumer {
		void accept(int destination, double[][] destinationFractions);
	}
}
