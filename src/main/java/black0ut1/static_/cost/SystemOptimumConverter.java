package black0ut1.static_.cost;

import black0ut1.data.network.Network;

public class SystemOptimumConverter implements CostFunction {
	
	private final CostFunction original;
	
	public SystemOptimumConverter(CostFunction original) {
		this.original = original;
	}
	
	@Override
	public double function(Network.Edge edge, double flow) {
		double functionValue = original.function(edge, flow);
		double derivativeValue = original.derivative(edge, flow);
		return functionValue + derivativeValue * flow;
	}
	
	@Override
	public double derivative(Network.Edge edge, double flow) {
		final double h = 1e-5;
		double derivativeValue = original.derivative(edge, flow);
		
		double derivativeValue1 = original.derivative(edge, flow + h);
		double derivativeValue2 = original.derivative(edge, flow - h);
		double secondDerivativeValue = (derivativeValue1 - derivativeValue2) / (2 * h);
		
		return 2 * derivativeValue + secondDerivativeValue * flow;
	}
	
	@Override
	public double integral(Network.Edge edge, double flow) {
		double functionValue = original.function(edge, flow);
		double integralValue = original.integral(edge, flow);
		return functionValue * flow + integralValue;
	}
}
