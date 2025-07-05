package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

import java.util.Vector;

public class SimplicialDecomposition extends LinkBasedAlgorithm {
	
	protected static final int SECANT_METHOD_MAX_ITERATIONS = 10;
	protected static final double SECANT_METHOD_PRECISION = 1e-10;
	
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
		
		System.out.println("Init smith gap: " + smithGap(flows, costs));
		for (int i = 0; i < 20; i++) {
			System.out.println("---- Inner iteration " + (i + 1) + " ----");
			
			double lambda = innerIteration();
			if (lambda == -1)
				break;
			
			System.out.println("Smith gap: " + smithGap(flows, costs));
			
			if (Math.abs(lambda) < 1e-4)
				break;
		}
		System.out.println();
	}
	
	protected double innerIteration() {
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
		
		
		// determine step size with secant method, finding the root of the derivative of
		// beckmann's function w.r.t. the step size, thus minimizing beckmann's function itself
		double lambda0 = 0, lambda1 = 1;
		for (int i = 0; i < SECANT_METHOD_MAX_ITERATIONS; i++) {
			
			double f0 = 0;
			double f1 = 0;
			
			for (int j = 0; j < network.edges; j++) {
				Network.Edge edge = network.getEdges()[j];
				
				f0 += costFunction.function(edge, flows[j] + lambda0 * deltaX[j]) * deltaX[j];
				f1 += costFunction.function(edge, flows[j] + lambda1 * deltaX[j]) * deltaX[j];
			}
			
			double nextLambda = lambda1 - f1 * (lambda1 - lambda0) / (f1 - f0);
			
			if (Math.abs(nextLambda - lambda1) < SECANT_METHOD_PRECISION) {
				lambda1 = nextLambda;
				break;
			}
			
			lambda0 = lambda1;
			lambda1 = nextLambda;
		}
		
		System.out.println("Lambda chosen: " + lambda1);
		for (int j = 0; j < network.edges; j++)
			flows[j] += lambda1 * deltaX[j];
		updateCosts();
		
		return lambda1;
	}
	
	protected double smithGap(double[] flows, double[] costs) {
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
