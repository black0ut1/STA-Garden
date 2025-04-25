package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.loading.link.Link;

/**
 * Model of intersection with basic signals. Basically a rotating
 * Daganzo diverge - during each phase, traffic from only one incoming
 * link is permitted.
 * <p>
 * One can imagine the phases on a clock. The total time of the clock
 * is the {@code cycleTime}. The clock is divided into sections and
 * each  has length of a value in {@code phaseTimes} - those are the
 * phases. {@code elapsedTime} represents the hand, it starts by
 * pointing upward and with each time step the hand moves
 * {@code stepSize} forward. The hand points to current phase on the
 * clock. Of course, there are pitfalls when {@code stepSize} is a
 * multiple of {@code cycle time} (or vice versa).
 */
public class BasicSignals extends Intersection {
	
	/** stepSize of the DNL. */
	protected final double stepSize;
	/** The time elapsed from the start of DNL (modulo'd by
	 * cycleTime). This way it points into the interval [0, cycleTime)
	 * to the current phase. */
	protected double elapsedTime;
	
	/** The index into incomingLinks of the only allowed incoming link
	 * for each phase. */
	protected final int[] phaseIncomingLinks;
	/** The lengths of time intervals of each phase in the cycle
	 * (cumulative for ease of use). */
	protected final double[] cumulativePhaseTimes;
	/** The time of the whole cycle - each phase has taken turn. */
	protected final double cycleTime;
	
	public BasicSignals(int index, Link[] incomingLinks, Link[] outgoingLinks,
						double stepSize, int[] phaseIncomingLinks, double[] phaseTimes) {
		super(index, incomingLinks, outgoingLinks);
		
		this.stepSize = stepSize;
		this.phaseIncomingLinks = phaseIncomingLinks;
		this.elapsedTime = 0;
		
		this.cumulativePhaseTimes = new double[phaseTimes.length];
		this.cumulativePhaseTimes[0] = phaseTimes[0];
		for (int i = 1; i < phaseTimes.length; i++)
			this.cumulativePhaseTimes[i] = cumulativePhaseTimes[i - 1] + phaseTimes[i];
		
		double cycleTime = 0;
		for (double phaseTime : phaseTimes)
			cycleTime += phaseTime;
		this.cycleTime = cycleTime;
	}
	
	@Override
	protected DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions) {
		DoubleMatrix orientedFlows = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		
		// 1. Determine current phase
		int currentPhase = 0;
		for (int i = 0; i < cumulativePhaseTimes.length; i++)
			if (elapsedTime < cumulativePhaseTimes[i]) {
				currentPhase = i;
				break;
			}
		
		int i = phaseIncomingLinks[currentPhase];
		double S = incomingLinks[i].getSendingFlow();
		
		// 2. Compute the portion of sending flow actually sent
		double theta = 1;
		for (int j = 0; j < outgoingLinks.length; j++) {
			
			double R = outgoingLinks[j].getReceivingFlow();
			theta = Math.min(theta, R / (S * totalTurningFractions.get(i, j)));
		}
		
		// 3. Compute outgoing flows
		for (int j = 0; j < outgoingLinks.length; j++) {
			double outgoingFlow = theta * S * totalTurningFractions.get(i, j);
			orientedFlows.set(i, j, outgoingFlow);
		}
		
		
		elapsedTime = (elapsedTime + stepSize) % cycleTime;
		return orientedFlows;
	}
}
