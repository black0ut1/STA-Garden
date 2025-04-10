package black0ut1.dynamic.loading.mixture;

public class ArrayMixtureFractions implements MixtureFractions {
	
	protected final double[][][] destinationTurningFractions;
	
	public ArrayMixtureFractions(double[][][] destinationTurningFractions) {
		this.destinationTurningFractions = destinationTurningFractions;
	}
	
	public double[][] getDestinationFractions(int destination) {
		return destinationTurningFractions[destination];
	}
	
	public void forEach(MixtureFractions.Consumer consumer) {
		for (int i = 0; i < destinationTurningFractions.length; i++)
			if (destinationTurningFractions[i] != null)
				consumer.accept(i, destinationTurningFractions[i]);
	}
	
	@Override
	public void checkPartialFractions() {
		for (int destination = 0; destination < destinationTurningFractions.length; destination++) {
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
