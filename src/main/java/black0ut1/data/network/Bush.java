package black0ut1.data.network;

import java.util.Stack;

public class Bush {
	
	private final boolean[] edgeFlags;
	
	private final double[] flows;
	
	public final int root;
	
	public Bush(int arcsNum, int root) {
		this.edgeFlags = new boolean[arcsNum];
		this.flows = new double[arcsNum];
		this.root = root;
	}
	
	public void addEdge(int edgeIndex) {
		edgeFlags[edgeIndex] = true;
	}
	
	public void removeEdge(int edgeIndex) {
		edgeFlags[edgeIndex] = false;
	}

	public boolean edgeExists(int edgeIndex) {
		return edgeFlags[edgeIndex];
	}

	public double getEdgeFlow(int edgeIndex) {
		return flows[edgeIndex];
	}

	public void addFlow(int edgeIndex, double flow) {
		flows[edgeIndex] += flow;
	}
	
	public static Bush[] invertBushes(Network network, Bush[] bushes) {
		Bush[] invertedBushes = new Bush[bushes.length];
		for (int dest = 0; dest < bushes.length; dest++)
			invertedBushes[dest] = new Bush(bushes[0].flows.length, dest);
		
		for (Bush bush : bushes) {
			Stack<Network.Edge> stack = new Stack<>();
			Stack<EdgeFlow> currentPath = new Stack<>();
			
			for (Network.Edge edge : network.forwardStar(bush.root))
				if (bush.edgeExists(edge.index))
					stack.push(edge);
			
			while (!stack.isEmpty()) {
				Network.Edge edge = stack.pop();
				
				// path end encountered
				if (!currentPath.isEmpty() && edge.startNode != currentPath.peek().edge.endNode) {
					double pathFlow = currentPath.peek().flow;
					int destination = currentPath.peek().edge.endNode;
					
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
				
				for (Network.Edge edge1 : network.forwardStar(edge.endNode))
					if (bush.edgeExists(edge1.index))
						stack.push(edge1);
			}
			
			while (!currentPath.isEmpty()) {
				double pathFlow = currentPath.peek().flow;
				int destination = currentPath.peek().edge.endNode;
				
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
