package black0ut1.dynamic.loading.routing;

import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.node.routing.RoutedIntersection;

public class MixtureOutgoingFractions {
	
	protected final double[] values;
	protected final int[] offsets;
	protected final DynamicNetwork network;
	
	public final int intersections;
	public final int destinations;
	public final int timeSteps;
	
	public MixtureOutgoingFractions(DynamicNetwork network, int timeSteps) {
		int outdegreeSum = 0;
		for (RoutedIntersection routedIntersection : network.routedIntersections)
			outdegreeSum += routedIntersection.outgoingLinks.length;
		
		this.values = new double[outdegreeSum * timeSteps * network.zones.length];
		this.offsets = new int[network.routedIntersections.length];
		for (int i = 1; i < network.routedIntersections.length; i++) {
			this.offsets[i] = this.offsets[i - 1] + timeSteps * network.zones.length *
					network.routedIntersections[i - 1].outgoingLinks.length;
		}
		this.network = network;
		
		this.intersections = network.routedIntersections.length;
		this.destinations = network.zones.length;
		this.timeSteps = timeSteps;
	}
	
	public double getFraction(int n, int t, int d, int j) {
		int J = network.routedIntersections[n].outgoingLinks.length;
		return values[offsets[n] + t * destinations * J + d * J + j];
	}
	
	public void setFraction(int n, int t, int d, int j, double val) {
		int J = network.routedIntersections[n].outgoingLinks.length;
		values[offsets[n] + t * destinations * J + d * J + j] = val;
	}
	
	public class Indices {
		
		protected final byte[] values = new byte[intersections * destinations * timeSteps];
		
		public byte getIndex(int n, int t, int d) {
			return values[n * timeSteps * destinations + t * destinations + d];
		}
		
		public void setIndex(int n, int t, int d, byte j) {
			values[n * timeSteps * destinations + t * destinations + d] = j;
		}
	}
}
