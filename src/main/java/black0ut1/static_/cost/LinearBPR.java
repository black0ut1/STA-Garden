package black0ut1.static_.cost;

import black0ut1.data.network.Network;

public class LinearBPR implements CostFunction {
	
	@Override
	public double function(Network.Edge edge, double flow) {
		return edge.freeFlow * (1 + edge.alpha * flow / edge.capacity);
	}
	
	@Override
	public double derivative(Network.Edge edge, double flow) {
		return edge.freeFlow * edge.alpha / edge.capacity;
	}
	
	@Override
	public double integral(Network.Edge edge, double flow) {
		return edge.freeFlow * flow * (1 + edge.alpha * flow / (edge.capacity * 2));
	}
}
