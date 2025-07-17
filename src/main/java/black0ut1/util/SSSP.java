package black0ut1.util;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.data.PriorityQueue;

import java.util.Arrays;

public class SSSP {
	
	public static Pair<Network.Edge[], double[]> dijkstra(Network network, int root, double[] costs) {
		double[] distance = new double[network.nodes];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[root] = 0;
		
		Network.Edge[] previous = new Network.Edge[network.nodes];
		
		PriorityQueue pq = new PriorityQueue();
		byte[] mark = new byte[network.nodes];
		
		
		pq.add(root, 0);
		while (!pq.isEmpty()) {
			int fromVertex = pq.popMin();
			mark[fromVertex] = 2;
			
			for (Network.Edge edge : network.forwardStar(fromVertex)) {
				int toVertex = edge.head;
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
					pq.setLowerPriority(toVertex, newDistance);
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
		byte[] mark = new byte[network.nodes];
		
		
		pq.add(root, 0);
		while (!pq.isEmpty()) {
			int fromVertex = pq.popMin();
			mark[fromVertex] = 2;
			
			if (previous[fromVertex] != null) {
				int prev = previous[fromVertex].tail;
				pathLength[fromVertex] = pathLength[prev] + 1;
			}
			
			for (Network.Edge edge : network.forwardStar(fromVertex)) {
				int toVertex = edge.head;
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
					pq.setLowerPriority(toVertex, newDistance);
				}
			}
		}
		
		return new Pair<>(previous, pathLength);
	}
	
	public static Pair<Network.Edge[], double[]> dijkstraDest(Network network, int destination, double[] costs) {
		double[] distance = new double[network.nodes];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[destination] = 0;
		
		Network.Edge[] next = new Network.Edge[network.nodes];
		
		PriorityQueue pq = new PriorityQueue();
		byte[] mark = new byte[network.nodes];
		
		
		pq.add(destination, 0);
		while (!pq.isEmpty()) {
			int toVertex = pq.popMin();
			mark[toVertex] = 2;
			
			for (Network.Edge edge : network.backwardStar(toVertex)) {
				int fromVertex = edge.tail;
				if (mark[fromVertex] == 2)
					continue;
				
				double newDistance = distance[toVertex] + costs[edge.index];
				if (mark[fromVertex] == 0) {
					mark[fromVertex] = 1;
					distance[fromVertex] = newDistance;
					next[fromVertex] = edge;
					pq.add(fromVertex, newDistance);
				} else if (mark[fromVertex] == 1 && newDistance < distance[fromVertex]) {
					distance[fromVertex] = newDistance;
					next[fromVertex] = edge;
					pq.setLowerPriority(fromVertex, newDistance);
				}
			}
		}
		
		return new Pair<>(next, distance);
	}
	
	public static Pair<Network.Edge[], Integer> AstarLen(Network network, int origin, int destination, double[] costs, DoubleMatrix heuristic) {
		double[] distance = new double[network.nodes];
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		distance[origin] = 0;
		
		Network.Edge[] previous = new Network.Edge[network.nodes];
		
		int[] pathLength = new int[network.nodes];
		
		PriorityQueue pq = new PriorityQueue();
		byte[] mark = new byte[network.nodes];
		
		
		pq.add(origin, 0);
		while (!pq.isEmpty()) {
			int fromVertex = pq.popMin();
			if (fromVertex == destination)
				break;
			
			mark[fromVertex] = 2;
			
			if (previous[fromVertex] != null) {
				int prev = previous[fromVertex].tail;
				pathLength[fromVertex] = pathLength[prev] + 1;
			}
			
			for (Network.Edge edge : network.forwardStar(fromVertex)) {
				int toVertex = edge.head;
				if (mark[toVertex] == 2)
					continue;
				
				double newDistance = distance[fromVertex] + costs[edge.index] - heuristic.get(toVertex, destination);
				if (mark[toVertex] == 0) {
					mark[toVertex] = 1;
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.add(toVertex, newDistance);
				} else if (mark[toVertex] == 1 && newDistance < distance[toVertex]) {
					distance[toVertex] = newDistance;
					previous[toVertex] = edge;
					pq.setLowerPriority(toVertex, newDistance);
				}
			}
		}
		
		return new Pair<>(previous, pathLength[destination]);
	}
}
