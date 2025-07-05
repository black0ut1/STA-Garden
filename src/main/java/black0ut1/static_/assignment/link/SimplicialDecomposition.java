package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

import java.util.Vector;

public class SimplicialDecomposition extends LinkBasedAlgorithm {
	
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
		
		System.out.println("Initial Smith gap: " + smithGap());
		for (int i = 0; i < 10; i++) {
			System.out.println("Inner iteration " + (i + 1));
			innerIteration();
			System.out.println("Smith gap: " + smithGap());
		}
	}
	
	protected void innerIteration() {
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
	
	@Override
	protected double[] calculateTarget() {
		return null;
	}
	
	@Override
	protected double calculateStepSize(double[] target) {
		return 0;
	}
}
