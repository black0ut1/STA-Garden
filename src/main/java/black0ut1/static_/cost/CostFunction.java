package black0ut1.static_.cost;

import black0ut1.data.network.Network;

public interface CostFunction {
	
	double function(Network.Edge edge, double flow);
	
	double derivative(Network.Edge edge, double flow);
	
	double integral(Network.Edge edge, double flow);
}
