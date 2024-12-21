package black0ut1.sta;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.sta.assignment.STAAlgorithm;
import black0ut1.sta.assignment.AON;
import black0ut1.sta.cost.CostFunction;
import black0ut1.util.SSSP;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.Consumer;

public class Convergence {
	
	private final Network network;
	private final DoubleMatrix odMatrix;
	private final CostFunction costFunction;
	
	private final Map<Criterion, Double> criteria;
	private final Vector<double[]> data;
	private final Consumer<double[]> callback;
	
	private double maxLowerBound = Double.NEGATIVE_INFINITY;
	
	private boolean tsttNeedsCalculation = false;
	private boolean spttNeedsCalculation = false;
	private boolean gapNeedsCalculation = false;
	private boolean beckmannFunctionNeedsCalculation = false;
	
	private double totalFlow = 0;
	private double tstt = 0;
	private double sptt = 0;
	private double gap = 0;
	private double beckmannFunction = 0;
	
	private Convergence(STAAlgorithm.Parameters algorithmParameters,
						Map<Criterion, Double> criteria,
						Consumer<double[]> callback) {
		this.criteria = criteria;
		this.callback = callback;
		this.network = algorithmParameters.network;
		this.odMatrix = algorithmParameters.odMatrix;
		this.costFunction = algorithmParameters.costFunction;
		this.data = new Vector<>();
		
		if (criteria.containsKey(Criterion.AVERAGE_EXCESS_COST)) {
			for (int startZone = 0; startZone < network.zones; startZone++)
				for (int endZone = 0; endZone < network.zones; endZone++)
					totalFlow += odMatrix.get(startZone, endZone);
		}
		
		if (criteria.containsKey(Criterion.TOTAL_SYSTEM_TRAVEL_TIME)
				|| criteria.containsKey(Criterion.AVERAGE_EXCESS_COST)
				|| criteria.containsKey(Criterion.RELATIVE_GAP_1))
			tsttNeedsCalculation = true;
		
		if (criteria.containsKey(Criterion.AVERAGE_EXCESS_COST)
				|| criteria.containsKey(Criterion.RELATIVE_GAP_1))
			spttNeedsCalculation = true;
		
		if (criteria.containsKey(Criterion.RELATIVE_GAP_2)
				|| criteria.containsKey(Criterion.RELATIVE_GAP_3)
				|| criteria.containsKey(Criterion.BECKMANN_FUNCTION))
			beckmannFunctionNeedsCalculation = true;
		
		if (criteria.containsKey(Criterion.RELATIVE_GAP_2)
				|| criteria.containsKey(Criterion.RELATIVE_GAP_3)
				|| criteria.containsKey(Criterion.GAP))
			gapNeedsCalculation = true;
	}
	
	public boolean checkForConvergence() {
		double[] lastIterationData = data.getLast();
		
		for (Criterion criterion : criteria.keySet()) {
			if (criteria.get(criterion) == null)
				continue;
			
			double convergenceValue = criteria.get(criterion);
			if (lastIterationData[criterion.ordinal()] < convergenceValue)
				return false;
		}
		
		return true;
	}
	
	public void computeCriteria(double[] flows, double[] costs) {
		// precompute values that are common for multiple convergence criterions
		if (tsttNeedsCalculation)
			tstt = calculateTSTT(flows, costs);
		if (spttNeedsCalculation)
			sptt = calculateSPTT(costs);
		if (gapNeedsCalculation)
			gap = calculateGap(flows, costs);
		if (beckmannFunctionNeedsCalculation)
			beckmannFunction = calculateBeckmannFunction(flows);
		
		double[] iterationData = new double[Criterion.values().length];
		for (Criterion criterion : criteria.keySet())
			iterationData[criterion.ordinal()] = getCriterionValue(criterion);
		data.add(iterationData);
		
		if (callback != null)
			callback.accept(iterationData);
	}
	
	public void printCriteriaValues() {
		double[] lastIterationData = data.getLast();
		
		for (Criterion criterion : criteria.keySet()) {
			System.out.printf("%s: %.15f%n", criterion.name,
					lastIterationData[criterion.ordinal()]);
		}
	}
	
	public Vector<double[]> getData() {
		return data;
	}
	
	private double calculateSPTT(double[] costs) {
		double sum = 0;
		
		for (int startZone = 0; startZone < network.zones; startZone++) {
			double[] minDistance = SSSP.dijkstra(network, startZone, costs).second();
			
			for (int endZone = 0; endZone < network.zones; endZone++) {
				if (odMatrix.get(startZone, endZone) == 0)
					continue;
				
				sum += minDistance[endZone] * odMatrix.get(startZone, endZone);
			}
		}
		
		return sum;
	}
	
	private double calculateTSTT(double[] flows, double[] costs) {
		double sum = 0;
		
		for (int i = 0; i < network.edges; i++)
			sum += costs[i] * flows[i];
		
		return sum;
	}
	
	private double calculateGap(double[] flows, double[] costs) {
		double[] aonFlows = new double[network.edges];
		AON.assign(network, odMatrix, costs, aonFlows);
		
		double gap = 0;
		for (int i = 0; i < network.edges; i++)
			gap += costs[i] * (aonFlows[i] - flows[i]);
		
		return -gap;
	}
	
	private double calculateBeckmannFunction(double[] flows) {
		double sum = 0;
		
		Network.Edge[] edges = network.getEdges();
		for (int i = 0; i < edges.length; i++)
			sum += costFunction.integral(edges[i], flows[i]);
		
		return sum;
	}
	
	private double getCriterionValue(Criterion criterion) {
		return switch (criterion) {
			case BECKMANN_FUNCTION -> beckmannFunction;
			case TOTAL_SYSTEM_TRAVEL_TIME -> tstt;
			case GAP -> gap;
			case AVERAGE_EXCESS_COST -> (tstt - sptt) / totalFlow;
			case RELATIVE_GAP_1 -> tstt / sptt - 1;
			case RELATIVE_GAP_2 -> {
				double lowerBound = beckmannFunction - gap;
				yield (beckmannFunction - lowerBound) / lowerBound;
			}
			case RELATIVE_GAP_3 -> {
				double lowerBound = beckmannFunction - gap;
				if (lowerBound > maxLowerBound)
					maxLowerBound = lowerBound;
				
				yield (beckmannFunction - maxLowerBound) / maxLowerBound;
			}
			case null -> Double.NaN;
		};
	}
	
	public static class Builder {
		
		private final Map<Criterion, Double> criteria = new LinkedHashMap<>();
		
		private Consumer<double[]> callback = null;
		
		public Builder addCriterion(Criterion criterion, double convergenceValue) {
			criteria.put(criterion, convergenceValue);
			return this;
		}
		
		public Builder addCriterion(Criterion criterion) {
			criteria.put(criterion, null);
			return this;
		}
		
		public Builder setCallback(Consumer<double[]> callback) {
			this.callback = callback;
			return this;
		}
		
		public Convergence build(STAAlgorithm.Parameters algorithmParameters) {
			return new Convergence(algorithmParameters, criteria, callback);
		}
	}
	
	public enum Criterion {
		
		BECKMANN_FUNCTION("Beckmann function"),
		
		TOTAL_SYSTEM_TRAVEL_TIME("Total system travel time"),
		
		GAP("Gap"),
		
		/** Transport Network Analysis - p. 148, definition 6.10 */
		AVERAGE_EXCESS_COST("Average excess cost"),
		
		/** Transport Network Analysis - p. 147, definition 6.7 */
		RELATIVE_GAP_1("Relative gap 1"),
		
		/** Transport Network Analysis - p. 147, definition 6.8 */
		RELATIVE_GAP_2("Relative gap 2"),
		
		/** Transport Network Analysis - p. 147, definition 6.9 */
		RELATIVE_GAP_3("Relative gap 3"),
		;
		
		public final String name;
		
		Criterion(String name) {
			this.name = name;
		}
	}
}
