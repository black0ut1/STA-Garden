package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.node.Node;

import java.util.Arrays;

/**
 * Base class for dynamic link models. Link models mainly differ
 * by their computation of receiving and sending flow.
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
	
	/** The receiving flow of this link - see definition */
	protected double receivingFlow;
	/** The sending flow of this link - see definition */
	protected double sendingFlow;
	/** The flow that entered this link at each time step. */
	public final MixtureFlow[] inflow;
	/** The flow that exited this link at each time step. */
	public final MixtureFlow[] outflow;
	/** How many vehicles passed the upstream end up until now. */
	protected final double[] cumulativeUpstreamCount;
	/** How many vehicles passed the downstream end up until now. */
	protected final double[] cumulativeDownstreamCount;
	
	public Link(int index, int timeSteps, double length, double capacity,
				double jamDensity, double freeFlowSpeed, double backwardWaveSpeed) {
		this.index = index;
		
		this.length = length;
		this.capacity = capacity;
		this.freeFlowSpeed = freeFlowSpeed;
		
		this.inflow = new MixtureFlow[timeSteps];
		this.outflow = new MixtureFlow[timeSteps];
		this.cumulativeUpstreamCount = new double[timeSteps + 1];
		this.cumulativeDownstreamCount = new double[timeSteps + 1];
		
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
	
	public abstract void computeReceivingAndSendingFlows(int time);
	
	public double getReceivingFlow() {
		return receivingFlow;
	}
	
	public double getSendingFlow() {
		return sendingFlow;
	}
	
	public MixtureFlow getOutgoingMixtureFlow(int time) {
		// get actual outgoing cumulative flow [veh]
		double cumOut = cumulativeDownstreamCount[time];
		MixtureFlow outMixture = null;
		// find the time when the cumulative flows are equal
		for (var t = 0; t < time - 1; t++) {
			if (cumulativeUpstreamCount[t] <= cumOut && cumOut < cumulativeUpstreamCount[t + 1]) {
				// take mixture of that time
				// for now take the lower (time) -> we should implement interpolation between time and time + 1
				outMixture = inflow[t];
			}
		}
		// this should not occur in theory (outflow cum is larger than inflow cum), because numerical problems it can happen
		// as fallback take the latest mixture
		if (outMixture == null) {
			if (time == 0)
				return new MixtureFlow();
			
			outMixture = inflow[time - 1];
		}
		return outMixture;
	}
	
	public void enterFlow(int time, MixtureFlow flow) {
		inflow[time] = flow;
		cumulativeUpstreamCount[time + 1] = cumulativeUpstreamCount[time] + flow.totalFlow;
	}
	
	public MixtureFlow exitFlow(int time, double flow) {
		MixtureFlow of = getOutgoingMixtureFlow(time); // or outflowMixture.getLast() if is already set ....
		var mf = of.copyWithFlow(flow);
		
		outflow[time] = mf;
		cumulativeDownstreamCount[time + 1] = cumulativeDownstreamCount[time] + flow;
		
		return mf;
	}
	
	public void reset() {
		// release objects
		Arrays.fill(inflow, null);
		Arrays.fill(outflow, null);
		
		cumulativeUpstreamCount[0] = 0;
		cumulativeDownstreamCount[0] = 0;
	}
}
