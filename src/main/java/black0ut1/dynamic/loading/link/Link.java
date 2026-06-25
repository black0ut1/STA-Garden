package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.routing.MixtureFlow;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.util.Util;

import java.util.Arrays;

/**
 * Base class for dynamic link models. Link models mainly differ by
 * their computation of receiving and sending flow.
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
	
	public MixtureFlow getOutgoingMixtureFlow(int time, double flow) {
		if (time == 0)
			return MixtureFlow.ZERO;
		if (flow < 1e-8)
			return MixtureFlow.ZERO;
		
		// this method occurs in the instant before t-th time step, since it retrieves
		// the mixture of sending flow, which itself is computedin the same instant
		
		// This is the point at the very front of the link
		double outflow1 = cumulativeOutflow[time];
		// This is the point at the back of flow
		double outflow2 = cumulativeOutflow[time] + flow;
		// The time at which outflow2 entered the link: cumulativeInflow[t2] == outflow2
		// It is guaranteed that t1 < t2 and t1 == t2 only if flow == 0
		double t1 = -1;
		// The time at which outflow1 entered the link: cumulativeInflow[t1] == outflow1
		double t2 = -1;
		
		// 1. Find the time at which outflow2 entered the link
		int t = time;
		for (; t >= 0; t--) {
			
			if (Util.equals(cumulativeInflow[t], outflow2, 1e-10)) {
				
				// found time is integer (or close enough)
				t2 = t;
				break;
				
			} else if (cumulativeInflow[t] < outflow2) {
				
				// found time is not integer -> must interpolate
				double a = cumulativeInflow[t];
				double b = cumulativeInflow[t + 1];
				
				// t + p is the found time
				double p = (outflow2 - a) / (b - a);
				t2 = t + p;
				break;
			}
		}
		
		// 2. Find the time at which outflow1 entered the link
		for (; t >= 0; t--) {
			if (Util.equals(cumulativeInflow[t], outflow1, 1e-10)) {
				
				// found time is integer (or close enough)
				t1 = t;
				break;
				
			} else if (cumulativeInflow[t] < outflow1) {
				
				// found time is not integer -> must interpolate
				double a = cumulativeInflow[t];
				double b = cumulativeInflow[t + 1];
				
				// t + p is the found time
				double p = (outflow1 - a) / (b - a);
				t1 = t + p;
				break;
			}
		}
		
		// 3. Create the mixture flow of the `flow` amount of flow at the beggining of the
		// link by summing `inflow` over the time interval [t1, t2].
		int t1i = (int) t1;
		int t2i = (int) t2;
		
		if (t1i == t2i) // all flow arrived in the same time step
			return inflow[(int) t1].copyWithFlow(flow);
		
		double t1f = t1 - t1i;
		double t2f = t2 - t2i;
		
		MixtureFlow result = inflow[t1i].copyWithFlow((1 - t1f) * inflow[t1i].totalFlow);
		if (t2f != 0)
			result = result.plus(inflow[t2i].copyWithFlow(t2f * inflow[t2i].totalFlow));
		if (t1i + 1 < t2i)
			for (int i = t1i + 1; i < t2i; i++)
				result = result.plus(inflow[i]);
		
		return result;
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
