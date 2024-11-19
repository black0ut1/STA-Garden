package black0ut1.sta.assignment.bush;

import black0ut1.data.Bush;
import black0ut1.data.Network;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.util.SSSP;

public abstract class BushBasedAlgorithm extends Algorithm {
	
	protected final Bush[] bushes;
	
	public BushBasedAlgorithm(Algorithm.Parameters parameters) {
		super(parameters);
		this.bushes = new Bush[network.zones];
	}
	
	@Override
	protected void init() {
		updateCosts();
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
	
	protected void checkFlows() {
		double[] flowCheck = new double[network.edges];
		
		for (int zone = 0; zone < network.zones; zone++) {
			Bush bush = bushes[zone];
			
			for (int i = 0; i < network.edges; i++) {
				Network.Edge edge = network.getEdges()[i];
				
				if (bush.getEdgeFlow(i) < 0) {
					System.err.println("Negative flow: edge "
							+ edge.startNode + "->" + edge.endNode
							+ ", bush " + zone + ", flow " + flows[i]);
				}
				flowCheck[i] += bush.getEdgeFlow(i);
			}
			
			for (int node = 0; node < network.nodes; node++) {
				if (node == zone)
					continue;
				
				double balance = 0;
				
				for (Network.Edge edge : network.incomingOf(node)) {
					balance += bush.getEdgeFlow(edge.index);
				}
				
				for (Network.Edge edge : network.neighborsOf(node)) {
					balance -= bush.getEdgeFlow(edge.index);
				}
				
				if (node < network.zones)
					balance -= odMatrix.get(zone, node);
				
				if (Math.abs(balance) > 1e-9) {
					System.err.println("Conservation violated: bush " + zone
							+ " node " + node + ", balance: " + balance);
				}
			}
		}
		
		for (int i = 0; i < network.edges; i++) {
			Network.Edge edge = network.getEdges()[i];
			
			if (Math.abs(flowCheck[i] - flows[i]) > 1e-9) {
				System.err.println("Bush flows not adding up: edge "
						+ edge.startNode + "->" + edge.endNode
						+ ", added " + flowCheck[i] + ", flow " + flows[i]);
			}
		}
	}
}
