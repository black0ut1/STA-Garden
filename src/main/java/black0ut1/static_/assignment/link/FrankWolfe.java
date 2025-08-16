package black0ut1.static_.assignment.link;

import black0ut1.data.network.Network;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.AON;
import black0ut1.util.Util;


/**
 * Frank-Wolfe algorithm. It is different from {@link MSA} in the calculation of step size. Here,
 * the step size is computed such that the move towards the target minimizes the Beckmann
 * function.
 * <p>
 * Bibliography:																		  <br>
 * - (Frank and Wolfe, 1956) An algorithm for quadratic programming						  <br>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2.2				  <br>
 */
public class FrankWolfe extends LinkBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	public FrankWolfe(Settings settings) {
		super(settings);
	}
	
	@Override
	protected double[] calculateTarget() {
		double[] newTarget = new double[network.edges];
		AON.assign(network, odm, costs, newTarget);
		return newTarget;
	}
	
	protected double calculateStepSize(double[] newFlows) {
		double lambda = .5;
		
		for (int j = 0; j < NEWTON_MAX_ITERATIONS; j++) {
			double numerator = 0;
			double denominator = 0;
			
			for (int i = 0; i < flows.length; i++) {
				Network.Edge edge = network.getEdges()[i];
				double a = lambda * newFlows[i] + (1 - lambda) * flows[i];
				
				double difference = newFlows[i] - flows[i];
				numerator += costFunction.function(edge, a) * difference;
				denominator += costFunction.derivative(edge, a) * difference * difference;
			}
			
			double newLamda = lambda - numerator / denominator;
			if (Math.abs(lambda - newLamda) < NEWTON_EPSILON) {
				lambda = newLamda;
				break;
			}
			
			lambda = newLamda;
		}
		
		return Util.projectToInterval(lambda, 0, 1);
	}
}
