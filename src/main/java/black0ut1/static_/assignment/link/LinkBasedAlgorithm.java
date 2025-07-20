package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;

/**
 * The base class for all link-based STA algorithms. Their common framework is the
 * following:																			  <br>
 * 1. Initialize												 						  <br>
 * 1.1. Generate initial link solution using AON										  <br>
 * 1.2. Update costs																	  <br>
 * 2. Iterate																			  <br>
 * 2.1. Generate target solution														  <br>
 * 2.2. Determine a step size (from interval [0, 1]) towards the target solution		  <br>
 * 2.3. Update current solution: current = step size * target + (1 - step size) * current <br>
 * 2.4. Update link costs																  <br>
 * <p>
 * Bibliography:																		  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2					  <br>
 */
public abstract class LinkBasedAlgorithm extends Algorithm {
	
	public LinkBasedAlgorithm(Network network, DoubleMatrix odMatrix,
							  CostFunction costFunction, int maxIterations,
							  Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void initialize() {
		// 1.1. Generate initial solution using AON
		AON.assign(network, odMatrix, costs, flows);
		
		// 1.2. Update costs
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		// 2.1. Generate target solution
		double[] target = calculateTarget();
		
		// 2.2. Determine a step size towards the target solution
		double stepSize = calculateStepSize(target);
		
		// 2.3. Update current solution
		for (int j = 0; j < network.edges; j++)
			flows[j] = stepSize * target[j] + (1 - stepSize) * flows[j];
		
		// alternative expression better showing the step direction
		// flows[j] += stepSize * (target[j] - flows[j]);
		
		// 2.4 Update link costs
		updateCosts();
	}
	
	protected abstract double[] calculateTarget();
	
	protected abstract double calculateStepSize(double[] target);
}
