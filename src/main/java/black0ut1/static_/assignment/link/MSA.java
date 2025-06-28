package black0ut1.static_.assignment.link;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.AON;
import black0ut1.static_.assignment.STAConvergence;
import black0ut1.static_.cost.CostFunction;

public class MSA extends LinkBasedAlgorithm {
	
	protected int i = 2;
	
	public MSA(Network network, DoubleMatrix odMatrix,
			   CostFunction costFunction, int maxIterations,
			   STAConvergence.Builder convergenceBuilder) {
		super(network, odMatrix, costFunction, maxIterations, convergenceBuilder);
	}
	
	
	@Override
	protected double[] calculateTarget() {
		double[] newTarget = new double[network.edges];
		AON.assign(network, odMatrix, costs, newTarget);
		return newTarget;
	}
	
	@Override
	protected double calculateStepSize(double[] target) {
		return 1.0 / i++;
	}
}
