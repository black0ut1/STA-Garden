package black0ut1.dynamic.loading.routing;

/**
 * This class represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 * TODO thoroughly document, implementation is fast but opaque
 * TODO value class candidate
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
	private final int[] destinations;
	
	private final double[] portions;
	
	/** Using this reduces the number of objects allocated. */
	public static final MixtureFlow ZERO = new MixtureFlow(0, new int[0], new double[0], 0);
	
	public MixtureFlow(double totalFlow, int[] destinations, double[] portions, int len) {
		this.totalFlow = totalFlow;
		
		this.destinations = new int[len];
		this.portions = new double[len];
		System.arraycopy(destinations, 0, this.destinations, 0, len);
		System.arraycopy(portions, 0, this.portions, 0, len);
	}
	
	private MixtureFlow(double totalFlow, int[] destinations, double[] portions) {
		this.totalFlow = totalFlow;
		this.destinations = destinations;
		this.portions = portions;
	}
	
	public int getDestination(int i) {
		return destinations[i];
	}
	
	public double getPortion(int i) {
		return portions[i];
	}
	
	public int size() {
		return destinations.length;
	}
	
	public MixtureFlow plus(MixtureFlow other) {
		if (this == ZERO)
			return other;
		if (other == ZERO)
			return this;
		
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
	
	public MixtureFlow copyWithFlow(double newFlow) {
		if (newFlow == 0)
			return MixtureFlow.ZERO;
		
		return new MixtureFlow(newFlow, destinations, portions);
	}
	
	public void rectify() {
		if (portions.length == 0)
			return;
		
		double sum = 0;
		double maxPortion = Double.NEGATIVE_INFINITY;
		int maxPortionIndex = -1;
		
		for (int i = 0; i < portions.length; i++) {
			sum += portions[i];
			
			if (portions[i] > maxPortion) {
				maxPortion = portions[i];
				maxPortionIndex = i;
			}
		}
		
		double error = 1 - sum;
		portions[maxPortionIndex] += error;
	}
}
