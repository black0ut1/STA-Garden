package black0ut1.static_.assignment.link;

import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.AON;

/**
 * Similar to {@link ConjugateFrankWolfe} in its smarter computation of target, but it
 * uses last two previous targets.
 * <p>
 * Bibliography:																		  <br>
 * - (Mitradjieva and Lindberg, 2013) The stiff is moving — conjugate direction
 * Frank-Wolfe methods with application to traffic assignment							  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2.3				  <br>
 */
public class BiconjugateFrankWolfe extends FrankWolfe {
	
	protected double[] oldTarget;
	protected double[] oldOldTarget;
	protected double oldStepSize;
	protected double oldOldStepSize;
	
	public BiconjugateFrankWolfe(Settings settings) {
		super(settings);
	}
	
	
	@Override
	protected double calculateStepSize(double[] newFlows) {
		double stepSize = super.calculateStepSize(newFlows);
		oldOldStepSize = oldStepSize;
		oldStepSize = stepSize;
		return stepSize;
	}
	
	@Override
	protected double[] calculateTarget() {
		if (iteration < 2 || oldStepSize == 1 || oldOldStepSize == 1) {
			double[] newTarget = new double[network.edges];
			AON.assign(network, odm, costs, newTarget);
			
			oldOldTarget = oldTarget;
			oldTarget = newTarget;
			return newTarget;
		}
		
		double[] newTarget = new double[network.edges];
		AON.assign(network, odm, costs, newTarget);
		
		double numerator = 0;
		double denominator = 0;
		var edges = network.getEdges();
		for (int i = 0; i < network.edges; i++) {
			double a = s.costFunction.derivative(edges[i], flows[i]) *
					(oldStepSize * oldTarget[i] - flows[i] + (1 - oldStepSize) * oldOldTarget[i]);
			numerator += a * (newTarget[i] - flows[i]);
			denominator += a * (oldOldTarget[i] - oldTarget[i]);
		}
		
		if (denominator == 0)
			return newTarget;
		
		double mu = -numerator / denominator;
		if (mu < 0)
			mu = 0;
		
		numerator = 0;
		denominator = 0;
		for (int i = 0; i < network.edges; i++) {
			double a = s.costFunction.derivative(edges[i], flows[i]) * (oldTarget[i] - flows[i]);
			numerator += a * (newTarget[i] - flows[i]);
			denominator += a * (oldTarget[i] - flows[i]);
		}
		
		if (denominator == 0)
			return newTarget;
		
		double nu = mu * oldStepSize / (1 - oldStepSize) - numerator / denominator;
		if (nu < 0)
			nu = 0;
		
		double beta0 = 1 / (1 + nu + mu);
		double beta1 = nu * beta0;
		double beta2 = mu * beta0;
		
		for (int i = 0; i < network.edges; i++) {
			newTarget[i] = beta0 * newTarget[i] +
					beta1 * oldTarget[i] +
					beta2 * oldOldTarget[i];
		}
		
		oldOldTarget = oldTarget;
		oldTarget = newTarget;
		return newTarget;
	}
}
