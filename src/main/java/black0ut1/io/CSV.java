package black0ut1.io;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CSV extends InputOutput {
	
	private final static String DELIMITER = ",";
	
	@Override
	protected List<Network.Edge> readEdges(String networkFile) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(networkFile))) {
			AtomicInteger zeroFreeFlowEdges = new AtomicInteger();
			
			List<Network.Edge> edges = reader.lines()
					.skip(1)
					.map(line -> line.split(DELIMITER))
					.map(arr -> {
						double freeFlow = Double.parseDouble(arr[16]);
						if (freeFlow == 0) {
							zeroFreeFlowEdges.getAndIncrement();
							freeFlow = 0.0001;
						}
						
						return new Network.Edge(
								Integer.parseInt(arr[2]) - 1,
								Integer.parseInt(arr[3]) - 1,
								Double.parseDouble(arr[8]),
								Double.parseDouble(arr[5]),
								freeFlow,
								Double.parseDouble(arr[12]),
								Double.parseDouble(arr[13]));
					})
					.toList();
			
			if (zeroFreeFlowEdges.get() > 0)
				System.out.println("Warning! Found " + zeroFreeFlowEdges.get() + " edges with zero free flow.");
			
			return edges;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Network.Node[] readNodes(String nodeFile, int nodesNum) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(nodeFile))) {
			Network.Node[] nodes = new Network.Node[nodesNum];
			
			reader.lines()
					.skip(1)
					.map(line -> line.split(DELIMITER))
					.forEach(arr -> {
						int nodeIndex =  Integer.parseInt(arr[1]) - 1;
						double x = Double.parseDouble(arr[3]);
						double y = Double.parseDouble(arr[4]);
						nodes[nodeIndex] = new Network.Node(nodeIndex, x, y);
					});
			
			return nodes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public DoubleMatrix parseODMatrix(String odmFile) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(odmFile))) {
			var odPairs = reader.lines()
					.skip(1)
					.map(line -> line.split(DELIMITER))
					.map(arr -> {
						int origin = Integer.parseInt(arr[0]) - 1;
						int destination = Integer.parseInt(arr[1]) - 1;
						double demand =  Double.parseDouble(arr[2]);
						return new Object[]{origin, destination, demand};
					})
					.toList();
			
			int zones = odPairs.stream()
					.mapToInt(arr -> Math.max((int) arr[0], (int) arr[1]))
					.max()
					.getAsInt() + 1;
			
			DoubleMatrix ODM = new DoubleMatrix(zones);
			
			for (Object[] odPair : odPairs)
				ODM.set((int) odPair[0], (int) odPair[1], (double) odPair[2]);
			
			return ODM;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void writeFlows(String outputFile, Network network, double[] flows, double[] costs) {}
	
	@Override
	public void writeODmatrix(String outputFile, DoubleMatrix ODMatrix) {}
}
