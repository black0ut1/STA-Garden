package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

import java.util.Vector;

public class SimplicialDecomposition extends LinkBasedAlgorithm {
	
	protected static final int INNER_ITERATIONS = 50;
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	protected final Vector<double[]> hullVertices = new Vector<>();
	
	public SimplicialDecomposition(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
								   int maxIterations, Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		hullVertices.add(flows.clone());
	}
	
	@Override
	protected void mainLoopIteration() {
		double[] newHullVertex = new double[network.edges];
		AON.assign(network, odMatrix, costs, newHullVertex);
		hullVertices.add(newHullVertex);
		
		for (int i = 0; i < INNER_ITERATIONS; i++) {
			
			double[] target = calculateTarget();
			double stepSize = calculateStepSize(target);
			
			for (int j = 0; j < network.edges; j++)
				flows[j] += stepSize * target[j];
			
			updateCosts();
			
			if (iteration == 0) // only one inner iteration during the first main loop
				break;
		}
	}
	
	@Override
	protected double[] calculateTarget() {
		double[] deltaX = new double[network.edges];
		
		double demom = 0;
		for (double[] hullVertex : hullVertices) {
			
			double coeff = 0;
			for (int i = 0; i < network.edges; i++)
				coeff += costs[i] * (flows[i] - hullVertex[i]);
			coeff = Math.max(0, coeff);
			
			demom += coeff;
			
			for (int i = 0; i < network.edges; i++)
				deltaX[i] += coeff * (hullVertex[i] - flows[i]);
		}
		
		for (int i = 0; i < network.edges; i++)
			deltaX[i] /= demom;
		
		return deltaX;
	}
	
	@Override
	protected double calculateStepSize(double[] deltaX) {
		double stepSize = 0;
		
		for (int i = 0; i < NEWTON_MAX_ITERATIONS; i++) {
			
			double numerator = 0;
			double denominator = 0;
			for (Network.Edge edge : network.getEdges()) {
				
				double newFlow = flows[edge.index] + stepSize * deltaX[edge.index];
				numerator += costFunction.function(edge, newFlow) * deltaX[edge.index];
				denominator += costFunction.derivative(edge, newFlow) * deltaX[edge.index] * deltaX[edge.index];
			}
			
			double newStepSize = stepSize - (numerator / denominator);
			
			if (Math.abs(stepSize - newStepSize) < NEWTON_EPSILON) {
				stepSize = newStepSize;
				break;
			}
			
			stepSize = newStepSize;
		}
		
		return stepSize;
	}
	
	protected double smithGap() {
		double sum = 0;
		
		for (double[] hullVertex : hullVertices) {
			
			double dotProduct = 0;
			for (int i = 0; i < network.edges; i++)
				dotProduct += costs[i] * (flows[i] - hullVertex[i]);
			
			sum += dotProduct * dotProduct;
		}
		
		return sum;
	}
}
