package black0ut1.dynamic.loading.node;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.mixture.MixtureFlow;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.link.Link;

/**
 * Intersection is a specialization of {@code Node} class for nodes
 * that have more than one outgoing link and thus need turning
 * fractions to determine where should incoming flows turn.
 */
public abstract class RoutedIntersection extends Intersection {
	
	protected MixtureFractions[] turningFractions;
	protected DoubleMatrix[] totalTurningFractions;
	public double potential;
	
	public RoutedIntersection(int index, Link[] incomingLinks, Link[] outgoingLinks) {
		super(index, incomingLinks, outgoingLinks);
	}
	
	public void setTurningFractions(MixtureFractions[] turningFractions) {
		this.turningFractions = turningFractions;
		this.totalTurningFractions =  new DoubleMatrix[turningFractions.length];
	}
	
	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeOrientedMixtureFlows(int time) {
		MixtureFractions fractions = turningFractions[time];
		
		// 1. Compute total turning fractions
		totalTurningFractions[time] = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		for (int i = 0; i < incomingLinks.length; i++) {
			
			// TODO cache this vv and switch loops
			var mixture = incomingLinks[i].getOutgoingMixtureFlow(time);
			for (int j = 0; j < outgoingLinks.length; j++) {
				
				for (int d = 0; d < mixture.destinations.length; d++) {
					int destination = mixture.destinations[d];
					double portion = mixture.portions[d];
					
					DoubleMatrix destinationFractions = fractions.getDestinationFractions(destination);
					totalTurningFractions[time].set(i, j,
							totalTurningFractions[time].get(i, j) + portion * destinationFractions.get(i, j));
				}
			}
		}
		
		// 2. Execute the specific node model
		DoubleMatrix orientedFlows = computeOrientedFlows(totalTurningFractions[time]);
		
		
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
		
		// 4. Compute the mixture flows
		// 4.1. Compute the flow exiting from incoming links
		MixtureFlow[] incomingMixtureFlows = new MixtureFlow[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			MixtureFlow of = incomingLinks[i].getOutgoingMixtureFlow(time);
			incomingMixtureFlows[i] = of.copyWithFlow(incomingFlows[i]);
		}
		
		// 4.2. Enter flows to outgoing links
		MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++) {
			if (outgoingFlows[j] <= 0) {
				outgoingMixtureFlows[j] = MixtureFlow.ZERO;
				continue;
			}
			
			int len = 0;
			int[] destinations = new int[fractions.destinations.length];
			double[] portions = new double[fractions.destinations.length];
			
			for (int d = 0; d < fractions.destinations.length; d++) {
				int destination = fractions.destinations[d];
				DoubleMatrix destinationFractions = fractions.destinationTurningFractions[d];
				
				double sum = 0;
				for (int i = 0; i < incomingLinks.length; i++)
					sum += incomingMixtureFlows[i].getDestinationFlow(destination) * destinationFractions.get(i, j);
				
				if (sum > 0) {
					destinations[len] = destination;
					portions[len] = sum / outgoingFlows[j];
					len++;
				}
			}
			
			MixtureFlow a = new MixtureFlow(outgoingFlows[j], destinations, portions, len);
			a.checkPortions(1e-4); // TODO remove
			outgoingMixtureFlows[j] = a;
		}
		
		return new Pair<>(incomingMixtureFlows, outgoingMixtureFlows);
	}
	
	protected abstract DoubleMatrix computeOrientedFlows(DoubleMatrix totalTurningFractions);
	
	public MixtureFractions[] getTurningFractions() {
		return turningFractions;
	}
	
	public DoubleMatrix[] getTotalTurningFractions() {
		return totalTurningFractions;
	}
}
