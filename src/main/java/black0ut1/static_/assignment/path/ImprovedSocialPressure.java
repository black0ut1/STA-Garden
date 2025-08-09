package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

public class ImprovedSocialPressure extends ProjectedGradient {
	
	protected static final double DELTA = 0.15;
	
	public ImprovedSocialPressure(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
								  int maxIterations, Convergence.Builder convergenceBuilder,
								  ShortestPathStrategy shortestPathStrategy) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder, shortestPathStrategy);
	}
	
	@Override
	protected double[] calculateStepDirection(int origin, int destination, Path basicPath) {
		var paths = odPairs.get(origin, destination);
		
		double[] costs =  new double[paths.size()];
		double maxPathCost = Double.NEGATIVE_INFINITY, minPathCost = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < paths.size(); i++) {
			costs[i] = paths.get(i).getCost(this.costs);
			
			if (costs[i] > maxPathCost)
				maxPathCost = costs[i];
			if (costs[i] < minPathCost)
				minPathCost = costs[i];
		}
		
		double pi = minPathCost + DELTA * (maxPathCost - minPathCost);
		
		
		double[] stepDirection = new double[paths.size()];
		
		// Compute direction for paths with cost <= pi
		double sum = 0;
		for (int i = 0; i < paths.size(); i++) {
			if (costs[i] <= pi) {
				stepDirection[i] = minPathCost - costs[i];
				sum += stepDirection[i];
			}
		}
		
		// Compute direction for paths with cost > pi
		double[] costDerivatives = new  double[paths.size()];
		double invSum = 0;
		for (int i = 0; i < paths.size(); i++) {
			
			if (costs[i] > pi) {
				for (int edgeIndex : paths.get(i).edges) {
					Network.Edge edge = network.getEdges()[i];
					costDerivatives[i] += costFunction.derivative(edge, flows[edgeIndex]);
				}
				invSum += 1 / costDerivatives[i];
			}
		}
		
		for (int i = 0; i < paths.size(); i++) {
			if (costs[i] > pi) {
				stepDirection[i] = -sum / (costDerivatives[i] * invSum);
			}
		}
		
		return stepDirection;
	}
}
