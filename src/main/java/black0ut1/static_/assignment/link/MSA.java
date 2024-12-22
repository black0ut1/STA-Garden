package black0ut1.static_.assignment.link;

import black0ut1.static_.assignment.STAAlgorithm;
import black0ut1.static_.assignment.AON;

public class MSA extends LinkBasedAlgorithm {
	
	protected int i = 2;
	
	public MSA(STAAlgorithm.Parameters parameters) {
		super(parameters);
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
