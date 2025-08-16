package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Matrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.util.SSSP;

import java.util.List;
import java.util.Vector;

/**
 * The base class for all path-based STA algorithms. There are two ways to approach their
 * framework which differ by the strategy of finding shortest paths. The first approach:  <br>
 * 1. Initialize																		  <br>
 * 1.1. Generate initial paths using AON												  <br>
 * 1.2. Update costs																	  <br>
 * 2. For each origin O																	  <br>
 * 2.1. Find shortest paths to all destinations using a SSSP algorithm					  <br>
 * 2.2. For each destination D															  <br>
 * 2.2.1. If the demand from O to D is zero, continue with next destination				  <br>
 * 2.2.2. Reconstruct the new found shortest path. If it is already contained in the set
 * of paths from O to D, use that instance, otherwise add the new path to the set		  <br>
 * 2.2.3. Equilibrate the set of paths from O to D and update their costs				  <br>
 * 2.3.4. Remove all paths with zero path flow from the set								  <br>
 * In the second approach, steps 2. to 2.2.1 are replaced with the following:			  <br>
 * 2. For each origin O																	  <br>
 * 2.2. For each destination D															  <br>
 * 2.2.1. If the demand from O to D is zero, continue with next destination				  <br>
 * 2.2.2. Find the shortest path from O to D using a P2PSP algorithm					  <br>
 * In the first approach, we find shortest paths from single origin to all other
 * destinations using a single-source shortest path (typically Dijkstra). This is
 * generally faster but less accurate than the second approach, because as we equilibrate
 * some destinations, the shortest paths become outdated. This is not the case for the
 * second approach, which uses point-to-point shortest path algorithm (typically A*) which
 * finds the most up-to-date shortest path. This is generally slower, but could be faster
 * for sparse OD matrices.
 * <p>
 * Bibliography:																		  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.3					  <br>
 */
public abstract class PathBasedAlgorithm extends Algorithm {
	
	protected final Matrix<Vector<Path>> odPairs;
	protected DoubleMatrix heuristic = null;
	
	public PathBasedAlgorithm(Settings settings) {
		super(settings);
		this.odPairs = new Matrix<>(network.zones);
	}
	
	
	@Override
	protected void initialize() {
		
		for (int origin = 0; origin < network.zones; origin++) {
			
			var a = SSSP.dijkstraLen(network, origin, costs);
			Network.Edge[] minTree = a.first();
			int[] pathLengths = a.second();
			
			for (int destination = 0; destination < network.zones; destination++) {
				if (odm.get(origin, destination) == 0)
					continue;
				
				int[] edgeIndices = new int[pathLengths[destination]];
				int i = edgeIndices.length - 1;
				for (Network.Edge edge = minTree[destination]; edge != null; edge = minTree[edge.tail])
					edgeIndices[i--] = edge.index;
				
				Path path = new Path(edgeIndices);
				double trips = odm.get(origin, destination);
				
				path.flow = trips;
				for (int edge : path.edges)
					flows[edge] += trips;
				
				odPairs.set(origin, destination, new Vector<>(List.of(path)));
			}
		}
		
		if (s.SHORTEST_PATH_STRATEGY == Settings.ShortestPathStrategy.P2PSP) {
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
		Network.Edge[] minTree = null;
		int[] pathLengths = null;
		SSSP.Astar astar = (s.SHORTEST_PATH_STRATEGY == Settings.ShortestPathStrategy.P2PSP)
				? new SSSP.Astar(network, heuristic)
				: null;
		
		// For each origin
		for (int origin = 0; origin < network.zones; origin++) {
			
			switch (s.SHORTEST_PATH_STRATEGY) {
				case P2PSP:
					// Reset A* for new origin
					astar.resetForOrigin(origin);
					break;
				
				case SSSP:
					// Find shortest paths from this origin to all other destinations
					var a = SSSP.dijkstraLen(network, origin, costs);
					minTree = a.first();
					pathLengths = a.second();
					break;
			}
			
			
			// For each destination
			for (int destination = 0; destination < network.zones; destination++) {
				if (odm.get(origin, destination) == 0) // Skip empty OD pairs
					continue;
				
				int length = switch (s.SHORTEST_PATH_STRATEGY) {
					case P2PSP:
						// Find shortest path from origin to destination
						double shortestPathCost = Double.POSITIVE_INFINITY;
						for (Path path : odPairs.get(origin, destination)) {
							double cost = path.getCost(costs);
							if (cost < shortestPathCost)
								shortestPathCost = cost;
						}
						
						var pair = astar.calculate(origin, destination, costs, shortestPathCost);
						minTree = pair.first();
						yield pair.second();
					case SSSP:
						yield pathLengths[destination];
				};
				
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
				if (odPairs.get(origin, destination).size() == 1)
					continue;
				
				equilibratePaths(origin, destination, basicPath);
				
				odPairs.get(origin, destination).removeIf(path -> path.flow <= 0);
			}
		}
	}
	
	protected abstract void equilibratePaths(int origin, int destination, Path basicPath);
	
	protected void updateCosts(Path path) {
		for (int edgeIndex : path.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			costs[edgeIndex] = s.costFunction.function(edge, flows[edgeIndex]);
		}
	}
}
