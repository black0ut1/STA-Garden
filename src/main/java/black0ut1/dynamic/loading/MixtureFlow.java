package black0ut1.dynamic.loading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 * TODO optimize this class using primitive collections
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
	protected final HashMap<Integer, Double> mixtures;
	
	public MixtureFlow(double totalFlow, HashMap<Integer, Double> mixtures) {
		this.totalFlow = totalFlow;
		this.mixtures = mixtures;
	}
	
	public MixtureFlow() {
		this(0, new HashMap<>());
	}
	
	/**
	 * Return the portion of the total flow that is heading for a
	 * specific destination.
	 * @param destination Destination index.
	 * @return totalFlow * destination portion
	 */
	public double getDestinationFlow(int destination) {
		if (!mixtures.containsKey(destination))
			return 0;
		
		return totalFlow * mixtures.get(destination);
	}
	
	public MixtureFlow plus(MixtureFlow other) {
		double resultFlow = totalFlow + other.totalFlow;
		HashMap<Integer, Double> resultPortions = new HashMap<>();
		
		Set<Integer> destinationUnion = new HashSet<>();
		destinationUnion.addAll(this.mixtures.keySet());
		destinationUnion.addAll(other.mixtures.keySet());
		
		for (int destination : destinationUnion) {
			double thisDestinationFlow = this.getDestinationFlow(destination);
			double otherDestinationFlow = other.getDestinationFlow(destination);
			
			resultPortions.put(destination, (thisDestinationFlow + otherDestinationFlow) / resultFlow);
		}
		
		return new MixtureFlow(resultFlow, resultPortions);
	}
	
	public void forEach(Consumer consumer) {
		for (Map.Entry<Integer, Double> entry : mixtures.entrySet())
			consumer.accept(entry.getKey(), entry.getValue());
	}
	
	public MixtureFlow copyWithFlow(double newFlow) {
		return new MixtureFlow(newFlow, mixtures);
	}
	
	public void checkPortions(double tolerance, String msg) {
		double sum = 0;
		for (double value : mixtures.values())
			sum += value;
		
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
