package black0ut1.dynamic.loading.mixture;

import com.carrotsearch.hppc.IntDoubleHashMap;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;
import com.carrotsearch.hppc.procedures.IntDoubleProcedure;
import com.carrotsearch.hppc.procedures.IntProcedure;

/**
 * This class represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 * TODO implement ArrayMixtureFlow
 */
public class MixtureFlow {
	
	/** The total flow, which consists of portions heading to
	 * different destinations. */
	public final double totalFlow;
	
	/** Map from destination to a portion of total flow. Each portion
	 * is from the interval [0, 1] and they must sum up to 1. If some
	 * destination is not included in this map, zero percent of the
	 * total totalFlow head there. */
	protected final IntDoubleHashMap mixtures;
	
	public MixtureFlow(double totalFlow, IntDoubleHashMap mixtures) {
		this.totalFlow = totalFlow;
		this.mixtures = mixtures;
	}
	
	public MixtureFlow() {
		this(0, new IntDoubleHashMap());
	}
	
	/**
	 * Return the portion of the total flow that is heading for a
	 * specific destination.
	 * @param destination Destination index.
	 * @return totalFlow * destination portion
	 */
	public double getDestinationFlow(int destination) {
		return totalFlow * mixtures.get(destination);
	}
	
	public MixtureFlow plus(MixtureFlow other) {
		double resultFlow = totalFlow + other.totalFlow;
		var resultPortions = new IntDoubleHashMap();
		
		var destinationUnion = new IntHashSet();
		destinationUnion.addAll(this.mixtures.keys());
		destinationUnion.addAll(other.mixtures.keys());
		
		destinationUnion.forEach((IntProcedure) destination -> {
			double thisDestinationFlow = this.getDestinationFlow(destination);
			double otherDestinationFlow = other.getDestinationFlow(destination);
			
			resultPortions.put(destination, (thisDestinationFlow + otherDestinationFlow) / resultFlow);
		});
		
		return new MixtureFlow(resultFlow, resultPortions);
	}
	
	public void forEach(Consumer consumer) {
		mixtures.forEach((IntDoubleProcedure) consumer::accept);
	}
	
	public MixtureFlow copyWithFlow(double newFlow) {
		return new MixtureFlow(newFlow, mixtures);
	}
	
	public void checkPortions(double tolerance, String msg) {
		double sum = 0;
		for (IntDoubleCursor c : mixtures)
			sum += c.value;
		
		if (sum != 0 && Math.abs(sum - 1) > tolerance) {
			System.out.println(msg);
			System.out.println("Portions do not sum to 1. Sum: " + sum);
		}
	}
	
	@FunctionalInterface
	public interface Consumer {
		void accept(int destination, double portion);
	}
}
