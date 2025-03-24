package black0ut1.dynamic.loading.link;

import black0ut1.dynamic.loading.Clock;
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
	
	protected final Clock clock;
	
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
	/** The backward wave speed [km/h] (is not negative) */
	public final double backwardWaveSpeed;
	
	///// Flow variables //////
	
	/** The receiving flow of this link - see definition */
	protected double receivingFlow;
	/** The sending flow of this link - see definition */
	protected double sendingFlow;
	/** The flow that entered this link at each time step. */
	protected final Vector<MixtureFlow> inflow = new Vector<>();
	/** The flow that exited this link at each time step. */
	protected final Vector<MixtureFlow> outflow = new Vector<>();
	/** How many vehicles passed the upstream end up until now. */
	protected final Vector<Double> cumulativeUpstreamCount = new Vector<>();
	/** How many vehicles passed the downstream end up until now. */
	protected final Vector<Double> cumulativeDownstreamCount = new Vector<>();
	/** Queue of mixture flows that currently reside on this link. */
	protected final Deque<MixtureFlow> mixtureFlowQueue = new ArrayDeque<>();
	
	public Link(int index, Clock clock, double length,
				double capacity, double jamDensity,
				double freeFlowSpeed, double backwardWaveSpeed) {
		this.index = index;
		
		this.length = length;
		this.capacity = capacity;
		this.jamDensity = jamDensity;
		this.freeFlowSpeed = freeFlowSpeed;
		// if backward speed is not specified, it is computed so it
		// creates triangular fundamental diagram
		this.backwardWaveSpeed = (backwardWaveSpeed != 0)
				? backwardWaveSpeed
				: (capacity * freeFlowSpeed) / (capacity - freeFlowSpeed * jamDensity);
		
		this.clock = clock;
		
		cumulativeUpstreamCount.add(0.0);
		cumulativeDownstreamCount.add(0.0);
		mixtureFlowQueue.addLast(new MixtureFlow(0, new HashMap<>()));
	}
	
	public abstract void computeReceivingAndSendingFlows();
	
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
				cumulativeDownstreamCount.getLast() + flow
		);
		
		return exitingMf;
	}
}
