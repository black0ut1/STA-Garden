package black0ut1.dynamic;

import black0ut1.dynamic.loading.link.Link;

public class Convergence {
	
	protected final TimeDependentODM odm;
	protected final double totalDemand;
	
	public Convergence(TimeDependentODM odm) {
		this.odm = odm;
		
		double totalDemand1 = 0;
		for (int origin = 0; origin < odm.zones; origin++)
			for (int destination = 0; destination < odm.zones; destination++)
				for (int t = 0; t < odm.timeSteps; t++)
					totalDemand1 += odm.getFlow(origin, destination, t);
		this.totalDemand = totalDemand1;
	}
	
	
	public double totalSystemTravelTime(DynamicNetwork network, double stepSize) {
		double tstt = 0;
		
		for (Link link : network.allLinks)
			for (int t = 0; t < link.cumulativeInflow.length - 1; t++) {
				double n1 = link.cumulativeInflow[t] - link.cumulativeOutflow[t];
				double n2 = link.cumulativeInflow[t + 1] - link.cumulativeOutflow[t + 1];
				tstt += (n1 + n2) / 2;
			}
		
		return tstt * stepSize;
	}
	
	public double shortestPathTravelTime(double[][][] costs) {
		double sptt = 0;
		
		for (int origin = 0; origin < odm.zones; origin++)
			for (int destination = 0; destination < odm.zones; destination++)
				for (int t = 0; t < odm.timeSteps; t++)
					sptt += costs[t][origin][destination] * odm.getFlow(origin, destination, t);
		
		return sptt;
	}
	
	public double averageExcessCost(double tstt, double sptt) {
		return (tstt - sptt) / totalDemand;
	}
}
