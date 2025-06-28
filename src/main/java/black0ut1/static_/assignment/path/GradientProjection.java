package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Matrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import java.util.Vector;

// TODO under construction
public class GradientProjection extends Algorithm {
	
	protected final Matrix<Vector<Path>> odPairs;
	
	public GradientProjection(Network network, DoubleMatrix odMatrix,
							  CostFunction costFunction, int maxIterations,
							  Convergence.Builder convergenceBuilder) {
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
				
				Path minPath = findMinPath(minTree, pathLengths, destination, paths);
				
				for (Path path : paths) {
					if (path == minPath)
						continue;
					
					double delta = findFlowDelta(path, minPath);
					path.addFlow(-delta);
					minPath.addFlow(delta);
				}
			}
			
			updateCosts();
		}
	}
	
	protected double findFlowDelta(Path path, Path minPath) {
		double numerator = path.cost - minPath.cost;
		double denominator = 0;
		
		int i = 0, j = 0;
		while (i < path.edgeIndices.length && j < minPath.edgeIndices.length) {
			if (path.edgeIndices[i] == minPath.edgeIndices[j]) {
				i++;
				j++;
			} else if (path.edgeIndices[i] < minPath.edgeIndices[j]) {
				int index = path.edgeIndices[i];
				denominator += costFunction.derivative(network.getEdges()[index], flows[index]);
				i++;
			} else {
				int index = minPath.edgeIndices[j];
				denominator += costFunction.derivative(network.getEdges()[index], flows[index]);
				j++;
			}
		}
		
		while (i < path.edgeIndices.length) {
			int index = path.edgeIndices[i];
			denominator += costFunction.derivative(network.getEdges()[index], flows[index]);
			i++;
		}
		
		while (j < minPath.edgeIndices.length) {
			int index = minPath.edgeIndices[j];
			denominator += costFunction.derivative(network.getEdges()[index], flows[index]);
			j++;
		}
		
		return Util.projectToInterval(numerator / denominator, 0, path.flow);
	}
	
	protected Path findMinPath(Network.Edge[] minTree, int[] pathLengths, int destination, Vector<Path> paths) {
		double minPathCost = 0;
		for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.startNode])
			minPathCost += costFunction.function(edge, flows[edge.index]);
		
		Path minPath = null;
		
		for (Path path : paths)
			if (path.cost == minPathCost) {
				minPath = path;
				break;
			}
		
		if (minPath == null) {
			int[] edgeIndices = new int[pathLengths[destination]];
			int i = 0;
			for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.startNode])
				edgeIndices[i++] = edge.index;
			minPath = new Path(edgeIndices);
			minPath.cost = minPathCost;
			
			paths.add(minPath);
		}
		
		return minPath;
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
