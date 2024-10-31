package black0ut1.sta.cost;

import black0ut1.data.Network;

public class BPR implements CostFunction {
	
	@Override
	public double function(Network.Edge edge, double flow) {
		double power = Math.pow(flow / edge.capacity, edge.beta);
		return edge.freeFlow * (1 + edge.alpha * power);
	}
	
	@Override
	public double derivative(Network.Edge edge, double flow) {
		double power = Math.pow(flow / edge.capacity, edge.beta - 1);
		return edge.alpha * edge.beta * edge.freeFlow * power / edge.capacity;
	}
	
	@Override
	public double integral(Network.Edge edge, double flow) {
		double power = Math.pow(flow / edge.capacity, edge.beta);
		return edge.freeFlow * flow * (1 + edge.alpha * power / (edge.beta + 1));
	}
}
