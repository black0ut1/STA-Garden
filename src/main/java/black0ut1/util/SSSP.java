package black0ut1.util;

import black0ut1.data.Network;
import black0ut1.data.Pair;
import black0ut1.data.PriorityQueue;

import java.util.Arrays;

public class SSSP {
	
	public static Pair<Network.Edge[], double[]> dijkstra(Network network, int root, double[] costs) {
		double[] distance = new double[network.nodes];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[root] = 0;
		
		Network.Edge[] previous = new Network.Edge[network.nodes];
		
		PriorityQueue pq = new PriorityQueue();
		int[] mark = new int[network.nodes];
		
		
		pq.add(root, 0);
		while (!pq.isEmpty()) {
			int fromVertex = pq.popMin();
			mark[fromVertex] = 2;
			
			for (Network.Edge edge : network.neighborsOf(fromVertex)) {
				int toVertex = edge.endNode;
				if (mark[toVertex] == 2)
					continue;
				
				double newDistance = distance[fromVertex] + costs[edge.index];
				if (mark[toVertex] == 0) {
					mark[toVertex] = 1;
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.add(toVertex, newDistance);
				} else if (mark[toVertex] == 1 && newDistance < distance[toVertex]) {
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.decreasePriority(toVertex, newDistance);
				}
			}
		}
		
		return new Pair<>(previous, distance);
	}
	
	public static Pair<Network.Edge[], int[]> dijkstraLen(Network network, int root, double[] costs) {
		double[] distance = new double[network.nodes];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[root] = 0;
		
		Network.Edge[] previous = new Network.Edge[network.nodes];
		
		int[] pathLength = new int[network.nodes];
		
		PriorityQueue pq = new PriorityQueue();
		int[] mark = new int[network.nodes];
		
		
		pq.add(root, 0);
		while (!pq.isEmpty()) {
			int fromVertex = pq.popMin();
			mark[fromVertex] = 2;
			
			if (previous[fromVertex] != null) {
				int prev = previous[fromVertex].startNode;
				pathLength[fromVertex] = pathLength[prev] + 1;
			}
			
			for (Network.Edge edge : network.neighborsOf(fromVertex)) {
				int toVertex = edge.endNode;
				if (mark[toVertex] == 2)
					continue;
				
				double newDistance = distance[fromVertex] + costs[edge.index];
				if (mark[toVertex] == 0) {
					mark[toVertex] = 1;
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.add(toVertex, newDistance);
				} else if (mark[toVertex] == 1 && newDistance < distance[toVertex]) {
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.decreasePriority(toVertex, newDistance);
				}
			}
		}
		
		return new Pair<>(previous, pathLength);
	}
}
