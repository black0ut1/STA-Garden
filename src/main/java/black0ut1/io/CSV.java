package black0ut1.io;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.RoutedIntersection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class CSV extends InputOutput {
	
	protected final static String DELIMITER = ",";
	
	protected final static String TAIL_COLUMN = "from_node_id";
	protected final static String HEAD_COLUMN = "to_node_id";
	protected final static String CAPACITY_COLUMN = "capacity";
	protected final static String LENGTH_COLUMN = "vdf_length_mi";
	protected final static String ALPHA_COLUMN = "vdf_alpha";
	protected final static String BETA_COLUMN = "vdf_beta";
	protected final static String FREE_FLOW_TIME_COLUMN = "vdf_fftt";
	
	@Override
	protected List<Network.Edge> readEdges(String networkFile) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(networkFile))) {
			var header = parseCsvHeader(reader);
			
			int zeroFreeFlowEdges = 0;
			
			Vector<Network.Edge> edges = new Vector<>();
			
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(DELIMITER);
				
				double freeFlow = Double.parseDouble(split[header.get(FREE_FLOW_TIME_COLUMN)]);
				if (freeFlow == 0) {
					zeroFreeFlowEdges++;
					freeFlow = 0.0001;
				}
				
				int tail = Integer.parseInt(split[header.get(TAIL_COLUMN)]) - 1;
				int head = Integer.parseInt(split[header.get(HEAD_COLUMN)]) - 1;
				double capacity = Double.parseDouble(split[header.get(CAPACITY_COLUMN)]);
				double length = Double.parseDouble(split[header.get(LENGTH_COLUMN)]);
				double alpha = Double.parseDouble(split[header.get(ALPHA_COLUMN)]);
				double beta = Double.parseDouble(split[header.get(BETA_COLUMN)]);
				edges.add(new Network.Edge(tail, head, capacity, length, freeFlow, alpha, beta));
			}
			
			if (zeroFreeFlowEdges > 0)
				System.out.println("Warning! Found " + zeroFreeFlowEdges + " edges with zero free flow.");
			
			return edges;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Map<String, Integer> parseCsvHeader(BufferedReader reader) throws IOException {
		String header = reader.readLine();
		String[] split = header.split(DELIMITER);
		
		HashMap<String, Integer> result = new HashMap<>();
		for (int i = 0; i < split.length; i++)
			result.put(split[i], i);
		
		return result;
	}
	
	@Override
	protected Network.Node[] readNodes(String nodeFile, int nodesNum) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(nodeFile))) {
			Network.Node[] nodes = new Network.Node[nodesNum];
			
			reader.lines()
					.skip(1)
					.map(line -> line.split(DELIMITER))
					.forEach(arr -> {
						int nodeIndex = Integer.parseInt(arr[1]) - 1;
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
						double demand = Double.parseDouble(arr[2]);
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
	
	public void writeCumulativeFlows(String outputFile, DynamicNetwork network, int stepsTaken) {
		try (BufferedWriter bfw = new BufferedWriter(new FileWriter(outputFile))) {
			
			for (Link link : network.links) {
				int fromNodeId = link.tail.index + 1;
				int toNodeId = link.head.index + 1;
				
				bfw.write("Link " + fromNodeId + " " + toNodeId + "\n");
				for (int t = 0; t < link.cumulativeInflow.length; t++) {
					double cinflow = (t <= stepsTaken)
							? link.cumulativeInflow[t]
							: link.cumulativeInflow[stepsTaken]; // last defined value
					double coutflow = (t <= stepsTaken)
							? link.cumulativeOutflow[t]
							: link.cumulativeOutflow[stepsTaken]; // last defined value
					
					bfw.write(t + " " +  cinflow + " " + coutflow + "\n");
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeTurningFractions(String outputFile, DynamicNetwork network) {
		try (BufferedWriter bfw = new BufferedWriter(new FileWriter(outputFile))) {
			
			for (RoutedIntersection intersection : network.routedIntersections) {
				int node = intersection.index + 1;
				
				bfw.write("N " + node + "\n");
				
				var mfs = intersection.getTurningFractions();
				for (int t = 0; t < mfs.length; t++) {
					var mf = mfs[t];
					
					bfw.write("T " + t + "\n");
					
					for (int d = 0; d < mf.destinations.length; d++) {
						int destination = mf.destinations[d];
						DoubleMatrix fractions = mf.destinationTurningFractions[d];
						
						bfw.write("D " + destination + "\n");
						
						for (int i = 0; i < fractions.m; i++) {
							int fromNode = intersection.incomingLinks[i].tail.index + 1;
							
							for (int j = 0; j < fractions.n; j++) {
								int toNode = intersection.outgoingLinks[j].head.index + 1;
								double fraction = fractions.get(i, j);
								
								if (fraction == 0)
									continue;
								
								if (fraction == 1)
									bfw.write(fromNode + " " + toNode + " 1\n");
								else
									bfw.write(fromNode + " " + toNode + " " + fraction + "\n");
							}
						}
					}
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
