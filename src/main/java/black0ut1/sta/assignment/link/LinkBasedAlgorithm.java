package black0ut1.sta.assignment.link;

import black0ut1.sta.assignment.STAAlgorithm;
import black0ut1.sta.assignment.AON;

public abstract class LinkBasedAlgorithm extends STAAlgorithm {
	
	public LinkBasedAlgorithm(STAAlgorithm.Parameters parameters) {
		super(parameters);
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
