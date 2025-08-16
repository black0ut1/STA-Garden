package black0ut1.static_.assignment;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.static_.cost.BPR;
import black0ut1.static_.cost.CostFunction;

public class Settings {
	
	public final Network network;
	public final DoubleMatrix odm;
	public final int maxIterations;
	public final Convergence.Builder convergenceBuilder;
	
	public CostFunction costFunction = new BPR();
	
	public Settings(Network network, DoubleMatrix odm, int maxIterations,
					Convergence.Builder convergenceBuilder) {
		this.network = network;
		this.odm = odm;
		this.maxIterations = maxIterations;
		this.convergenceBuilder = convergenceBuilder;
	}
}
