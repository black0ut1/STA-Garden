package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.Util;

/**
 * Bibliography:																		  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 9.5.2				  <br>
 * - (Jayakrishnan et al., 1994) Faster Path-Based Algorithm for Traffic Assignment
 */
public class GradientProjection extends PathBasedAlgorithm {
	
	public GradientProjection(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
							  int maxIterations, Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		for (Path path : odPairs.get(origin, destination)) {
			if (path == basicPath)
				continue;
			
			double flowDelta = computeFlowDelta(basicPath, path);
			if (flowDelta == 0)
				continue;
				
			shiftFlows(flowDelta, basicPath, path);
		}
	}
	
	protected double computeFlowDelta(Path basicPath, Path path) {
		double numerator = path.getCost(costs) - basicPath.getCost(costs);
		if (numerator == 0)
			return 0;
		
		double denominator = 0;
		for (int edgeIndex : basicPath.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			denominator += costFunction.derivative(edge, flows[edgeIndex]);
		}
		for (int edgeIndex : path.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			denominator += costFunction.derivative(edge, flows[edgeIndex]);
		}
		
		return Util.projectToInterval(numerator / denominator, 0, path.flow);
	}
	
	protected void shiftFlows(double flowDelta, Path basicPath, Path path) {
		basicPath.flow += flowDelta;
		for (int edgeIndex : basicPath.edges)
			flows[edgeIndex] += flowDelta;
		
		path.flow -= flowDelta;
		for (int edgeIndex : path.edges)
			flows[edgeIndex] -= flowDelta;
		
		updateCosts(basicPath);
		updateCosts(path);
	}
}
