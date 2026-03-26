package black0ut1.data.network;

/**
 * Pair of Alternative Segments.
 * A segment is a sequence of consecutive edges - essentially a
 * path, but connecting any two nodes, not just origin and destination.
 * A pair of alternative segments (PAS) is exactly what it means,
 * but with three conditions:
 * 1) both segments start in the same node - the tail,
 * 2) both segments end in the same node - the head,
 * 3) the segments do not share any edges and any nodes, apart from
 *    tail and head.
 * PAS also has an origin associated with it.
 * <p>
 * When PAS is created, one of the segments (minSegment) has lesser
 * cost than the other (maxSegment). However, these roles can switch
 * as the algorithm shift flows. That's why there is field minSegmentIndex
 * which point to the minSegment in array segments. To update the
 * roles, call updateSegments(), which computes the costs of both
 * segments and compares them to determine the roles.
 */
public class PAS {
	
	public int origin;
	private int minSegmentIndex = 0;
	private final int[][] segments;
	private final double[] costs;
	
	public PAS(int[] minSegment, int[] maxSegment, int origin) {
		this.origin = origin;
		this.segments = new int[2][];
		this.costs = new double[2];
		
		segments[minSegmentIndex] = minSegment;
		segments[1 - minSegmentIndex] = maxSegment;
	}
	
	public int[] minSegment() {
		return segments[minSegmentIndex];
	}
	
	public int[] maxSegment() {
		return segments[1 - minSegmentIndex];
	}
	
	public int minSegmentLastEdge() {
		return minSegment()[minSegment().length - 1];
	}
	
	public int maxSegmentLastEdge() {
		return maxSegment()[maxSegment().length - 1];
	}
	
	public double minSegmentFlowBound(Bush[] bushes) {
		Bush bush = bushes[origin];
		
		double flowBound = Double.POSITIVE_INFINITY;
		for (int i : minSegment())
			flowBound = Math.min(flowBound, bush.getEdgeFlow(i));
		
		return flowBound;
	}
	
	public double maxSegmentFlowBound(Bush[] bushes) {
		Bush bush = bushes[origin];
		
		double flowBound = Double.POSITIVE_INFINITY;
		for (int i : maxSegment())
			flowBound = Math.min(flowBound, bush.getEdgeFlow(i));
		
		return flowBound;
	}
	
	public void updateSegments(double[] costs) {
		this.costs[0] = 0;
		for (int i : segments[0]) {
			this.costs[0] += costs[i];
		}
		
		this.costs[1] = 0;
		for (int i : segments[1]) {
			this.costs[1] += costs[i];
		}
		
		minSegmentIndex = (this.costs[0] < this.costs[1]) ? 0 : 1;
	}
	
	public double segmentsCostDifference() {
		return costs[1 - minSegmentIndex] - costs[minSegmentIndex];
	}
	
	public int head(Network network) {
		return network.getEdges()[minSegmentLastEdge()].head;
	}
}
