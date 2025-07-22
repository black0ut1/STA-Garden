package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

/**
 * @author Petr Pernicka
 */
public class PathEquilibration extends GradientProjection {
	
	
	public PathEquilibration(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
							 int maxIterations, Convergence.Builder convergenceBuilder,
							 ShortestPathStrategy shortestPathStrategy) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder, shortestPathStrategy);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		Path maxPath = null;
		double maxPathCost = Double.NEGATIVE_INFINITY;
		for (Path path : odPairs.get(origin, destination)) {
			double pathCost = path.getCost(costs);
			if (pathCost > maxPathCost) {
				maxPath = path;
				maxPathCost = pathCost;
			}
		}
		
		if (maxPath == null || maxPath == basicPath)
			return;
		
		double flowDelta = computeFlowDelta(basicPath, maxPath);
		if (flowDelta == 0)
			return;
		
		shiftFlows(flowDelta, basicPath, maxPath);
	}
}
