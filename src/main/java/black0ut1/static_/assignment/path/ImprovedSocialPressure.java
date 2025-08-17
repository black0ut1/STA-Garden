package black0ut1.static_.assignment.path;

import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Settings;

public class ImprovedSocialPressure extends ProjectedGradient {
	
	public ImprovedSocialPressure(Settings settings) {
		super(settings);
	}
	
	@Override
	protected double[] calculateStepDirection(int origin, int destination, Path basicPath) {
		var paths = odPairs.get(origin, destination);
		
		// 1. Determine pi
		double[] costs =  new double[paths.size()];
		double maxPathCost = Double.NEGATIVE_INFINITY, minPathCost = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < paths.size(); i++) {
			costs[i] = paths.get(i).getCost(this.costs);
			
			if (costs[i] > maxPathCost)
				maxPathCost = costs[i];
			if (costs[i] < minPathCost)
				minPathCost = costs[i];
		}
		
		double pi = minPathCost + s.ISP_DELTA * (maxPathCost - minPathCost);
		
		
		double[] stepDirection = new double[paths.size()];
		
		// 2. Compute direction for paths with cost > pi
		double sum = 0;
		for (int i = 0; i < paths.size(); i++) {
			if (costs[i] > pi) {
				stepDirection[i] = minPathCost - costs[i];
				sum += stepDirection[i];
			}
		}
		
		double rectification = sum;
		
		// 3. Compute direction for paths with cost <= pi
		// 3.1  Compute derivatives of these paths and sum of their inverses
		double[] costDerivatives = new  double[paths.size()];
		double invSum = 0;
		for (int i = 0; i < paths.size(); i++) {
			
			if (costs[i] <= pi) {
				for (int edgeIndex : paths.get(i).edges) {
					Network.Edge edge = network.getEdges()[i];
					costDerivatives[i] += s.costFunction.derivative(edge, flows[edgeIndex]);
				}
				invSum += 1 / costDerivatives[i];
			}
		}
		
		// 3.2 Compute the direction
		for (int i = 0; i < paths.size(); i++) {
			if (costs[i] <= pi) {
				stepDirection[i] = -sum / (costDerivatives[i] * invSum);
				rectification += stepDirection[i];
			}
		}
		
		// Rectify step direction such that the sum is 0 - without it, the errors of
		// double precision will induce infeasible flows
		stepDirection[stepDirection.length - 1] -= rectification;
		
		return stepDirection;
	}
}
