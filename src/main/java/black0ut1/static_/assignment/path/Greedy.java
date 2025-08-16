package black0ut1.static_.assignment.path;

import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Settings;

import java.util.Arrays;
import java.util.Comparator;

public class Greedy extends PathBasedAlgorithm {
	
	public Greedy(Settings settings) {
		super(settings);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		var paths = odPairs.get(origin, destination);
		
		// 1. Initialization
		double[] pathCosts = new double[paths.size()];
		double[] pathCostDerivatives = new double[paths.size()];
		double[] c = new double[paths.size()];
		
		for (int i = 0; i < paths.size(); i++) {
			
			for (int edgeIndex : paths.get(i).edges) {
				Network.Edge edge = network.getEdges()[edgeIndex];
				pathCostDerivatives[i] += s.costFunction.derivative(edge, flows[edgeIndex]);
			}
			
			pathCosts[i] = paths.get(i).getCost(costs);
			c[i] = pathCosts[i] - pathCostDerivatives[i] * paths.get(i).flow;
		}
		
		// 2. Sort path indices according to c
		Integer[] indices = new Integer[paths.size()];
		for (int i = 0; i < paths.size(); i++)
			indices[i] = i;
		
		Arrays.sort(indices, Comparator.comparingDouble(i -> c[i]));
		
		// 3. Main loop
		double B = 0, C = 0;
		double w = Double.POSITIVE_INFINITY;
		int h;
		for (h = 0; h < paths.size() && c[indices[h]] < w; h++) {
			double tmp = pathCostDerivatives[indices[h]] * odm.get(origin, destination);
			B += 1 / tmp;
			C += c[indices[h]] / tmp;
			w = (1 + C) / B;
		}
		
		// 4. Update flow
		double rectification = odm.get(origin, destination);
		for (int i = 0; i < paths.size(); i++) {
			Path path = paths.get(indices[i]);
			
			double newFlow = (i < h)
					? (w - c[indices[i]]) / pathCostDerivatives[indices[i]]
					: 0;
			
			rectification -= newFlow;
			if (i == h - 1)
				newFlow += rectification;
			
			double flowDelta = newFlow - path.flow;
			
			path.flow = newFlow;
			for (int edgeIndex : path.edges)
				flows[edgeIndex] += flowDelta;
			
			updateCosts(path);
		}
	}
}
