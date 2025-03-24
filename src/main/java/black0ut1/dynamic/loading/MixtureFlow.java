package black0ut1.dynamic.loading;

import black0ut1.data.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 * TODO optimize this class using primitive collections
 * TODO implement ArrayMixtureFlow
 * @param totalFlow The total flow, which consists of portions heading
 * to different destinations.
 * @param portions Map from destination to a portion of total flow.
 * Each portion is from the interval [0, 1] and they must sum up to 1.
 * If some destination is not included in this map, zero percent of
 * the total totalFlow head there.
 */
public record MixtureFlow(
		double totalFlow,
		HashMap<Integer, Double> portions
) {
	/**
	 * Return the portion of the total flow that is heading for a
	 * specific destination.
	 * @param destination Destination index.
	 * @return totalFlow * destination portion
	 */
	public double getDestinationFlow(int destination) {
		if (!portions.containsKey(destination))
			return 0;
		
		return totalFlow * portions.get(destination);
	}
	
	/**
	 * {@code this.totalFlow} is split into {@code flow} and
	 * {@code this.totalFlow - flow} from which two new mixture flows
	 * with the same portions as {@code this.portions} are created.
	 * @param flow The splitting flow, cannot be larger than
	 * {@code this.totalflow}.
	 * @return Pair of mixture flows where first.totalFlow = flow and
	 * second.totalFlow = this.totalFlow - flow.
	 * TODO optimize, do not create two new objects, modify this and return second
	 */
	public Pair<MixtureFlow, MixtureFlow> splitFlow(double flow) {
		return new Pair<>(
				new MixtureFlow(flow, this.portions),
				new MixtureFlow(this.totalFlow - flow, this.portions)
		);
	}
	
	public MixtureFlow plus(MixtureFlow other) {
		double resultFlow = totalFlow + other.totalFlow;
		HashMap<Integer, Double> resultPortions = new HashMap<>();
		
		Set<Integer> destinationUnion = new HashSet<>();
		destinationUnion.addAll(this.portions().keySet());
		destinationUnion.addAll(other.portions().keySet());
		
		for (int destination : destinationUnion) {
			double thisDestinationFlow = this.getDestinationFlow(destination);
			double otherDestinationFlow = other.getDestinationFlow(destination);
			
			resultPortions.put(destination, (thisDestinationFlow + otherDestinationFlow) / resultFlow);
		}
		
		return new MixtureFlow(resultFlow, resultPortions);
	}
	
	public void checkPortions() {
		double sum = 0;
		for (double value : portions.values())
			sum += value;
		
		if (sum != 1)
			System.err.println("Portions do not sum to 1. Sum: " + sum);
	}
}
