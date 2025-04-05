package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.node.Node;

import java.util.ArrayDeque;
import java.util.Deque;
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
	/** Queue of mixture flows that currently reside on this link. */
	protected final Deque<MixtureFlow> mixtureFlowQueue = new ArrayDeque<>();
	
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
		mixtureFlowQueue.addLast(new MixtureFlow(0, new HashMap<>()));
	}
	
	public abstract void computeReceivingAndSendingFlows(int time);
	
	public double getReceivingFlow() {
		return receivingFlow;
	}
	
	public double getSendingFlow() {
		return sendingFlow;
	}
	
	public MixtureFlow getOutgoingMixtureFlow() {
		MixtureFlow first = mixtureFlowQueue.removeFirst();
		if (first.totalFlow() == 0 && !mixtureFlowQueue.isEmpty()) {
			return mixtureFlowQueue.getFirst();
		}
		// TODO if queue empty return zero
		
		mixtureFlowQueue.addFirst(first);
		return mixtureFlowQueue.getFirst();
	}
	
	public void enterFlow(MixtureFlow flow) {
		mixtureFlowQueue.addLast(flow);
		inflow.add(flow);
		cumulativeUpstreamCount.add(
				cumulativeUpstreamCount.getLast() + flow.totalFlow()
		);
	}
	
	public MixtureFlow exitFlow(double flow) {
//		int t_now = clock.getCurrentStep();
//		double cumulativeOutNow = cumulativeDownstreamCount.get(t_now);
//
//		double entryTime;
//		for (int t = t_now; t >= 0; t--) {
//			double cumulativeIn = cumulativeUpstreamCount.get(t);
//
//			if (cumulativeIn == cumulativeOutNow) {
//				entryTime = t;
//				break;
//			} else if (cumulativeIn < cumulativeOutNow) {
//				double cumulativeIn2 = cumulativeUpstreamCount.get(t + 1);
//
//				double x = (cumulativeOutNow - cumulativeIn)
//						/ (cumulativeIn2 - cumulativeIn);
//				entryTime = t + x;
//			}
//		}
		
		MixtureFlow exitingMf = new MixtureFlow(0, new HashMap<>());
		
		while (flow > 0) {
			if (mixtureFlowQueue.isEmpty()) {
				if (flow > 1) // TODO remove
					System.out.println("Exiting flow but queue is empty: " + flow);
				break;
			}
			
			MixtureFlow mf = mixtureFlowQueue.removeFirst();
			
			if (mf.totalFlow() > flow) {
				var pair = mf.splitFlow(flow);
				mixtureFlowQueue.addFirst(pair.second());
				mf = pair.first();
			}
			
			exitingMf = exitingMf.plus(mf);
			flow -= mf.totalFlow();
		}
		
		outflow.add(exitingMf);
		cumulativeDownstreamCount.add(
				cumulativeDownstreamCount.getLast() + exitingMf.totalFlow()
		);
		
		return exitingMf;
	}
	
	public void reset() {
		inflow.clear();
		outflow.clear();
		
		cumulativeUpstreamCount.clear();
		cumulativeDownstreamCount.clear();
		cumulativeUpstreamCount.add(0.0);
		cumulativeDownstreamCount.add(0.0);
		
		mixtureFlowQueue.clear();
		mixtureFlowQueue.addLast(new MixtureFlow(0, new HashMap<>()));
	}
}
