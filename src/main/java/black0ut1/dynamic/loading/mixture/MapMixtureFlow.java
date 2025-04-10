package black0ut1.dynamic.loading.mixture;

import com.carrotsearch.hppc.IntDoubleHashMap;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;
import com.carrotsearch.hppc.procedures.IntDoubleProcedure;

public class MapMixtureFlow extends MixtureFlow {
	
	/** Map from destination to a portion of total flow. Each portion
	 * is from the interval [0, 1] and they must sum up to 1. If some
	 * destination is not included in this map, zero percent of the
	 * total totalFlow head there. */
	protected final IntDoubleHashMap mixtures;
	
	public MapMixtureFlow(double totalFlow, IntDoubleHashMap mixtures) {
		super(totalFlow);
		this.mixtures = mixtures;
	}
	
	public double getDestinationFlow(int destination) {
		return totalFlow * mixtures.get(destination);
	}
	
	public void forEach(MixtureFlow.Consumer consumer) {
		mixtures.forEach((IntDoubleProcedure) consumer::accept);
	}
	
	public MapMixtureFlow copyWithFlow(double newFlow) {
		return new MapMixtureFlow(newFlow, mixtures);
	}
	
	public void checkPortions(double tolerance) {
		double sum = 0;
		for (IntDoubleCursor c : mixtures)
			sum += c.value;
		
		if (sum != 0 && Math.abs(sum - 1) > tolerance)
			System.out.println("Portions do not sum to 1. Sum: " + sum);
	}
}
