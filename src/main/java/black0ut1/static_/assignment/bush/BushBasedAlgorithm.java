package black0ut1.static_.assignment.bush;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.SSSP;

public abstract class BushBasedAlgorithm extends Algorithm {
	
	protected final Bush[] bushes;
	
	public BushBasedAlgorithm(Network network, DoubleMatrix odMatrix,
							  CostFunction costFunction, int maxIterations,
							  Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
		this.bushes = new Bush[network.zones];
	}
	
	@Override
	protected void initialize() {
		for (int zone = 0; zone < bushes.length; zone++)
			bushes[zone] = createBush(zone);
		
		for (Bush bush : bushes)
			for (Network.Edge edge : network.getEdges())
				flows[edge.index] += bush.getEdgeFlow(edge.index);
		
		updateCosts();
	}
	
	protected Bush createBush(int zone) {
		Bush bush = new Bush(network.edges, zone);
		
		var pair = SSSP.dijkstra(network, zone, costs);
		var minTree = pair.first();
		var minDistance = pair.second();
		
		for (Network.Edge edge : network.getEdges())
			if (minDistance[edge.startNode] < minDistance[edge.endNode])
				bush.addEdge(edge.index);
		
		for (int node = 0; node < network.zones; node++) {
			double trips = odMatrix.get(zone, node);
			if (trips == 0)
				continue;
			
			for (Network.Edge edge = minTree[node];
				 edge != null;
				 edge = minTree[edge.startNode]) {
				bush.addFlow(edge.index, trips);
			}
		}
		
		return bush;
	}
	
	public Bush[] getBushes() {
		return bushes;
	}
}
