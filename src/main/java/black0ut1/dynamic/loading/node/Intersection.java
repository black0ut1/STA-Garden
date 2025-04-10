package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.mixture.ArrayMixtureFlow;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.mixture.MixtureFractions;

public abstract class Intersection extends Node {
	
	protected MixtureFractions[] turningFractions;
	
	public Intersection(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	public void setTurningFractions(MixtureFractions[] turningFractions) {
		this.turningFractions = turningFractions;
	}
	
	public void shiftOrientedMixtureFlows(int time, int destinations) {
		MixtureFractions fractions = turningFractions[time];
		
		// 1. Compute approximation of total turning fractions
		// only an approximation of total turning fractions, for exact
		// solution we would need to know the exact outgoing flow
		// portions (not just portions of the first outgoing
		// MixtureFlow), could reiterate for more precise solution
		double[][] totalTurningFractions = new double[incomingLinks.length][outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			
			var mixture = incomingLinks[i].getOutgoingMixtureFlow(time);
			for (int j = 0; j < outgoingLinks.length; j++) {
				
				int finalI = i;
				int finalJ = j;
				mixture.forEach((destination, portion) -> {
					double[][] destinationFractions = fractions.getDestinationFractions(destination);
					totalTurningFractions[finalI][finalJ] += portion * destinationFractions[finalI][finalJ];
				});
			}
		}
		
		// 2. Execute the specific node model
		double[][] orientedFlows = computeOrientedFlows(totalTurningFractions);
		
		
		// 3. Compute total incoming and outgoing flows
		// flows coming out of incoming link i (< sending flow)
		double[] incomingFlows = new double[incomingLinks.length];
		// flows going into outgoing link j (< receiving flow)
		double[] outgoingFlows = new double[outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			for (int j = 0; j < outgoingLinks.length; j++) {
				incomingFlows[i] += orientedFlows[i][j];
				outgoingFlows[j] += orientedFlows[i][j];
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
			
			var mixtures = new double[destinations];
			
			int finalJ = j;
			fractions.forEach((destination, destinationFractions) -> {
				
				double sum = 0;
				for (int i = 0; i < incomingLinks.length; i++)
					sum += incomingMixtureFlows[i].getDestinationFlow(destination) * destinationFractions[i][finalJ];
				
				mixtures[destination] = sum / outgoingFlows[finalJ];
			});
			
			MixtureFlow a = new ArrayMixtureFlow(outgoingFlows[j], mixtures);
			a.checkPortions(1e-4); // TODO remove
			outgoingLinks[j].enterFlow(time, a);
		}
	}
	
	protected abstract double[][] computeOrientedFlows(double[][] totalTurningFractions);
}
