package black0ut1.dynamic.loading.mixture;

import java.util.Arrays;

/**
 * This class represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 * TODO thoroughly document, implementation is fast but opaque
 */
public class MixtureFlow {
	
	/** The total flow, which consists of portions heading to
	 * different destinations. */
	public final double totalFlow;
	
	/* Map from destination to a portion of total flow. Each portion
	 * is from the interval [0, 1] and they must sum up to 1. If some
	 * destination is not included in this map, zero percent of the
	 * total totalFlow head there. */
	/** Array of destinations in ascending order. */
	public final int[] destinations;
	
	public final double[] portions;
	
	/** Using this reduces the number of objects allocated. */
	public static final MixtureFlow ZERO = new MixtureFlow(0, new int[0], new double[0], 0);
	
	public MixtureFlow(double totalFlow, int[] destinations, double[] portions, int len) {
		this.totalFlow = totalFlow;
		
		this.destinations = new int[len];
		this.portions = new double[len];
		System.arraycopy(destinations, 0, this.destinations, 0, len);
		System.arraycopy(portions, 0, this.portions, 0, len);
	}
	
	/**
	 * Return the portion of the total flow that is heading for a
	 * specific destination.
	 * @param destination Destination index.
	 * @return totalFlow * destination portion
	 */
	public double getDestinationFlow(int destination) {
		int i = Arrays.binarySearch(destinations, destination);
		if (i < 0)
			return 0;
		
		return totalFlow * portions[i];
	}
	
	public MixtureFlow plus(MixtureFlow other) {
		double resultFlow = totalFlow + other.totalFlow;
		
		int[] destinationUnion = new int[this.destinations.length + other.destinations.length];
		double[] portions = new double[this.destinations.length + other.destinations.length];
		
		// union of two sorted arrays algorithm
		int m = this.destinations.length, n = other.destinations.length;
		int i = 0, j = 0, len = 0;
		while (i < m && j < n) {
			
			if (this.destinations[i] < other.destinations[j]) {
				destinationUnion[len] = this.destinations[i];
				portions[len] = this.portions[i] * this.totalFlow / resultFlow;
				
				i++;
			} else if (this.destinations[i] > other.destinations[j]) {
				destinationUnion[len] = other.destinations[j];
				portions[len] = other.portions[j] * other.totalFlow / resultFlow;
				
				j++;
			} else {
				destinationUnion[len] = this.destinations[i];
				portions[len] = (this.portions[i] * this.totalFlow
						+ other.portions[j] * other.totalFlow) / resultFlow;
				
				i++;
				j++;
			}
			
			len++;
		}
		while (i < m) {
			destinationUnion[len] = this.destinations[i];
			portions[len] = this.portions[i] * this.totalFlow / resultFlow;
			
			i++;
			len++;
		}
		while (j < n) {
			destinationUnion[len] = other.destinations[j];
			portions[len] = other.portions[j] * other.totalFlow / resultFlow;
			
			j++;
			len++;
		}

		return new MixtureFlow(resultFlow, destinationUnion, portions, len);
	}
	
//	public MixtureFlow plus(MixtureFlow other) {
//		double resultFlow = totalFlow + other.totalFlow;
//		var resultPortions = new IntDoubleHashMap();
//
//		var destinationUnion = new IntHashSet();
//		destinationUnion.addAll(this.mixtures.keys());
//		destinationUnion.addAll(other.mixtures.keys());
//
//		destinationUnion.forEach((IntProcedure) destination -> {
//			double thisDestinationFlow = this.getDestinationFlow(destination);
//			double otherDestinationFlow = other.getDestinationFlow(destination);
//
//			resultPortions.put(destination, (thisDestinationFlow + otherDestinationFlow) / resultFlow);
//		});
//
//		return new MixtureFlow(resultFlow, resultPortions);
//	}
	
	public MixtureFlow copyWithFlow(double newFlow) {
		if (newFlow == 0)
			return MixtureFlow.ZERO;
		
		return new MixtureFlow(newFlow, destinations, portions, destinations.length);
	}
	
	public void checkPortions(double tolerance) {
		double sum = 0;
		for (double portion : portions)
			sum += portion;
		
		if (sum != 0 && Math.abs(sum - 1) > tolerance)
			System.out.println("Portions do not sum to 1. Sum: " + sum);
	}
}
