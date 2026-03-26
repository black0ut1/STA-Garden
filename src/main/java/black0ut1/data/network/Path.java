package black0ut1.data.network;

import java.util.Arrays;

public class Path {
	
	public final int[] edges;
	public double flow;
	
	public Path(int[] edges) {
		this.edges = edges;
	}
	
	public double getCost(double[] costs) {
		double cost = 0;
		for (int edge : edges)
			cost += costs[edge];
		return cost;
	}
	
	@Override
	public boolean equals(Object obj) {
		Path other = (Path) obj;
		
		return Arrays.equals(edges, other.edges);
	}
}
