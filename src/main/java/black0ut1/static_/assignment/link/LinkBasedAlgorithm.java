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
 * 1. Generate initial solution															  <br>
 * 2. Generate target solution															  <br>
 * 3. Determine a step size towards the target solution									  <br>
 * 4. Update current solution															  <br>
 * 5. Update link costs																	  <br>
 * 6. Go to 2.																			  <br>
 * <p>
 * Bibliography:																		  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2
 */
public abstract class LinkBasedAlgorithm extends Algorithm {
	
	public LinkBasedAlgorithm(Network network, DoubleMatrix odMatrix,
							  CostFunction costFunction, int maxIterations,
							  Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected void initialize() {
		// 1. Generate initial solution
		AON.assign(network, odMatrix, costs, flows);
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		// 2. Generate target solution
		double[] target = calculateTarget();
		
		// 3. Determine a step size towards the target solution
		double stepSize = calculateStepSize(target);
		
		// 4. Update current solution
		for (int j = 0; j < network.edges; j++)
			flows[j] = stepSize * target[j] + (1 - stepSize) * flows[j];
		
		// alternate expression better showing the step direction
		// flows[j] += stepSize * (target[j] - flows[j]);
		
		// 5. Update link costs
		updateCosts();
	}
	
	protected abstract double[] calculateTarget();
	
	protected abstract double calculateStepSize(double[] target);
}
