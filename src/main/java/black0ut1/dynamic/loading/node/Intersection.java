package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.link.Link;

/**
 * Intersection is a specialization of {@code Node} class for nodes
 * that have more than one outgoing link and thus need turning
 * fractions to determine where should incoming flows turn.
 */
public abstract class Intersection extends Node {
	
	protected MixtureFractions[] turningFractions;
	
	public Intersection(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	public void setTurningFractions(MixtureFractions[] turningFractions) {
		this.turningFractions = turningFractions;
	}
	
	public void shiftOrientedMixtureFlows(int time, int destinationsNum) {
		MixtureFractions fractions = turningFractions[time];
		
		// 1. Compute total turning fractions
		DoubleMatrix totalTurningFractions = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++) {
			
			var mixture = incomingLinks[i].getOutgoingMixtureFlow(time);
			for (int j = 0; j < outgoingLinks.length; j++) {
				
				for (int d = 0; d < mixture.destinations.length; d++) {
					int destination = mixture.destinations[d];
					double portion = mixture.portions[d];
					
					DoubleMatrix destinationFractions = fractions.getDestinationFractions(destination);
					totalTurningFractions.set(i, j,
							totalTurningFractions.get(i, j) + portion * destinationFractions.get(i, j));
				}
			}
		}
		
		// 2. Execute the specific node model
		DoubleMatrix orientedFlows = computeOrientedFlows(totalTurningFractions);
		
		
		// 3. Compute total incoming and outgoing flows
		// flows coming out of incoming link i (< sending flow)
		double[] incomingFlows = new double[incomingLinks.length];
		// flows going into outgoing link j (< receiving flow)
		double[] outgoingFlows = new double[outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++) {
				incomingFlows[i] += orientedFlows.get(i, j);
				outgoingFlows[j] += orientedFlows.get(i, j);
			}
		
		// 4. Compute the mixture flows and shift them accordingly
		// 4.1. Exit flow from incoming links
		MixtureFlow[] incomingMixtureFlows = new MixtureFlow[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			incomingMixtureFlows[i] = incomingLinks[i].exitFlow(time, incomingFlows[i]);
		
		// 4.2. Enter flows to outgoing links
		for (int j = 0; j < outgoingLinks.length; j++) {
			if (outgoingFlows[j] <= 0) {
				outgoingLinks[j].enterFlow(time, MixtureFlow.ZERO);
				continue;
			}
			
			final int[] len = {0};
			int[] destinations = new int[destinationsNum];
			double[] portions = new double[destinationsNum];
			
			for (int d = 0; d < fractions.destinations.length; d++) {
				int destination = fractions.destinations[d];
				DoubleMatrix destinationFractions = fractions.destinationTurningFractions[d];
				
				double sum = 0;
				for (int i = 0; i < incomingLinks.length; i++)
					sum += incomingMixtureFlows[i].getDestinationFlow(destination) * destinationFractions.get(i, j);
				
				if (sum > 0) {
					destinations[len[0]] = destination;
					portions[len[0]] = sum / outgoingFlows[j];
					len[0]++;
				}
			}
			
			MixtureFlow a = new MixtureFlow(outgoingFlows[j], destinations, portions, len[0]);
			a.checkPortions(1e-4); // TODO remove
			outgoingLinks[j].enterFlow(time, a);
		}
	}
	
	protected abstract DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions);
}
