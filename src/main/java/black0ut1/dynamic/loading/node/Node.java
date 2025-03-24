package black0ut1.dynamic.loading.node;

import black0ut1.dynamic.loading.MixtureFlow;
import black0ut1.dynamic.loading.MixtureFractions;
import black0ut1.dynamic.loading.link.Link;

import java.util.HashMap;

/**
 * Base class for dynamic node models.
 */
public abstract class Node {
	
	public final int index;
	public final Link[] incomingLinks;
	public final Link[] outgoingLinks;
	
	protected MixtureFractions[] turningFractions;
	
	public Node(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		this.index = index;
		this.incomingLinks = incomingLinks;
		this.outgoingLinks = outgoingLinks;
		
	}
	
	public void setTurningFractions(MixtureFractions[] turningFractions) {
		this.turningFractions = turningFractions;
	}
	
	public void shiftOrientedMixtureFlows(int timeStep) {
		MixtureFractions fractions = turningFractions[timeStep];
		
		// 1. Compute approximation of total turning fractions
		// only an approximation of total turning fractions, for exact
		// solution we would need to know the exact outgoing flow
		// portions (not just portions of the first outgoing
		// MixtureFlow), could reiterate for more precise solution
		double[][] totalTurningFractions = new double[incomingLinks.length][outgoingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			
			HashMap<Integer, Double> mixture = incomingLinks[i].getOutgoingMixtureFlow().portions();
			for (int j = 0; j < outgoingLinks.length; j++) {
				for (int destination : mixture.keySet()) {
					double[][] destinationFractions = fractions.getDestinationFractions(destination);
					totalTurningFractions[i][j] += mixture.get(destination) * destinationFractions[i][j];
				}
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
		for (int j = 0; j < incomingLinks.length; j++)
			incomingMixtureFlows[j] = incomingLinks[j].exitFlow(incomingFlows[j]);
		
		// 4.2. Enter flows to outgoing links
		for (int j = 0; j < outgoingLinks.length; j++) {
			if (outgoingFlows[j] <= 0) {
				outgoingLinks[j].enterFlow(new MixtureFlow(0, new HashMap<>()));
				continue;
			}
			
			HashMap<Integer, Double> proportions = new HashMap<>();
			
			for (int destination : fractions.destinationTurningFractions().keySet()) {
				double[][] destinationFractions = fractions.getDestinationFractions(destination);
				
				double sum = 0;
				for (int i = 0; i < incomingLinks.length; i++) {
					sum += incomingMixtureFlows[i].getDestinationFlow(destination)
							* destinationFractions[i][j];
				}
				
				proportions.put(destination, sum / outgoingFlows[j]);
			}
			
			outgoingLinks[j].enterFlow(new MixtureFlow(outgoingFlows[j], proportions));
		}
	}
	
	protected abstract double[][] computeOrientedFlows(double[][] totalTurningFractions);
}
