package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Matrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.NetworkUtils;
import black0ut1.util.SSSP;

import java.util.List;
import java.util.Vector;

public abstract class PathBasedAlgorithm extends Algorithm {
	
	protected final Matrix<Vector<Path>> odPairs;
	protected final ShortestPathStrategy shortestPathStrategy;
	protected DoubleMatrix heuristic = null;
	
	public PathBasedAlgorithm(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
							  int maxIterations, Convergence.Builder convergenceBuilder,
							  ShortestPathStrategy shortestPathStrategy) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
		this.shortestPathStrategy = shortestPathStrategy;
		this.odPairs = new Matrix<>(network.zones);
	}
	
	@Override
	protected void initialize() {
		
		for (int origin = 0; origin < network.zones; origin++) {
			
			var a = SSSP.dijkstraLen(network, origin, costs);
			Network.Edge[] minTree = a.first();
			int[] pathLengths = a.second();
			
			for (int destination = 0; destination < network.zones; destination++) {
				if (odMatrix.get(origin, destination) == 0)
					continue;
				
				int[] edgeIndices = new int[pathLengths[destination]];
				int i = edgeIndices.length - 1;
				for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.tail])
					edgeIndices[i--] = edge.index;
				
				Path path = new Path(edgeIndices);
				double trips = odMatrix.get(origin, destination);
				
				path.flow = trips;
				for (int edge : path.edges)
					flows[edge] += trips;
				
				odPairs.set(origin, destination, new Vector<>(List.of(path)));
			}
		}
		
		if (shortestPathStrategy == ShortestPathStrategy.P2PSP) {
			heuristic = new DoubleMatrix(network.nodes, network.zones);
			
			for (int destination = 0; destination < network.zones; destination++) {
				double[] distance = SSSP.dijkstraDest(network, destination, costs).second();
				
				for (int node = 0; node < network.nodes; node++)
					heuristic.set(node, destination, distance[node]);
			}
		}
		
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		
		if (shortestPathStrategy == ShortestPathStrategy.SSSP) {
			
			for (int origin = 0; origin < network.zones; origin++) {
				
				var a = SSSP.dijkstraLen(network, origin, costs);
				Network.Edge[] minTree = a.first();
				int[] pathLengths = a.second();
				
				for (int destination = 0; destination < network.zones; destination++) {
					if (odMatrix.get(origin, destination) == 0)
						continue;
					
					int[] edgeIndices = new int[pathLengths[destination]];
					int i = edgeIndices.length - 1;
					for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.tail])
						edgeIndices[i--] = edge.index;
					
					Path basicPath = new Path(edgeIndices);
					
					// Check if this shortest path is already in the set
					boolean exists = false;
					for (Path path : odPairs.get(origin, destination))
						if (basicPath.equals(path)) {
							basicPath = path;
							exists = true;
							break;
						}
					if (!exists) // if not, add it
						odPairs.get(origin, destination).add(basicPath);
					
					equilibratePaths(origin, destination, basicPath);
					
					odPairs.get(origin, destination).removeIf(path -> path.flow <= 0);
				}
			}
			
		} else if (shortestPathStrategy == ShortestPathStrategy.P2PSP) {
			
			for (int origin = 0; origin < network.zones; origin++) {
				
				SSSP.Astar astar = new SSSP.Astar(network, heuristic, origin);
				
				for (int destination = 0; destination < network.zones; destination++) {
					if (odMatrix.get(origin, destination) == 0)
						continue;
					
					double shortestPathCost = Double.POSITIVE_INFINITY;
					for (Path path : odPairs.get(origin, destination)) {
						double cost = path.getCost(costs);
						if (cost < shortestPathCost)
							shortestPathCost = cost;
					}
					
					var pair = astar.calculate(destination, costs, shortestPathCost);
					Network.Edge[] minTree = pair.first();
					int length = pair.second();
					
					int[] edgeIndices = new int[length];
					int i = edgeIndices.length - 1;
					for (Network.Edge edge = minTree[destination]; i != -1; edge = minTree[edge.tail])
						edgeIndices[i--] = edge.index;
					
					Path basicPath = new Path(edgeIndices);
					
					// Check if this shortest path is already in the set
					boolean exists = false;
					for (Path path : odPairs.get(origin, destination))
						if (basicPath.equals(path)) {
							basicPath = path;
							exists = true;
							break;
						}
					if (!exists) // if not, add it
						odPairs.get(origin, destination).add(basicPath);
					
					equilibratePaths(origin, destination, basicPath);
					
					odPairs.get(origin, destination).removeIf(path -> path.flow <= 0);
				}
			}
		}
	}
	
	protected abstract void equilibratePaths(int origin, int destination, Path basicPath);
	
	protected void updateCosts(Path path) {
		for (int edgeIndex : path.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			costs[edgeIndex] = costFunction.function(edge, flows[edgeIndex]);
		}
	}
	
	public enum ShortestPathStrategy {
		P2PSP,
		SSSP,
	}
}
