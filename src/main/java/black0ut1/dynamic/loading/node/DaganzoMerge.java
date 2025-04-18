//package black0ut1.dynamic.loading.node;
//
//import black0ut1.dynamic.loading.mixture.ArrayMixtureFlow;
//import black0ut1.dynamic.loading.mixture.MapMixtureFlow;
//import black0ut1.dynamic.loading.link.Link;
//import black0ut1.dynamic.loading.mixture.MixtureFlow;
//
//import java.util.Vector;
//
///**
// * Basic node model representing more links merging into one:
// * ---v
// * -->O-->
// * ---^
// * It has multiple incoming links and one outgoing link.
// * Bibliography:
// * - Original merge for 2 links: (Daganzo, 1995a)
// * - Use of sending flows for priorities: (Jin and Zhang, 2003)
// * - Use of capacities to determine priorities and generalization for
// *   more than 2 link: (Ni and Leonard, 2005)
// * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.2.2
// */
//public class DaganzoMerge extends Node {
//
//	/**
//	 * Priorites of incoming links, each must be from interval (0, 1)
//	 * and they must sum up to 1. By default they are proportional to
//	 * capacities. Must be same length as incomingLinks.
//	 */
//	protected final double[] priorities;
//
//	public DaganzoMerge(int index, Link[] incomingLinks, Link outgoingLink, double[] priorities) {
//		super(index, incomingLinks, new Link[]{outgoingLink});
//		this.priorities = priorities;
//	}
//
//	public DaganzoMerge(int index, Link[] incomingLinks, Link outgoingLink) {
//		this(index, incomingLinks, outgoingLink, capacitiesToPriorities(incomingLinks));
//	}
//
//	@Override
//	public void shiftOrientedMixtureFlows(int time) {
//		Link outgoingLink = outgoingLinks[0];
//		double R = outgoingLink.getReceivingFlow();
//
//		double totalS = 0;
//		for (Link incomingLink : incomingLinks)
//			totalS += incomingLink.getSendingFlow();
//
//		if (totalS <= R) { // all vehicles can pass
//
//			MixtureFlow totalExited = new ArrayMixtureFlow();
//			for (Link incomingLink : incomingLinks) {
//				double S = incomingLink.getSendingFlow();
//
//				MixtureFlow exited = incomingLink.exitFlow(time, S);
//				totalExited = exited.plus(totalExited);
//			}
//
//			outgoingLink.enterFlow(time, totalExited);
//
//		} else { // outgoing link is congested
//
//			double remainingR = R;
//			double[] remainingS = new double[incomingLinks.length];
//			for (int i = 0; i < incomingLinks.length; i++)
//				remainingS[i] = incomingLinks[i].getReceivingFlow();
//
//			double[] transitionFlows = new double[incomingLinks.length];
//
//
//			Vector<Integer> activeIncomingLinks = new Vector<>();
//			for (int i = 0; i < incomingLinks.length; i++)
//				activeIncomingLinks.add(i);
//
//			while (!activeIncomingLinks.isEmpty()) {
//
//				double[] alpha = new double[incomingLinks.length];
//				double totalAlpha = 0;
//				for (int i : activeIncomingLinks) {
//					alpha[i] = priorities[i];
//					totalAlpha += priorities[i];
//				}
//
//				double theta = remainingR / totalAlpha;
//				for (int i : activeIncomingLinks)
//					theta = Math.min(theta, remainingS[i] / alpha[i]);
//
//				for (int i : activeIncomingLinks) {
//					transitionFlows[i] += theta * alpha[i];
//					remainingS[i] -= theta * alpha[i];
//					remainingR -= theta * alpha[i];
//				}
//
//				if (remainingR == 0)
//					break;
//
//				activeIncomingLinks.removeIf(i -> remainingS[i] == 0);
//			}
//
//			MapMixtureFlow totalExited = new MapMixtureFlow();
//			for (int i = 0; i < incomingLinks.length; i++) {
//				MapMixtureFlow exited = incomingLinks[i].exitFlow(time, transitionFlows[i]);
//				totalExited = exited.plus(totalExited);
//			}
//
//			outgoingLink.enterFlow(time, totalExited);
//		}
//	}
//
//	private static double[] capacitiesToPriorities(Link[] incomingLinks) {
//		double sum = 0;
//		for (Link link : incomingLinks)
//			sum += link.capacity;
//
//		double[] priorities = new double[incomingLinks.length];
//		for (int i = 0; i < priorities.length; i++)
//			priorities[i] = incomingLinks[i].capacity / sum;
//
//		return priorities;
//	}
//}
