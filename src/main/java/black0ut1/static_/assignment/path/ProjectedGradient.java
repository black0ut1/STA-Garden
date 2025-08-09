package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.Util;
import com.carrotsearch.hppc.IntDoubleHashMap;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;

import java.util.Vector;

public class ProjectedGradient extends PathBasedAlgorithm {
	
	protected static final double STEP_DIRECTION_EPSILON = 1e-12;
	protected final IntDoubleHashMap edgeIndicesToCoeff = new IntDoubleHashMap();
	
	public ProjectedGradient(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
							 int maxIterations, Convergence.Builder convergenceBuilder,
							 ShortestPathStrategy shortestPathStrategy) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder, shortestPathStrategy);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		double[] stepDirection = calculateStepDirection(origin, destination, basicPath);
		if (stepDirection == null)
			return;
		
		double stepSize = calculateStepSize(origin, destination, stepDirection);
		if (stepSize == 0)
			return;
		
		shiftFlows(origin, destination, stepDirection, stepSize);
	}
	
	protected double[] calculateStepDirection(int origin, int destination, Path basicPath) {
		Vector<Path> paths = odPairs.get(origin, destination);
		
		double averageTravelTime = 0;
		double min = Double.POSITIVE_INFINITY,  max = Double.NEGATIVE_INFINITY;
		double[] stepDirection = new double[paths.size()];
		
		for (int i = 0; i < stepDirection.length; i++) {
			double pathCost = paths.get(i).getCost(costs);
			
			if (pathCost < min)
				min = pathCost;
			if (pathCost > max)
				max = pathCost;
			
			stepDirection[i] = pathCost;
			averageTravelTime += pathCost;
		}
		
		// if step direction is too small, skip this OD pair (avoids numerical errors)
		if (max - min < STEP_DIRECTION_EPSILON)
			return null;
		
		averageTravelTime /= paths.size();
		double rectification = 0;
		for (int i = 0; i < stepDirection.length; i++) {
			stepDirection[i] = averageTravelTime - stepDirection[i];
			rectification += stepDirection[i];
		}
		
		// Rectify step direction such that the sum is 0 - without it, the errors of
		// double precision will induce infeasible flows
		stepDirection[stepDirection.length - 1] -= rectification;
		
		return stepDirection;
	}
	
	protected double calculateStepSize(int origin, int destination, double[] stepDirection) {
		Vector<Path> paths = odPairs.get(origin, destination);
		
		double maxStepSize = Double.POSITIVE_INFINITY;
		for (int i = 0; i < paths.size(); i++) {
			if (stepDirection[i] < 0)
				maxStepSize = Math.min(maxStepSize, -paths.get(i).flow / stepDirection[i]);
		}
		
		if (maxStepSize <= 0)
			return 0;
		
		edgeIndicesToCoeff.clear();
		for (int i = 0; i < paths.size(); i++)
			for (int edgeIndex : paths.get(i).edges)
				edgeIndicesToCoeff.addTo(edgeIndex, stepDirection[i]);

		double numerator = 0;
		double denominator = 0;
		for (IntDoubleCursor intDoubleCursor : edgeIndicesToCoeff) {
			int edgeIndex = intDoubleCursor.key;
			double coeff = intDoubleCursor.value;
			
			Network.Edge edge = network.getEdges()[edgeIndex];
			numerator += costFunction.function(edge, flows[edgeIndex]) * coeff;
			denominator += costFunction.derivative(edge, flows[edgeIndex]) * coeff * coeff;
		}

		if (numerator == 0)
			return 0;
		return Util.projectToInterval(-numerator / denominator, 0, maxStepSize);
	}
	
	protected void shiftFlows(int origin, int destination, double[] stepDirection, double stepSize) {
		Vector<Path> paths = odPairs.get(origin, destination);
		
		for (int i = 0; i < paths.size(); i++) {
			Path path = paths.get(i);
			
			double flowShift = stepSize * stepDirection[i];
			path.flow += flowShift;
			
			for (int edgeIndex : path.edges)
				flows[edgeIndex] += flowShift;
			
			updateCosts(path);
		}
	}
}
