package black0ut1.dynamic.loading.mixture;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;

public class MapMixtureFractions implements MixtureFractions {
	
	/** Map from destination to turning fractions of flow heading to
	 * that destination. */
	protected final IntObjectHashMap<double[][]> destinationTurningFractions;
	
	public MapMixtureFractions(IntObjectHashMap<double[][]> destinationTurningFractions) {
		this.destinationTurningFractions = destinationTurningFractions;
	}
	
	public double[][] getDestinationFractions(int destination) {
		return destinationTurningFractions.get(destination);
	}
	
	public void forEach(MixtureFractions.Consumer consumer) {
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
}
