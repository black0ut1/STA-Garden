package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.node.Node;

import java.util.Arrays;

/**
 * Base class for dynamic link models. Link models mainly differ by
 * their computation of receiving and sending flow.
 */
public abstract class Link {
	
	public final int index;
	public Node head;
	public Node tail;
	
	///// Fundamental diagram parameters //////
	
	/** Length of the link [km] */
	public final double length;
	/** Capacity/maximum possible flow of the link [veh/h] */
	public final double capacity;
	/** Jam density [veh/km] - the density of vehicles, after which
	 *  flow falls to 0 */
	public final double jamDensity;
	/** The speed of free flow on the link [km/h] */
	public final double freeFlowSpeed;
	/** The backward wave speed [km/h] (must be positive) */
	public final double backwardWaveSpeed;
	
	///// Flow variables //////
	
	/** The receiving flow (supply) of this link. */
	protected double receivingFlow;
	/** The sending flow (demand) of this link. */
	protected double sendingFlow;
	/** The flow that entered this link at each time step. */
	public final MixtureFlow[] inflow;
	/** The flow that exited this link at each time step. */
	public final MixtureFlow[] outflow;
	/** How many vehicles passed the upstream end up until now. */
	protected final double[] cumulativeInflow;
	/** How many vehicles passed the downstream end up until now. */
	protected final double[] cumulativeOutflow;
	
	public Link(int index, int timeSteps, double length, double capacity,
				double jamDensity, double freeFlowSpeed, double backwardWaveSpeed) {
		this.index = index;
		
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
	 * Computes the receiving (supply) and sending (demand) flow for
	 * this time step. This method is implemented by the specific link
	 * model.
	 * @param time Current time step.
	 */
	public abstract void computeReceivingAndSendingFlows(int time);
	
	public double getReceivingFlow() {
		return receivingFlow;
	}
	
	public double getSendingFlow() {
		return sendingFlow;
	}
	
	public MixtureFlow getOutgoingMixtureFlow(int time) {
		// TODO do not floor, interpolate, also precompute
		// get actual outgoing cumulative flow [veh]
		double cumOut = cumulativeOutflow[time];
		
		MixtureFlow outMixture = null;
		// find the time when the cumulative flows are equal
		for (int t = time; t >= 0; t--)
			if (cumulativeInflow[t] <= cumOut) {
				outMixture = inflow[t];
				break;
			}
		
		// this should not occur in theory (outflow cum is larger than inflow cum), because numerical problems it can happen
		// as fallback take the latest mixture
		if (outMixture == null) {
			if (time == 0)
				return MixtureFlow.ZERO;
			
			outMixture = inflow[time - 1];
		}
		
		return outMixture;
	}
	
	/**
	 * This method is used to update flow variables when a flow enters
	 * this link.
	 * @param time Current time step.
	 * @param flow The mixture flow entering this link.
	 */
	public void enterFlow(int time, MixtureFlow flow) {
		inflow[time] = flow;
		cumulativeInflow[time + 1] = cumulativeInflow[time] + flow.totalFlow;
	}
	
	/**
	 * This method is used to update flow variables when a flow exits
	 * this link. It returns mixture flow exiting this link.
	 * @param time Current time step.
	 * @param flow The amount of flow exiting this link.
	 * @return Mixture flow with {@code totalFlow = flow} and
	 * according mixtures.
	 */
	public MixtureFlow exitFlow(int time, double flow) {
		MixtureFlow of = getOutgoingMixtureFlow(time);
		var mf = of.copyWithFlow(flow);
		
		outflow[time] = mf;
		cumulativeOutflow[time + 1] = cumulativeOutflow[time] + flow;
		
		return mf;
	}
	
	/**
	 * Resets the link to its original state making it ready to be
	 * used again for DNL.
	 */
	public void reset() {
		// release objects
		Arrays.fill(inflow, null);
		Arrays.fill(outflow, null);
		
		cumulativeInflow[0] = 0;
		cumulativeOutflow[0] = 0;
	}
}
