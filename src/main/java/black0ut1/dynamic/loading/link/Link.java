package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Intersection;

import java.util.Arrays;

/**
 * Base class for dynamic link models. Link models mainly differ by
 * their computation of receiving and sending flow.
 * <p>
 * Let the final number of steps = X (returned by {@link DynamicNetworkLoading#loadNetwork()}),
 * then arrays of flows on links are defined up to index X - 1 (higher values are null)
 * and arrays of cumulative flows are defined up to index X (higher values are zero).
 * Flows for indices >= X should be treated as 0 and cumulative flows for indices > X
 * should be treated as the value on index X.
 */
public abstract class Link {
	
	public final int index;
	public Intersection head;
	public Intersection tail;
	
	protected final double stepSize;
	
	///// Fundamental diagram parameters //////
	
	/** Length of the link [km]. */
	public final double length;
	/** Capacity/maximum possible flow of the link [veh/h]. */
	public final double capacity;
	/** Jam density [veh/km] - the density of vehicles, after which
	 * flow falls to 0. */
	public final double jamDensity;
	/** The speed of free flow on the link [km/h]. */
	public final double freeFlowSpeed;
	/** The backward wave speed [km/h] (must be positive). */
	public final double backwardWaveSpeed;
	
	///// Flow variables //////
	
	/** The receiving flow (supply) of this link. Should be treated as
	 * undefined until {@code computeReceivingFlow()} is called. */
	protected double receivingFlow;
	/** The sending flow (demand) of this link. Should be treated as
	 * undefined until {@code computeSendingFlow()} is called. */
	protected double sendingFlow;
	/** The flow that entered this link at each time step. */
	public final MixtureFlow[] inflow;
	/** The flow that exited this link at each time step. */
	public final MixtureFlow[] outflow;
	/** How many vehicles passed the upstream end up until now. */
	public final double[] cumulativeInflow;
	/** How many vehicles passed the downstream end up until now. */
	public final double[] cumulativeOutflow;
	
	public Link(int index, double stepSize, int timeSteps, double length,
				double capacity, double jamDensity, double freeFlowSpeed,
				double backwardWaveSpeed) {
		this.index = index;
		this.stepSize = stepSize;
		
		this.length = length;
		this.capacity = capacity;
		this.freeFlowSpeed = freeFlowSpeed;
		
		this.inflow = new MixtureFlow[timeSteps];
		this.outflow = new MixtureFlow[timeSteps];
		this.cumulativeInflow = new double[timeSteps + 1];
		this.cumulativeOutflow = new double[timeSteps + 1];
		
		// if backward speed is not specified, it is computed so it
		// creates triangular fundamental diagram using the formula:
		// -(q_max * u_f) / (q_max - u_f * k_j)
		this.backwardWaveSpeed = (backwardWaveSpeed != 0)
				? backwardWaveSpeed
				: -(capacity * freeFlowSpeed) / (capacity - freeFlowSpeed * jamDensity);
		
		// same with jam density, the formula is:
		// q_max * (w + u_f) / (w * u_f)
		this.jamDensity = (jamDensity != 0)
				? jamDensity
				: capacity * (backwardWaveSpeed + freeFlowSpeed) / (backwardWaveSpeed * freeFlowSpeed);
	}
	
	/**
	 * Computes the receiving flow (supply) for this time step. This
	 * method is implemented by the specific link model.
	 * @param time Current time step.
	 */
	public abstract void computeReceivingFlow(int time);
	
	/**
	 * Computes the sending flow (demand) for this time step. This
	 * method is implemented by the specific link model.
	 * @param time Current time step.
	 */
	public abstract void computeSendingFlow(int time);
	
	public double getReceivingFlow() {
		return receivingFlow;
	}
	
	public double getSendingFlow() {
		return sendingFlow;
	}
	
	public MixtureFlow getOutgoingMixtureFlow(int time) {
		if (time == 0)
			return MixtureFlow.ZERO;
		
		double currOutflow = cumulativeOutflow[time];
		
		// find the time when the cumulative flows are equal (the time
		// when currOutflow entered the link)
		MixtureFlow outMixture = null;
		for (int t = time; t >= 0; t--) {
			
			if (Math.abs(cumulativeInflow[t] - currOutflow) < 1e-8) {
				// found time is exactly an integer (or to combat
				// numerical problems, very close to integer)
				outMixture = inflow[t];
				break;
				
			} else if (cumulativeInflow[t] < currOutflow) {
				if (t == time - 1)
					return inflow[time - 1];
				
				// found time is not integer, must interpolate
				double a = cumulativeInflow[t];
				double b = cumulativeInflow[t + 1];
				
				// t + p is the found time
				double p = (currOutflow - a) / (b - a);
				
				MixtureFlow A = inflow[t];
				MixtureFlow B = inflow[t + 1];
				
				// A + p * (B - A) = p * B + (1 - p) * A
				outMixture = B.copyWithFlow(B.totalFlow * p)
						.plus(
								A.copyWithFlow(A.totalFlow * (1 - p))
						);
				break;
			}
		}
		
		
		// this should not occur in theory (outflow cum is larger than
		// inflow cum), because numerical problems it can happen as
		// fallback take the latest mixture
		if (outMixture == null)
			outMixture = inflow[time - 1];
		
		return outMixture;
	}
	
	/**
	 * Resets the link to its original state (as if it was just
	 * created). Nevertheless, the link is ready mostly  for another
	 * DNL anyway even if this method is not called.
	 */
	public void reset() {
		// release objects
		Arrays.fill(inflow, null);
		Arrays.fill(outflow, null);
		
		// zero out cumulative flows
		Arrays.fill(cumulativeInflow, 0);
		Arrays.fill(cumulativeOutflow, 0);
		
		this.sendingFlow = 0;
		this.receivingFlow = 0;
	}
}
