package black0ut1.dynamic.loading.mixture;

public class ArrayMixtureFlow extends MixtureFlow {
	
	protected final double[] mixtures;
	
	public ArrayMixtureFlow(double totalFlow, double[] mixtures) {
		super(totalFlow);
		this.mixtures = mixtures;
	}
	
	public double getDestinationFlow(int destination) {
		return totalFlow * mixtures[destination];
	}
	
	public void forEach(MixtureFlow.Consumer consumer) {
		for (int i = 0; i < mixtures.length; i++)
			if (mixtures[i] > 0)
				consumer.accept(i, mixtures[i]);
	}
	
	public ArrayMixtureFlow copyWithFlow(double newFlow) {
		return new ArrayMixtureFlow(newFlow, mixtures);
	}
	
	@Override
	public void checkPortions(double tolerance) {
		double sum = 0;
		for (double portion : mixtures)
			sum += portion;
		
		if (sum != 0 && Math.abs(sum - 1) > tolerance)
			System.out.println("Portions do not sum to 1. Sum: " + sum);
	}
}
