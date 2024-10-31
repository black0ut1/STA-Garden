package black0ut1.sta.assignment;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Network;
import black0ut1.sta.Convergence;
import black0ut1.sta.cost.CostFunction;

public abstract class Algorithm {
	
	protected final Network network;
	protected final DoubleMatrix odMatrix;
	protected final CostFunction costFunction;
	protected final int maxIterations;
	protected final Convergence convergence;
	
	protected int iteration = 0;
	
	protected final double[] flows;
	protected final double[] costs;
	
	public Algorithm(Parameters algorithmParameters) {
		this.network = algorithmParameters.network;
		this.odMatrix = algorithmParameters.odMatrix;
		this.costFunction = algorithmParameters.costFunction;
		this.maxIterations = algorithmParameters.maxIterations;
		this.convergence = algorithmParameters.convergence;
		this.flows = new double[network.edges];
		this.costs = new double[network.edges];
	}
	
	public void run() {
		updateCosts();
		init();
		
		System.out.println("===================================");
		System.out.println("Algorithm: " + this.getClass().getSimpleName());
		System.out.println("Max. iterations: " + maxIterations);
		convergence.computeCriteria(flows, costs);
		convergence.printCriteriaValues();
		System.out.println("===================================");
		
		while (convergence.checkForConvergence() && iteration < maxIterations) {
			System.out.println("Iteration " + iteration);
			
			updateFlows();
			
			convergence.computeCriteria(flows, costs);
			convergence.printCriteriaValues();
			
			System.out.println("-----------------------------------");
			iteration++;
		}
		
		cleanUp();
	}
	
	protected abstract void init();
	
	protected abstract void updateFlows();
	
	protected void cleanUp() {}
	
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
	
	public static class Parameters {
		
		public final Network network;
		public final DoubleMatrix odMatrix;
		public final CostFunction costFunction;
		public final int maxIterations;
		public final Convergence convergence;
		
		public Parameters(Network network, DoubleMatrix odMatrix, CostFunction costFunction,
			int maxIterations, Convergence.Builder convergenceBuilder) {
			this.network = network;
			this.odMatrix = odMatrix;
			this.costFunction = costFunction;
			this.maxIterations = maxIterations;
			this.convergence = convergenceBuilder.build(this);
		}
	}
}
