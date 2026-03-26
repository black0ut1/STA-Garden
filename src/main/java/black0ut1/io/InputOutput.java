package black0ut1.io;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;

import java.util.List;

/**
 * This abstract class defines the common interface for classes that load traffic data
 * (like OD matrices and networks) from different formats (e.g. the TNTP or CSV format).
 */
public abstract class InputOutput {
	
	//////////////////// Reading ////////////////////
	
	public Network parseNetwork(String networkFile, String nodesFile, int zones) {
		var edgesArray = readEdges(networkFile);
		
		int nodes = edgesArray.stream()
				.mapToInt(edge -> Math.max(edge.head, edge.tail))
				.max()
				.getAsInt() + 1;
		
		Network.Node[] nodesArray = (nodesFile == null)
				? null
				: readNodes(nodesFile, nodes);
		
		return new Network(edgesArray, nodesArray, nodes, zones);
	}
	
	protected abstract List<Network.Edge> readEdges(String netFile);
	
	protected abstract Network.Node[] readNodes(String nodeFile, int nodesNum);
	
	public abstract DoubleMatrix parseODMatrix(String odmFile);
	
	//////////////////// Writing ////////////////////
	
	public abstract void writeFlows(String outputFile, Network network, double[] flows, double[] costs);
	
	public abstract void writeODmatrix(String outputFile, DoubleMatrix ODMatrix);
}
