package black0ut1.dynamic.equilibrium;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.mixture.MixtureFractions;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.util.SSSP;

import java.util.Arrays;

public class StaticAONRouteChoice implements StaticRouteChoice {
	
	protected final Network network;
	protected final DynamicNetwork dNetwork;
	protected final DoubleMatrix odMatrix;
	protected final int timeSteps;
	
	public StaticAONRouteChoice(Network network, DynamicNetwork dNetwork, DoubleMatrix odMatrix, int timeSteps) {
		this.network = network;
		this.odMatrix = odMatrix;
		this.dNetwork = dNetwork;
		this.timeSteps = timeSteps;
	}
	
	public MixtureFractions[][] computeInitialMixtureFractions() {
		MixtureFractions[][] result = new MixtureFractions[network.nodes][timeSteps];
		
		double[][] destinationFlows = assignFlows();
		
		// create first mixture fraction for each node
		for (int node = 0; node < network.nodes; node++)
			result[node][0] = createNodeFractions(destinationFlows, node);
		
		// copy the first mixture fraction to each time step
		for (int i = 0; i < network.nodes; i++)
			Arrays.fill(result[i], result[i][0]);
		
		return result;
	}
	
	protected MixtureFractions createNodeFractions(double[][] destinationFlows, int node1) {
		DoubleMatrix[] turningFractions = new DoubleMatrix[dNetwork.destinations.length];
		
		Intersection node = dNetwork.routedIntersections[node1];
		
		// compute fractions for a destination
		for (int destination = 0; destination < network.zones; destination++) {
			DoubleMatrix destinationFractions = new DoubleMatrix(node.incomingLinks.length, node.outgoingLinks.length);
			
			// the amount of flow going through the node
			double nodeFlow = 0;
			
			// the next link in tree (the only link with outgoing flow)
			int J = -1;
			
			if (node1 == destination) {
				
				// if node is the destination, next link is the
				// destination connector (which is first outgoing)
				J = 0;
				for (int i = 1; i < node.incomingLinks.length; i++) {
					int index = node.incomingLinks[i].index;
					nodeFlow += destinationFlows[destination][index];
				}
				
			} else {
				
				for (int j = 0; j < node.outgoingLinks.length; j++) {
					if (node1 < network.zones && j == 0)
						continue; // skip destination connector
					
					int index = node.outgoingLinks[j].index;
					if (destinationFlows[destination][index] > 0) {
						J = j;
						nodeFlow = destinationFlows[destination][index];
						break;
					}
				}
			}
			
			// Destination flow do not use this intersection -> fractions will be
			// uniformly distributed
			if (nodeFlow == 0) {
				
				for (int i = 0; i < node.incomingLinks.length; i++)
					for (int j = 0; j < node.outgoingLinks.length; j++)
						destinationFractions.set(i, j, 1.0 / node.outgoingLinks.length);
				
			} // all flow from each incoming link is going into J
			else {
				for (int i = 0; i < node.incomingLinks.length; i++)
					destinationFractions.set(i, J, 1);
			}
			
			turningFractions[destination] = destinationFractions;
		}
		
		return new MixtureFractions(turningFractions);
	}
	
	protected double[][] assignFlows() {
		double[][] destinationFlows = new double[network.zones][network.edges];
		
		double[] costs = new double[network.edges];
		for (int i = 0; i < network.edges; i++)
			costs[i] = network.getEdges()[i].freeFlow;
		
		for (int destination = 0; destination < network.zones; destination++) {
			Network.Edge[] next = SSSP.dijkstraDest(network, destination, costs).first();
			
			for (int origin = 0; origin < network.zones; origin++) {
				double demand = odMatrix.get(origin, destination);
				if (demand == 0)
					continue;
				
				for (Network.Edge link = next[origin];
					 link != null;
					 link = next[link.head]) {
					destinationFlows[destination][link.index] += demand;
				}
			}
		}
		
		return destinationFlows;
	}
}
