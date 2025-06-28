package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

public abstract class LinkBasedAlgorithm extends Algorithm {
	
	
	public LinkBasedAlgorithm(Network network, DoubleMatrix odMatrix,
							  CostFunction costFunction, int maxIterations,
							  Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void init() {
		AON.assign(network, odMatrix, costs, flows);
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		double[] target = calculateTarget();
		
		double stepSize = calculateStepSize(target);
		
		for (int j = 0; j < network.edges; j++)
			flows[j] = stepSize * target[j] + (1 - stepSize) * flows[j];
		
		// alternate expression better showing the step direction
		// flows[j] += stepSize * (target[j] - flows[j]);
		
		updateCosts();
	}
	
	protected abstract double[] calculateTarget();
	
	protected abstract double calculateStepSize(double[] target);
}
