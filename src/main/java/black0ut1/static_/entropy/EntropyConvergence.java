package black0ut1.static_.entropy;

import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Triplet;
import black0ut1.util.SSSP;

public class EntropyConvergence {
	
	public static double calculateEntropy(double[][] nodeFlows, Bush[] bushes, Network network) {
		double entropy = 0;
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			for (Network.Edge edge : network.getEdges()) {
				double x = bush.getEdgeFlow(edge.index);
				double n = nodeFlows[origin][edge.endNode];
				entropy += (x == 0) // 0 * ln(0) = 0
						? 0
						: x * Math.log(x / n);
			}
		}
		
		return -entropy;
	}
	
	// Bar-Gera, 2010 and Xie et Nie, 2019
	public static Triplet<Double, Integer, Double> computeConsistency(
			double[][] nodeFlows, Bush[] bushes, Network network, double[] costs, double flowEpsilon) {
		double sigma = Double.POSITIVE_INFINITY; // min reduced cost on links with zero origin flow
		double xi = Double.NEGATIVE_INFINITY; // max reduced cost on links with nonzero origin flow
		
		for (int origin = 0; origin < network.zones; origin++) {
			double[] minDist = SSSP.dijkstra(network, origin, costs).second();
			
			for (Network.Edge edge : network.getEdges()) {
				if (nodeFlows[origin][edge.endNode] <= flowEpsilon)
					continue;
				
				double reducedCost = minDist[edge.startNode] + costs[edge.index] - minDist[edge.endNode];
				if (bushes[origin].getEdgeFlow(edge.index) <= flowEpsilon) {
					sigma = Math.min(sigma, reducedCost);
				} else {
					xi = Math.max(xi, reducedCost);
				}
			}
		}
		
		int a = 0;
		for (int origin = 0; origin < network.zones; origin++) {
			double[] minDist = SSSP.dijkstra(network, origin, costs).second();
			
			for (Network.Edge edge : network.getEdges()) {
				if (nodeFlows[origin][edge.endNode] <= flowEpsilon)
					continue;
				
				double reducedCost = minDist[edge.startNode] + costs[edge.index] - minDist[edge.endNode];
				if (bushes[origin].getEdgeFlow(edge.index) <= flowEpsilon) {
					if (reducedCost <= xi)
						a++;
				}
			}
		}
		
		return new Triplet<>(sigma / xi, a, xi);
	}
}
