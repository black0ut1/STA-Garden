package black0ut1.dynamic.tdsp;

import black0ut1.data.PriorityQueue;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;

import java.util.Arrays;

public class TDDijkstra {
	
	protected final DynamicNetwork network;
	
	public TDDijkstra(DynamicNetwork network) {
		this.network = network;
	}
	
	public Link[] run(int t, int r) {
		Link[] tree = new Link[network.intersections.length];
		
		double[] distance = new double[network.intersections.length];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[r] = t;
		
		PriorityQueue pq = new PriorityQueue();
		pq.add(r, t);
		while (!pq.isEmpty()) {
			int node = pq.popMin();
		}
		
		return tree;
	}
}
