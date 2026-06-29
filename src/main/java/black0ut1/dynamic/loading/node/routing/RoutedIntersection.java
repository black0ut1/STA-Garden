package black0ut1.dynamic.loading.node.routing;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.routing.MixtureFlow;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.dynamic.loading.node.models.NodeModel;

/**
 * Intersection is a specialization of {@link Intersection} class for nodes that have more
 * than one outgoing link and thus need turning fractions to determine where should
 * incoming flows turn.
 */
public class RoutedIntersection extends Intersection {
	
	protected MixtureOutgoingFractions fractions;
	protected final NodeModel nodeModel;
	public double potential;
	
	public RoutedIntersection(int index, Link[] incomingLinks, Link[] outgoingLinks, NodeModel nodeModel) {
		super(index, incomingLinks, outgoingLinks);
		this.nodeModel = nodeModel;
	}
	
	public void setTurningFractions(MixtureOutgoingFractions turningFractions) {
		this.fractions = turningFractions;
	}
	
	@Override
	public Pair<MixtureFlow[], MixtureFlow[]> computeMixtureInflowsOutflows(int time) {
		
		double[] sendingFlows = new double[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			sendingFlows[i] = incomingLinks[i].getSendingFlow();
		double[] receivingFlows = new double[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++)
			receivingFlows[j] = outgoingLinks[j].getReceivingFlow();
		
		// 1. Compute total turning fractions
		DoubleMatrix totalTurningFractions = new DoubleMatrix(incomingLinks.length, outgoingLinks.length);
		MixtureFlow[] mixtureFlows = new MixtureFlow[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++) {
			mixtureFlows[i] = incomingLinks[i].getOutgoingMixtureFlow(time, sendingFlows[i]);
			
			for (int d = 0; d < mixtureFlows[i].destinations.length; d++) {
				int destination = mixtureFlows[i].destinations[d];
				double portion = mixtureFlows[i].portions[d];
				
				for (int j = 0; j < outgoingLinks.length; j++) {
					totalTurningFractions.set(i, j,
							totalTurningFractions.get(i, j) + portion * fractions.getFraction(time, destination, j));
				}
			}
		}
		
		// 2. Execute the specific node model
		var pair = nodeModel.computeTotalInflowsOutflows(totalTurningFractions, sendingFlows, receivingFlows);
		double[] inflows = pair.first();
		double[] outflows = pair.second();
		
		// 3. Compute the mixture flows
		// 3.1. Compute the flow exiting from incoming links
		MixtureFlow[] incomingMixtureFlows = new MixtureFlow[incomingLinks.length];
		for (int i = 0; i < incomingLinks.length; i++)
			incomingMixtureFlows[i] = mixtureFlows[i].copyWithFlow(inflows[i]);
		
		// 3.2. Enter flows to outgoing links
		MixtureFlow[] outgoingMixtureFlows = new MixtureFlow[outgoingLinks.length];
		for (int j = 0; j < outgoingLinks.length; j++) {
			if (outflows[j] <= 0) {
				outgoingMixtureFlows[j] = MixtureFlow.ZERO;
				continue;
			}
			
			int len = 0;
			int[] destinations = new int[fractions.destinations];
			double[] portions = new double[fractions.destinations];
			
			for (int d = 0; d < fractions.destinations; d++) {
				double sum = 0;
				for (int i = 0; i < incomingLinks.length; i++)
					sum += incomingMixtureFlows[i].getDestinationFlow(d) * fractions.getFraction(time, d, j);
				
				if (sum > 0) {
					destinations[len] = d;
					portions[len] = sum / outflows[j];
					len++;
				}
			}
			
			MixtureFlow a = new MixtureFlow(outflows[j], destinations, portions, len);
			outgoingMixtureFlows[j] = a;
		}
		
		return new Pair<>(incomingMixtureFlows, outgoingMixtureFlows);
	}
}
