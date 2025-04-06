package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.node.Node;

import java.util.HashMap;
import java.util.Vector;

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
	public final Vector<MixtureFlow> inflow = new Vector<>();
	/** The flow that exited this link at each time step. */
	public final Vector<MixtureFlow> outflow = new Vector<>();
	/** How many vehicles passed the upstream end up until now. */
	protected final Vector<Double> cumulativeUpstreamCount = new Vector<>();
	/** How many vehicles passed the downstream end up until now. */
	protected final Vector<Double> cumulativeDownstreamCount = new Vector<>();

	protected final Vector<MixtureFlow> inflowMixture = new Vector<>();
	protected final Vector<MixtureFlow> outflowMixture = new Vector<>();
	
	public Link(int index, double length, double capacity, double jamDensity,
				double freeFlowSpeed, double backwardWaveSpeed) {
		this.index = index;
		
		this.length = length;
		this.capacity = capacity;
		this.freeFlowSpeed = freeFlowSpeed;
		
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
		
		cumulativeUpstreamCount.add(0.0);
		cumulativeDownstreamCount.add(0.0);
		inflowMixture.addLast(new MixtureFlow(0, new HashMap<>()));
	}
	
	public abstract void computeReceivingAndSendingFlows(int time);
	
	public double getReceivingFlow() {
		return receivingFlow;
	}
	
	public double getSendingFlow() {
		return sendingFlow;
	}
	
	public MixtureFlow getOutgoingMixtureFlow() {
		// get actual outgoing cumulative flow [veh]
		Double cumOut = cumulativeDownstreamCount.getLast();
		MixtureFlow outMixture = null;
		// find the time when the cumulative flows are equal
		for (var time = 0; time < cumulativeUpstreamCount.size() - 1; time++){
			if (cumulativeUpstreamCount.get(time) <= cumOut && cumOut < cumulativeUpstreamCount.get(time + 1)){
				// take mixture of that time
				// for now take the lower (time) -> we should implement interpolation between time and time + 1
				outMixture = inflowMixture.get(time);
			}
		}
		// this should not occur in theory (outflow cum is larger than inflow cum), because numerical problems it can happen
		// as fallback take the latest mixture
		if (outMixture == null){
			outMixture = inflowMixture.getLast();
		}
		return outMixture;
	}
	
	public void enterFlow(MixtureFlow flow) {
		inflowMixture.addLast(flow);
		inflow.add(flow);
		cumulativeUpstreamCount.add(
				cumulativeUpstreamCount.getLast() + flow.totalFlow()
		);
	}

	// TODO tímhle si fakt nejsem jist ale asi takto -> jen změnit total flow
	public MixtureFlow exitFlow(double flow) {
		MixtureFlow of = getOutgoingMixtureFlow(); // or outflowMixture.getLast() if is already set ....
		return new MixtureFlow(flow, of.portions());
	}
	
	public void reset() {
		inflow.clear();
		outflow.clear();
		
		cumulativeUpstreamCount.clear();
		cumulativeDownstreamCount.clear();
		cumulativeUpstreamCount.add(0.0);
		cumulativeDownstreamCount.add(0.0);
		
		inflowMixture.clear();
		outflowMixture.clear();
		inflowMixture.addLast(new MixtureFlow(0, new HashMap<>()));
	}
}
