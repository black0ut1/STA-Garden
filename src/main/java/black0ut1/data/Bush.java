package black0ut1.data;

public class Bush {
	
	private final boolean[] edgeFlags;
	
	private final double[] flows;
	
	public final int root;
	
	public Bush(int arcsNum, int root) {
		this.edgeFlags = new boolean[arcsNum];
		this.flows = new double[arcsNum];
		this.root = root;
	}
	
	public void addEdge(int edgeIndex) {
		edgeFlags[edgeIndex] = true;
	}
	
	public void removeEdge(int edgeIndex) {
		edgeFlags[edgeIndex] = false;
	}

	public boolean edgeExists(int edgeIndex) {
		return edgeFlags[edgeIndex];
	}

	public double getEdgeFlow(int edgeIndex) {
		return flows[edgeIndex];
	}

	public void addFlow(int edgeIndex, double flow) {
		flows[edgeIndex] += flow;
	}
}
