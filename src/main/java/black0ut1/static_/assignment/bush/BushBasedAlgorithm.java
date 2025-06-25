package black0ut1.static_.assignment.bush;

import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.data.tuple.Pair;
import black0ut1.data.tuple.Triplet;
import black0ut1.static_.assignment.STAAlgorithm;
import black0ut1.util.SSSP;

import java.util.Stack;
import java.util.Vector;

public abstract class BushBasedAlgorithm extends STAAlgorithm {
	
	protected static final double FLOW_CHECK_ERROR = 1e-9;
	
	protected final Bush[] bushes;
	
	public BushBasedAlgorithm(STAAlgorithm.Parameters parameters) {
		super(parameters);
		this.bushes = new Bush[network.zones];
	}
	
	@Override
	protected void init() {
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
	
	public void checkFlows() {
		double[] flowCheck = new double[network.edges];
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			for (int i = 0; i < network.edges; i++) {
				Network.Edge edge = network.getEdges()[i];
				
				if (bush.getEdgeFlow(i) < 0) {
					System.err.println("Negative flow: edge "
							+ edge.startNode + "->" + edge.endNode
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
						+ edge.startNode + "->" + edge.endNode
						+ ", difference " + Math.abs(flowCheck[i] - flows[i]));
			}
		}
	}
	
	public double removeCycleFlows() {
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
				
				int node = edge.startNode;
				if (nodeFlags[node]) {
					
					boolean inPath = false;
					for (int i = 0; i < pathLen; i++) {
						Network.Edge edge1 = currPath[i];
						if (edge1.startNode == node) {
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
							if (cycleEdge.startNode == node)
								break;
						}
						
						flowRemoved += cycleFlow;
						
						i = pathLen - 1;
						for (Network.Edge cycleEdge = currPath[i]; ;
							 cycleEdge = currPath[--i]) {
							
							bush.addFlow(cycleEdge.index, -cycleFlow);
							flows[cycleEdge.index] -= cycleFlow;
							if (cycleEdge.startNode == node)
								break;
						}
					}
				} else {
					nodeFlags[node] = true;
					
					for (Network.Edge newEdge : network.forwardStar(edge.endNode)) {
						if (bush.getEdgeFlow(newEdge.index) > 0)
							stack.push(new Pair<>(newEdge, pathLen + 1));
					}
				}
			}
		}
		
		return flowRemoved;
	}
	
	public Vector<Path> calculatePathFlows() {
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
					if (proportions[edge.index] == 0)
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
					
					if (edge.startNode == origin) {
						int[] edges = new int[pathLen];
						System.arraycopy(currPath, 0, edges, 0, pathLen);
						
						Path path = new Path(edges);
						path.flow = flow;
						paths.add(path);
					}
					
					for (Network.Edge nextEdge : network.backwardStar(edge.startNode)) {
						if (proportions[nextEdge.index] == 0)
							continue;
						
						var newData = new Triplet<>(nextEdge, pathLen + 1, flow * proportions[nextEdge.index]);
						stack.push(newData);
					}
				}
			}
		}
		
		return paths;
	}
}
