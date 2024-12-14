package black0ut1.sta.cost;

import black0ut1.data.network.Network;

public class CanonicalBPR implements CostFunction {
	
	@Override
	public double function(Network.Edge edge, double flow) {
		double ratio = flow / edge.capacity;
		double fourthPow = ratio * ratio * ratio * ratio;
		return edge.freeFlow * (1 + 0.15 * fourthPow);
	}
	
	@Override
	public double derivative(Network.Edge edge, double flow) {
		double a = flow / edge.capacity;
		double b = a * a * a;
		return 0.6 * edge.freeFlow * b / edge.capacity;
	}
	
	@Override
	public double integral(Network.Edge edge, double flow) {
		double b = flow / edge.capacity;
		double power = b * b * b * b;
		return edge.freeFlow * flow * (1 + 0.03 * power);
	}
}
