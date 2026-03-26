package black0ut1.dynamic.tdsp;

import black0ut1.data.PriorityQueue;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Destination;
import black0ut1.dynamic.loading.node.RoutedIntersection;
import black0ut1.util.DynamicUtils;

import java.util.Arrays;

public class TDDijkstra {
	
	protected final DynamicNetwork network;
	protected final double stepSize;
	
	public TDDijkstra(DynamicNetwork network, double stepSize) {
		this.network = network;
		this.stepSize = stepSize;
	}
	
	public Pair<Link[], double[]> run(int t, int r) {
		Link[] tree = new Link[network.routedIntersections.length];
		
		double[] distance = new double[network.routedIntersections.length];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[r] = t;
		
		byte[] mark = new byte[network.routedIntersections.length];
		
		PriorityQueue pq = new PriorityQueue();
		pq.add(r, t);
		while (!pq.isEmpty()) {
			
			double arrivalTime = pq.getMinPriority(); // time of arriving at node
			int node = pq.popMin();
			mark[node] = 2;
			
			RoutedIntersection intersection = network.routedIntersections[node];
			
			for (Link outgoingLink : intersection.outgoingLinks) {
				if (outgoingLink.head instanceof Destination)
					continue;
				
				int headIndex = outgoingLink.head.index;
				if (mark[headIndex] == 2)
					continue;
				
				double travelTime = DynamicUtils.computeTravelTime(arrivalTime, outgoingLink, stepSize);
				double newDistance = arrivalTime + travelTime; // TODO check if in time horizon
				if (mark[headIndex] == 0) {
					mark[headIndex] = 1;
					distance[headIndex] = newDistance;
					tree[headIndex] = outgoingLink;
					pq.add(headIndex, newDistance);
				} else if (mark[headIndex] == 1 && newDistance < distance[headIndex]) {
					distance[headIndex] = newDistance;
					tree[headIndex] = outgoingLink;
					pq.setLowerPriority(headIndex, newDistance);
				}
			}
		}
		
		return new Pair<>(tree, distance);
	}
}
