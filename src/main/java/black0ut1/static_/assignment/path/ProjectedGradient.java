package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Matrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.STAAlgorithm;
import black0ut1.static_.assignment.STAConvergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import java.util.Vector;

// TODO under construction
public class ProjectedGradient extends STAAlgorithm {
	
	protected final Matrix<Vector<Path>> odPairs;
	
	public ProjectedGradient(Network network, DoubleMatrix odMatrix,
							 CostFunction costFunction, int maxIterations,
							 STAConvergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
		this.odPairs = new Matrix<>(network.zones);
	}
	
	@Override
	protected void init() {
		updateCosts();
		for (int origin = 0; origin < network.zones; origin++) {
			
			var a = SSSP.dijkstraLen(network, origin, costs);
			Network.Edge[] minTree = a.first();
			int[] pathLengths = a.second();
			
			for (int destination = 0; destination < network.zones; destination++) {
				if (odMatrix.get(origin, destination) == 0)
					continue;
				
				odPairs.set(origin, destination, new Vector<>());
				
				int[] edgeIndices = new int[pathLengths[destination]];
				int i = 0;
				for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.startNode])
					edgeIndices[i++] = edge.index;
				
				Path minPath = new Path(edgeIndices);
				minPath.updateCost();
				minPath.addFlow(odMatrix.get(origin, destination));
				
				odPairs.get(origin, destination).add(minPath);
			}
		}
		
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		for (int origin = 0; origin < network.zones; origin++) {
			
			var a = SSSP.dijkstraLen(network, origin, costs);
			Network.Edge[] minTree = a.first();
			int[] pathLengths = a.second();
			
			for (int destination = 0; destination < network.zones; destination++) {
				var paths = odPairs.get(origin, destination);
				if (paths == null)
					continue;
				
				for (Path path : paths)
					path.updateCost();
				
				double avgCost = getAveragePathCost(paths);
				double stepSize = getStepSize(paths, avgCost);
				
				for (Path path : paths) {
					double deltaX = path.cost - avgCost;
					
					path.addFlow(stepSize * deltaX);
				}
			}
			
			updateCosts();
		}
	}
	
	protected double getStepSize(Vector<Path> paths, double avgCost) {
		
		
		double maxStepSize = 0;
		for (Path path : paths) {
			double a = path.flow / (path.cost - avgCost);
			if (a > maxStepSize)
				maxStepSize = a;
		}
		
		double stepSize = maxStepSize / 2;
		
		
		return Util.projectToInterval(stepSize, 0, maxStepSize);
	}
	
	protected double getAveragePathCost(Vector<Path> paths) {
		int numPaths = paths.size();
		
		double totalTT = 0;
		for (Path path : paths)
			totalTT += path.cost;
		
		return totalTT / numPaths;
	}
	
	protected class Path {
		
		public final int[] edgeIndices;
		private double flow = 0;
		private double cost;
		
		public Path(int[] edgeIndices) {
			this.edgeIndices = edgeIndices;
		}
		
		public void addFlow(double flow) {
			this.flow += flow;
			for (int edgeIndex : edgeIndices)
				flows[edgeIndex] += flow;
		}
		
		private void updateCost() {
			cost = 0;
			for (int index : edgeIndices)
				cost += costFunction.function(network.getEdges()[index], flows[index]);
		}
	}
}
