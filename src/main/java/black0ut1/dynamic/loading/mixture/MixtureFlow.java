package black0ut1.dynamic.loading.mixture;

/**
 * This interface represents a flow in DNL. The flow cannot be only a
 * double, because we must track how much of the flow is going to each
 * destination.
 */
public abstract class MixtureFlow {
	
	/** The total flow, which consists of portions heading to
	 * different destinations. */
	public final double totalFlow;
	
	protected MixtureFlow(double totalFlow) {
		this.totalFlow = totalFlow;
	}
	
	/**
	 * Return the portion of the total flow that is heading for a
	 * specific destination.
	 * @param destination Destination index.
	 * @return totalFlow * destination portion
	 */
	public abstract double getDestinationFlow(int destination);
	
	public abstract MixtureFlow copyWithFlow(double newFlow);
	
	public abstract void forEach(Consumer consumer);
	
	public abstract void checkPortions(double tolerance);
	
	@FunctionalInterface
	public interface Consumer {
		void accept(int destination, double portion);
	}
	
	public static MixtureFlow ZERO = new MixtureFlow(0) {
		@Override
		public double getDestinationFlow(int destination) {
			return 0;
		}
		
		@Override
		public MixtureFlow copyWithFlow(double newFlow) {
			return this;
		}
		
		@Override
		public void forEach(Consumer consumer) {}
		
		@Override
		public void checkPortions(double tolerance) {}
	};
}
