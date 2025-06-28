package black0ut1.static_.assignment;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.cost.CostFunction;

/**
 * Base class for all static traffic assignment algorithms. The common parts of such
 * algorithms are: the traffic network, origin-destination matrix, cost function, maximum
 * number of iterations and convergence criteria.
 * <p>
 * The common process of STA algorithms is:
 * 1. Initialize (flows, paths, bushes etc.)
 * 2. While convergence criteria are not met, iterate
 * 3. Post-process
 */
public abstract class Algorithm {
	
	protected final Network network;
	protected final DoubleMatrix odMatrix;
	protected final CostFunction costFunction;
	protected final int maxIterations;
	protected final Convergence convergence;
	
	protected int iteration = 0;
	
	protected final double[] flows;
	protected final double[] costs;
	
	public Algorithm(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
					 int maxIterations, Convergence.Builder convergenceBuilder) {
		this.network = network;
		this.odMatrix = odMatrix;
		this.costFunction = costFunction;
		this.maxIterations = maxIterations;
		this.convergence = convergenceBuilder.build(network, odMatrix, costFunction);
		
		this.flows = new double[network.edges];
		this.costs = new double[network.edges];
		updateCosts();
	}
	
	public void assignFlows() {
		initialize();
		
		System.out.println("===================================");
		System.out.println("STA Algorithm: " + this.getClass().getSimpleName());
		System.out.println("Max. iterations: " + maxIterations);
		convergence.computeCriteria(flows, costs);
		convergence.printCriteriaValues();
		System.out.println("===================================");
		
		while (convergence.checkForConvergence() && iteration < maxIterations) {
			System.out.println("Iteration " + (iteration + 1));
			
			mainLoopIteration();
			
			convergence.computeCriteria(flows, costs);
			convergence.printCriteriaValues();
			System.out.println("-----------------------------------");
			iteration++;
		}
		
		postProcess();
	}
	
	protected abstract void initialize();
	
	protected abstract void mainLoopIteration();
	
	protected void postProcess() {
		convergence.close();
	}
	
	public double[] getFlows() {
		return flows;
	}
	
	public double[] getCosts() {
		return costs;
	}
	
	protected void updateCosts() {
		for (int i = 0; i < network.edges; i++)
			costs[i] = costFunction.function(network.getEdges()[i], flows[i]);
	}
}
