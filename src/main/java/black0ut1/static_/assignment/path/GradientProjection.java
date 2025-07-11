package black0ut1.static_.assignment.path;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import java.util.BitSet;
import java.util.Vector;

public class GradientProjection extends PathBasedAlgorithm {
	
	public GradientProjection(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
							  int maxIterations, Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		for (Path path : odPairs.get(origin, destination)) {
			if (path == basicPath)
				continue;
			
			// TODO measure the convergence when using symmetric difference
			int[] symmetricDifference = symmetricDifference(basicPath, path);
			double flowDelta = computeFlowDelta(basicPath, path, symmetricDifference);
			if (flowDelta == 0)
				continue;
				
			shiftFlows(flowDelta, basicPath, path);
		}
	}
	
	/**
	 * Computes the symmetric difference of the two paths, i.e. a set of links, where each
	 * link is contained in either of the paths but not both.
	 * @return Array of edge indices.
	 */
	protected int[] symmetricDifference(Path basicPath, Path path) {
		BitSet bitSet1 = new BitSet(network.edges);
		BitSet bitSet2 = new BitSet(network.edges);
		
		for (int edgeIndex : basicPath.edges)
			bitSet1.set(edgeIndex);
		for (int edgeIndex : path.edges)
			bitSet2.set(edgeIndex);
		
		bitSet1.xor(bitSet2);
		
		int[] result = new int[bitSet1.cardinality()];
		int i = 0;
		for (int index = bitSet1.nextSetBit(0); index >= 0; index = bitSet1.nextSetBit(index + 1)) {
			result[i++] = index;
		}
		
		return result;
	}
	
	protected double computeFlowDelta(Path basicPath, Path path, int[] symDiff) {
		double numerator = path.getCost(costs) - basicPath.getCost(costs);
		if (numerator == 0)
			return 0;
		
		double denominator = 0;
		for (int edgeIndex : basicPath.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			denominator += costFunction.derivative(edge, flows[edgeIndex]);
		}
		for (int edgeIndex : path.edges) {
			Network.Edge edge = network.getEdges()[edgeIndex];
			denominator += costFunction.derivative(edge, flows[edgeIndex]);
		}
		
		return Util.projectToInterval(numerator / denominator, 0, path.flow);
	}
	
	protected void shiftFlows(double flowDelta, Path basicPath, Path path) {
		basicPath.flow += flowDelta;
		for (int edgeIndex : basicPath.edges)
			flows[edgeIndex] += flowDelta;
		
		path.flow -= flowDelta;
		for (int edgeIndex : path.edges)
			flows[edgeIndex] -= flowDelta;
		
		updateCosts(basicPath);
		updateCosts(path);
	}
}
