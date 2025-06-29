package black0ut1.util;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.data.tuple.Pair;
import black0ut1.data.tuple.Triplet;

import java.util.Stack;
import java.util.Vector;

public class NetworkUtils {
	
	public static final double FLOW_CHECK_ERROR = 1e-9;
	
	public static Vector<Path> calculatePathsFromBushes(Network network, DoubleMatrix odMatrix, Bush[] bushes) {
		Vector<Path> paths = new Vector<>();
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			
			double[] proportions = new double[network.edges];
			for (int node = 0; node < network.nodes; node++) {
				
				double nodeFlow = 0;
				for (Network.Edge edge : network.backwardStar(node))
					nodeFlow += bush.getEdgeFlow(edge.index);
				
				for (Network.Edge edge : network.backwardStar(node)) {
					proportions[edge.index] = (nodeFlow == 0)
							? 0
							: bush.getEdgeFlow(edge.index) / nodeFlow;
				}
			}
			
			for (int destination = 0; destination < network.zones; destination++) {
				if (odMatrix.get(origin, destination) == 0)
					continue;
				
				double demand = odMatrix.get(origin, destination);
				int[] currPath = new int[network.nodes];
				Stack<Triplet<Network.Edge, Integer, Double>> stack = new Stack<>();
				
				for (Network.Edge edge : network.backwardStar(destination)) {
					if (!bush.edgeExists(edge.index) || proportions[edge.index] == 0)
						continue;
					
					var data = new Triplet<>(edge, 1, demand * proportions[edge.index]);
					stack.push(data);
				}
				
				while (!stack.empty()) {
					var data = stack.pop();
					Network.Edge edge = data.first();
					int pathLen = data.second();
					double flow = data.third();
					
					currPath[pathLen - 1] = edge.index;
					
					if (edge.tail == origin) {
						
						int[] edges = new int[pathLen];
						for (int i = 0; i < pathLen; i++)
							edges[pathLen - i - 1] = currPath[i];
						
						Path path = new Path(edges);
						path.flow = flow;
						paths.add(path);
					}
					
					for (Network.Edge nextEdge : network.backwardStar(edge.tail)) {
						if (!bush.edgeExists(edge.index) || proportions[nextEdge.index] == 0)
							continue;
						
						var newData = new Triplet<>(nextEdge, pathLen + 1, flow * proportions[nextEdge.index]);
						stack.push(newData);
					}
				}
			}
		}
		
		return paths;
	}
	
	public static void checkBushFlows(Network network, DoubleMatrix odMatrix, Bush[] bushes, double[] flows) {
		double[] flowCheck = new double[network.edges];
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			for (int i = 0; i < network.edges; i++) {
				Network.Edge edge = network.getEdges()[i];
				
				if (bush.getEdgeFlow(i) < 0) {
					System.err.println("Negative flow: edge "
							+ edge.tail + "->" + edge.head
							+ ", bush " + origin + ", flow " + bush.getEdgeFlow(i));
				}
				flowCheck[i] += bush.getEdgeFlow(i);
			}
			
			for (int destination = 0; destination < network.nodes; destination++) {
				if (destination == origin)
					continue;
				
				double balance = 0;
				
				for (Network.Edge edge : network.backwardStar(destination)) {
					balance += bush.getEdgeFlow(edge.index);
				}
				
				for (Network.Edge edge : network.forwardStar(destination)) {
					balance -= bush.getEdgeFlow(edge.index);
				}
				
				if (destination < network.zones)
					balance -= odMatrix.get(origin, destination);
				
				if (Math.abs(balance) > FLOW_CHECK_ERROR) {
					System.err.println("Conservation violated: bush " + origin
							+ ", node " + destination
							+ ", balance " + balance);
				}
			}
		}
		
		for (int i = 0; i < network.edges; i++) {
			Network.Edge edge = network.getEdges()[i];
			
			if (Math.abs(flowCheck[i] - flows[i]) > FLOW_CHECK_ERROR) {
				System.err.println("Bush flows not adding up: edge "
						+ edge.tail + "->" + edge.head
						+ ", difference " + Math.abs(flowCheck[i] - flows[i]));
			}
		}
	}
	
	// TODO not entirely working
	public static double removeCycleFlows(Network network, Bush[] bushes, double[] flows) {
		double flowRemoved = 0;
		Stack<Pair<Network.Edge, Integer>> stack = new Stack<>();
		
		for (Bush bush : bushes) {
			for (Network.Edge edge : network.forwardStar(bush.root)) {
				if (bush.getEdgeFlow(edge.index) > 0)
					stack.push(new Pair<>(edge, 1));
			}
			
			boolean[] nodeFlags = new boolean[network.nodes];
			Network.Edge[] currPath = new Network.Edge[network.edges];
			
			while (!stack.isEmpty()) {
				
				var pair = stack.pop();
				Network.Edge edge = pair.first();
				int pathLen = pair.second();
				
				currPath[pathLen - 1] = edge;
				
				int node = edge.tail;
				if (nodeFlags[node]) {
					
					boolean inPath = false;
					for (int i = 0; i < pathLen; i++) {
						Network.Edge edge1 = currPath[i];
						if (edge1.tail == node) {
							inPath = true;
							break;
						}
					}
					
					if (inPath) {
						double cycleFlow = Double.POSITIVE_INFINITY;
						int i = pathLen - 1;
						for (Network.Edge cycleEdge = currPath[i]; ;
							 cycleEdge = currPath[--i]) {
							
							cycleFlow = Math.min(cycleFlow, bush.getEdgeFlow(cycleEdge.index));
							if (cycleEdge.tail == node)
								break;
						}
						
						flowRemoved += cycleFlow;
						
						i = pathLen - 1;
						for (Network.Edge cycleEdge = currPath[i]; ;
							 cycleEdge = currPath[--i]) {
							
							bush.addFlow(cycleEdge.index, -cycleFlow);
							flows[cycleEdge.index] -= cycleFlow;
							if (cycleEdge.tail == node)
								break;
						}
					}
				} else {
					nodeFlags[node] = true;
					
					for (Network.Edge newEdge : network.forwardStar(edge.head)) {
						if (bush.getEdgeFlow(newEdge.index) > 0)
							stack.push(new Pair<>(newEdge, pathLen + 1));
					}
				}
			}
		}
		
		return flowRemoved;
	}
	
	// TODO not entirely working
	public static Bush[] originBushesToDestinationBushes(Network network, Bush[] bushes) {
		Bush[] invertedBushes = new Bush[bushes.length];
		for (int dest = 0; dest < bushes.length; dest++)
			invertedBushes[dest] = new Bush(network.edges, dest);
		
		for (Bush bush : bushes) {
			Stack<Network.Edge> stack = new Stack<>();
			Stack<EdgeFlow> currentPath = new Stack<>();
			
			for (Network.Edge edge : network.forwardStar(bush.root))
				if (bush.edgeExists(edge.index))
					stack.push(edge);
			
			while (!stack.isEmpty()) {
				Network.Edge edge = stack.pop();
				
				// path end encountered
				if (!currentPath.isEmpty() && edge.tail != currentPath.peek().edge.head) {
					double pathFlow = currentPath.peek().flow;
					int destination = currentPath.peek().edge.head;
					
					for (EdgeFlow edgeFlow : currentPath) {
						invertedBushes[destination].addEdge(edgeFlow.edge.index);
						invertedBushes[destination].addFlow(edgeFlow.edge.index, pathFlow);
						bush.addFlow(edgeFlow.edge.index, -pathFlow);
						edgeFlow.flow -= pathFlow;
					}
					
					while (!currentPath.isEmpty() && currentPath.peek().flow == 0)
						currentPath.pop();
					
					stack.push(edge);
					continue;
				}
				
				if (currentPath.isEmpty())
					currentPath.push(new EdgeFlow(edge, bush.getEdgeFlow(edge.index)));
				else
					currentPath.push(new EdgeFlow(edge, Math.min(currentPath.peek().flow, bush.getEdgeFlow(edge.index))));
				
				for (Network.Edge edge1 : network.forwardStar(edge.head))
					if (bush.edgeExists(edge1.index))
						stack.push(edge1);
			}
			
			while (!currentPath.isEmpty()) {
				double pathFlow = currentPath.peek().flow;
				int destination = currentPath.peek().edge.head;
				
				for (EdgeFlow edgeFlow : currentPath) {
					invertedBushes[destination].addEdge(edgeFlow.edge.index);
					invertedBushes[destination].addFlow(edgeFlow.edge.index, pathFlow);
					bush.addFlow(edgeFlow.edge.index, -pathFlow);
					edgeFlow.flow -= pathFlow;
				}
				
				while (!currentPath.isEmpty() && currentPath.peek().flow == 0)
					currentPath.pop();
			}
		}
		
		return invertedBushes;
	}
	
	private static class EdgeFlow {
		public Network.Edge edge;
		public double flow;
		
		public EdgeFlow(Network.Edge edge, double flow) {
			this.edge = edge;
			this.flow = flow;
		}
		
		@Override
		public String toString() {
			return edge.toString() + " " + flow;
		}
	}
}
