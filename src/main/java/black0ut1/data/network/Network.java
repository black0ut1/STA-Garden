package black0ut1.data.network;

import black0ut1.data.ArrayView;

import java.util.List;
import java.util.Vector;

@SuppressWarnings("unchecked")
public class Network {

	private final int[] indices;
	private final Edge[] edgesArr;
	
	private final int[] inverseIndices;
	private final Edge[] inverseEdgesArr;
	
	private final Edge[] mirrorEdgesArr;
	
	private final Node[] nodesArr;
	
	public final int nodes;
	public final int zones;
	public final int edges;
	
	public Network(List<Edge> edgesList, Node[] nodesArray, int nodes, int zones) {
		this.nodesArr = nodesArray;
		
		this.edges = edgesList.size();
		this.zones = zones;
		this.nodes = nodes;
		
		this.indices = new int[this.nodes + 1];
		this.inverseIndices = new int[this.nodes + 1];
		this.edgesArr = new Edge[this.edges];
		this.inverseEdgesArr = new Edge[this.edges];
		this.mirrorEdgesArr = new Edge[this.edges];
		
		// build adjacency list and compressed sparse row arrays for forward stars
		Vector<Edge>[] adjacencyList = new Vector[this.nodes];
		for (int i = 0; i < this.nodes; i++)
			adjacencyList[i] = new Vector<>();
		
		for (Edge edge : edgesList)
			adjacencyList[edge.tail].add(edge);
		
		int offset = 0;
		for (int startNode = 0; startNode < adjacencyList.length; startNode++) {
			indices[startNode] = offset;
			
			var neighbors = adjacencyList[startNode];
			for (int i = 0; i < neighbors.size(); i++)
				edgesArr[offset + i] = new Edge(neighbors.get(i), offset + i);
			
			offset += neighbors.size();
		}
		indices[indices.length - 1] = offset;
		
		// build inverse adjacency list and compressed sparse row arrays for backward stars
		Vector<Edge>[] inverseAdjacencyList = new Vector[this.nodes];
		for (int i = 0; i < this.nodes; i++)
			inverseAdjacencyList[i] = new Vector<>();
		
		for (Edge edge : edgesArr)
			inverseAdjacencyList[edge.head].add(edge);
		
		offset = 0;
		for (int endNode = 0; endNode < inverseAdjacencyList.length; endNode++) {
			inverseIndices[endNode] = offset;
			
			var incoming = inverseAdjacencyList[endNode];
			for (int i = 0; i < incoming.size(); i++)
				inverseEdgesArr[offset + i] = incoming.get(i);
			
			offset += incoming.size();
		}
		inverseIndices[inverseIndices.length - 1] = offset;
		
		// build array for mirror edges
		for (Edge edge : edgesArr) {
			Network.Edge mirror = null;
			for (Edge edge1 : forwardStar(edge.head))
				if (edge1.head == edge.tail) {
					mirror = edge1;
					break;
				}
			
			mirrorEdgesArr[edge.index] = mirror;
		}
	}
	
	public ArrayView<Edge> forwardStar(int node) {
		return new ArrayView<>(edgesArr, indices[node], indices[node + 1]);
	}
	
	public ArrayView<Edge> backwardStar(int node) {
		return new ArrayView<>(inverseEdgesArr, inverseIndices[node], inverseIndices[node + 1]);
	}
	
	public Network.Edge mirrorEdgeOf(int edgeIndex) {
		return mirrorEdgesArr[edgeIndex];
	}
	
	public Edge[] getEdges() {
		return edgesArr;
	}
	
	public Node[] getNodes() {
		return nodesArr;
	}
	
	public static class Edge {
		
		public final int head;
		public final int tail;
		public final int index;
		public final double capacity;
		public final double length;
		public final double freeFlow;
		public final double alpha;
		public final double beta;

		public Edge(int tail, int head, double capacity, double length,
					double freeFlow, double alpha, double beta) {
			this.tail = tail;
			this.head = head;
			this.capacity = capacity;
			this.length = length;
			this.freeFlow = freeFlow;
			this.alpha = alpha;
			this.beta = beta;
			this.index = -1;
		}

		private Edge(Edge copy, int index) {
			this.tail = copy.tail;
			this.head = copy.head;
			this.capacity = copy.capacity;
			this.length = copy.length;
			this.freeFlow = copy.freeFlow;
			this.alpha = copy.alpha;
			this.beta = copy.beta;
			this.index = index;
		}
		
		@Override
		public String toString() {
			return tail + "->" + head;
		}
	}
	
	public record Node(int id, double x, double y) {}
}
