package black0ut1.dynamic.loading.node;

import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;

import java.util.Vector;

/**
 * Basic node model representing more links merging into one:
 * ---v
 * -->O-->
 * ---^
 * It has multiple incoming links and one outgoing link.
 * <p>
 * Bibliography:
 * - Original merge for 2 links: (Daganzo, 1995a)
 * - Use of sending flows for priorities: (Jin and Zhang, 2003), don't
 *   use as it violates something
 * - Use of capacities to determine priorities and generalization for
 *   more than 2 link: (Ni and Leonard, 2005)
 * - (Boyles et al., 2025) Transportation Network Analysis, Section
 *    9.2.2
 */
public class DaganzoMerge extends Intersection {

	/**
	 * Priorites of incoming links, each must be from interval (0, 1)
	 * and they must sum up to 1. By default they are proportional to
	 * capacities. Must be same length as incomingLinks.
	 */
	protected final double[] priorities;

	public DaganzoMerge(int index, Link[] incomingLinks, Link outgoingLink, double[] priorities) {
		super(index, incomingLinks, new Link[]{outgoingLink});
		this.priorities = priorities;
	}

	public DaganzoMerge(int index, Link[] incomingLinks, Link outgoingLink) {
		this(index, incomingLinks, outgoingLink, capacitiesToPriorities(incomingLinks));
	}

	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeOrientedMixtureFlows(int time) {
		Link outgoingLink = outgoingLinks[0];
		double R = outgoingLink.getReceivingFlow();

		double totalS = 0;
		for (Link incomingLink : incomingLinks)
			totalS += incomingLink.getSendingFlow();

		if (totalS <= R) { // all vehicles can pass

			MixtureFlow outgoingMixtureFlow = MixtureFlow.ZERO;
			
			MixtureFlow[] incomingMixtureFlows = new MixtureFlow[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++) {
				double S = incomingLinks[i].getSendingFlow();
				
				MixtureFlow of = incomingLinks[i].getOutgoingMixtureFlow(time);
				incomingMixtureFlows[i] = of.copyWithFlow(S);
				
				outgoingMixtureFlow = outgoingMixtureFlow.plus(incomingMixtureFlows[i]);
			}
			
			MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[] {outgoingMixtureFlow};
			return new Pair<>(incomingMixtureFlows, outgoingMixtureFlows);

		} else { // outgoing link is congested

			double remainingR = R;
			double[] remainingS = new double[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++)
				remainingS[i] = incomingLinks[i].getSendingFlow();

			double[] transitionFlows = new double[incomingLinks.length];


			Vector<Integer> activeIncomingLinks = new Vector<>();
			for (int i = 0; i < incomingLinks.length; i++)
				activeIncomingLinks.add(i);

			while (!activeIncomingLinks.isEmpty()) {

				double[] alpha = new double[incomingLinks.length];
				double totalAlpha = 0;
				for (int i : activeIncomingLinks) {
					alpha[i] = priorities[i];
					totalAlpha += priorities[i];
				}

				double theta = remainingR / totalAlpha;
				for (int i : activeIncomingLinks)
					theta = Math.min(theta, remainingS[i] / alpha[i]);

				for (int i : activeIncomingLinks) {
					transitionFlows[i] += theta * alpha[i];
					remainingS[i] -= theta * alpha[i];
					remainingR -= theta * alpha[i];
				}

				if (remainingR == 0)
					break;

				activeIncomingLinks.removeIf(i -> remainingS[i] == 0);
			}

			
			MixtureFlow outgoingMixtureFlow = MixtureFlow.ZERO;
			
			MixtureFlow[] incomingMixtureFlows = new MixtureFlow[incomingLinks.length];
			for (int i = 0; i < incomingLinks.length; i++) {
				MixtureFlow of = incomingLinks[i].getOutgoingMixtureFlow(time);
				incomingMixtureFlows[i] = of.copyWithFlow(transitionFlows[i]);
				
				outgoingMixtureFlow = outgoingMixtureFlow.plus(incomingMixtureFlows[i]);
			}
			
			MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[] {outgoingMixtureFlow};
			return new Pair<>(incomingMixtureFlows, outgoingMixtureFlows);
		}
	}

	private static double[] capacitiesToPriorities(Link[] incomingLinks) {
		double sum = 0;
		for (Link link : incomingLinks)
			sum += link.capacity;

		double[] priorities = new double[incomingLinks.length];
		for (int i = 0; i < priorities.length; i++)
			priorities[i] = incomingLinks[i].capacity / sum;

		return priorities;
	}
}
