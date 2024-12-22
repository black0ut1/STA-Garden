package black0ut1.static_.assignment;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.util.SSSP;

public class AON {
	
	public static void assign(Network network, DoubleMatrix odMatrix, double[] costs, double[] flows) {
		
		for (int zone = 0; zone < network.zones; zone++) {
			Network.Edge[] previous = SSSP.dijkstra(network, zone, costs).first();
			
			for (int node = 0; node < network.zones; node++) {
				double trips = odMatrix.get(zone, node);
				if (trips == 0)
					continue;
				
				for (Network.Edge edge = previous[node];
					 edge != null;
					 edge = previous[edge.startNode]) {
					flows[edge.index] += trips;
				}
			}
		}
	}
}
