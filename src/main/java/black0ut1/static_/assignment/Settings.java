package black0ut1.static_.assignment;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.assignment.path.PathBasedAlgorithm;
import black0ut1.static_.cost.BPR;
import black0ut1.static_.cost.CostFunction;

public class Settings {
	
	public final Network network;
	public final DoubleMatrix odm;
	public final int maxIterations;
	public final Convergence.Builder convergenceBuilder;
	
	public CostFunction costFunction = new BPR();
	
	public int NEWTON_MAX_ITERATIONS = 100;
	public double NEWTON_EPSILON = 1e-10;
	
	public double CONJUGATE_FW_ALPHA_TOLERANCE = 0.01;
	
	public int FUKUSHIMA_FW_L = 2;
	
	public int SD_INNER_ITERATIONS = 50;
	
	public ShortestPathStrategy SHORTEST_PATH_STRATEGY = ShortestPathStrategy.SSSP;
	
	public Settings(Network network, DoubleMatrix odm, int maxIterations,
					Convergence.Builder convergenceBuilder) {
		this.network = network;
		this.odm = odm;
		this.maxIterations = maxIterations;
		this.convergenceBuilder = convergenceBuilder;
	}
	
	public enum ShortestPathStrategy {
		P2PSP, SSSP,
	}
}
