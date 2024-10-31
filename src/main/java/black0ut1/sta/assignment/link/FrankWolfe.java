package black0ut1.sta.assignment.link;

import black0ut1.data.Network;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.AON;
import black0ut1.util.Util;

public class FrankWolfe extends LinkBasedAlgorithm {
	
	protected static final int NEWTON_MAX_ITERATIONS = 100;
	protected static final double NEWTON_EPSILON = 1e-10;
	
	public FrankWolfe(Algorithm.Parameters parameters) {
		super(parameters);
	}
	
	@Override
	protected double[] calculateTarget() {
		double[] newTarget = new double[network.edges];
		AON.assign(network, odMatrix, costs, newTarget);
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
