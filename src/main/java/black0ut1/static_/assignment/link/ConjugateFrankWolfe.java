package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.Util;

/**
 * Conjugate Frank-Wolfe algorithm differs from ordinary {@link FrankWolfe} by smarter
 * computation of target, for which it uses the previous target.
 * <p>
 * Bibliography:																		  <br>
 * - (Mitradjieva and Lindberg, 2013) The stiff is moving — conjugate direction
 * Frank-Wolfe methods with application to traffic assignment							  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2.3				  <br>
 */
public class ConjugateFrankWolfe extends FrankWolfe {
	
	protected static final double ALPHA_TOLERANCE = 0.1;
	
	protected double[] oldTarget;
	protected double oldStepSize;
	
	public ConjugateFrankWolfe(Network network, DoubleMatrix odMatrix,
							   CostFunction costFunction, int maxIterations,
							   Convergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	@Override
	protected double calculateStepSize(double[] newFlows) {
		double stepSize = super.calculateStepSize(newFlows);
		oldStepSize = stepSize;
		return stepSize;
	}
	
	@Override
	protected double[] calculateTarget() {
		if (iteration == 0 || oldStepSize == 1) {
			double[] newTarget = new double[network.edges];
			AON.assign(network, odMatrix, costs, newTarget);
			
			oldTarget = newTarget;
			return newTarget;
		}
		
		double[] newTarget = new double[network.edges];
		AON.assign(network, odMatrix, costs, newTarget);
		
		double numerator = 0;
		double denominator = 0;
		var edges = network.getEdges();
		for (int i = 0; i < network.edges; i++) {
			double a = costFunction.derivative(edges[i], flows[i]) * (oldTarget[i] - flows[i]);
			numerator += a * (newTarget[i] - flows[i]);
			denominator += a * (newTarget[i] - oldTarget[i]);
		}
		
		double alpha = (denominator == 0)
				? 0
				: Util.projectToInterval(numerator / denominator, 0, 1 - ALPHA_TOLERANCE);
		
		for (int i = 0; i < network.edges; i++)
			newTarget[i] = alpha * oldTarget[i] + (1 - alpha) * newTarget[i];
		
		oldTarget = newTarget;
		return newTarget;
	}
}
