package black0ut1.static_.assignment.link;

import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.AON;

/**
 * Method of successive averages - the simplest of STA algorithms. The target is just a
 * new AON assignment w.r.t. current costs. The steps size is a fixed sequence dependent
 * on iteration. However there are two conditions put upon this sequence to guarantee
 * convergence:
 * 1. the sum of its elements must diverge,												  <br>
 * 2. the sum of squares of its elements must converge.									  <br>
 * The sequence used here is {@code stepSize = 1 / (iteration + 2)} and it satisfies these
 * conditions.
 * <p>
 * - (Boyles et al., 2025) Transportation Network Analysis, Section 6.2.1				  <br>
 */
public class MSA extends LinkBasedAlgorithm {
	
	public MSA(Settings settings) {
		super(settings);
	}
	
	
	@Override
	protected double[] calculateTarget() {
		double[] newTarget = new double[network.edges];
		AON.assign(network, odm, costs, newTarget);
		return newTarget;
	}
	
	@Override
	protected double calculateStepSize(double[] target) {
		return 1.0 / (iteration + 2);
	}
}
