package black0ut1.io;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Network;
import black0ut1.data.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

@SuppressWarnings("unchecked")
public final class TNTP {
	
	private final static String COMMENT_SIGN = "~";
	private final static String HEADER_END = "<END OF METADATA>";
	private final static String ZONES_NUMBER = "<NUMBER OF ZONES>";
	private final static String NODES_NUMBER = "<NUMBER OF NODES>";
	
	//////////////////// Reading ////////////////////
	
	public static Network parseNetwork(String netFile) {
		var pair = readAdjacencyList(netFile);
		return new Network(pair.first(), pair.second());
	}
	
	public static Network parseNetwork(String netFile, String nodeFile) {
		var pair = readAdjacencyList(netFile);
		Network.Node[] nodes = readNodes(nodeFile, pair.first().length);
		return new Network(pair.first(), pair.second(), nodes);
	}
	
	public static DoubleMatrix parseODMatrix(String path) {
		try (var reader = new BufferedReader(new FileReader(path))) {
			var header = parseHeader(reader);
			
			int zonesNumber = Integer.parseInt(header.get(ZONES_NUMBER));
			DoubleMatrix odMatrix = new DoubleMatrix(zonesNumber);
			
			int fromNode = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				line = sanitize(line);
				if (line.isEmpty())
					continue;
				
				if (line.contains("Origin"))
					fromNode = Integer.parseInt(line.replace("Origin", "").trim()) - 1;
				else {
					
					String[] split = line.replaceAll("[ \t]", "").split(";");
					for (String s : split) {
						
						String[] pair = s.split(":");
						int toNode = Integer.parseInt(pair[0]) - 1;
						double demand = Double.parseDouble(pair[1]);
						odMatrix.set(fromNode, toNode, demand);
					}
				}
			}
			
			return odMatrix;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Pair<Vector<Network.Edge>[], Integer> readAdjacencyList(String netFile) {
		try (var reader = new BufferedReader(new FileReader(netFile))) {
			int zeroFreeFlowEdges = 0;
			
			var header = parseHeader(reader);
			
			int nodes = Integer.parseInt(header.get(NODES_NUMBER));
			int zones = Integer.parseInt(header.get(ZONES_NUMBER));
			
			Vector<Network.Edge>[] adjacencyList = new Vector[nodes];
			for (int i = 0; i < adjacencyList.length; i++)
				adjacencyList[i] = new Vector<>();
			
			String line;
			while ((line = reader.readLine()) != null) {
				line = sanitize(line);
				if (line.isEmpty())
					continue;
				
				String[] split = line.split("[ \t]+");
				
				int fromNode = Integer.parseInt(split[0]) - 1;
				int endNode = Integer.parseInt(split[1]) - 1;
				double capacity = Double.parseDouble(split[2]);
				double freeFlow = Double.parseDouble(split[4]);
				double alpha = Double.parseDouble(split[5]);
				double beta = Double.parseDouble(split[6]);
				
				if (freeFlow == 0) {
					freeFlow = .0001;
					zeroFreeFlowEdges++;
				}
				
				adjacencyList[fromNode].add(new Network.Edge(
						fromNode, endNode, capacity, freeFlow, alpha, beta
				));
			}
			
			if (zeroFreeFlowEdges > 0) {
				System.out.println("Warning! Found " + zeroFreeFlowEdges +
						" edges with zero free flow, setting the free flow to 0.0001");
			}
			
			return new Pair<>(adjacencyList, zones);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Network.Node[] readNodes(String nodeFile, int nodesNum) {
		try (var reader = new BufferedReader(new FileReader(nodeFile))) {
			Network.Node[] nodes = new Network.Node[nodesNum];
			
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				line = sanitize(line);
				if (line.isEmpty())
					continue;
				
				String[] split = line.split("[ \t]+");
				
				int node = Integer.parseInt(split[0]) - 1;
				double x = Double.parseDouble(split[1]);
				double y = Double.parseDouble(split[2]);
				
				nodes[node] = new Network.Node(node, x, y);
			}
			
			return nodes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Map<String, String> parseHeader(BufferedReader reader) throws IOException {
		HashMap<String, String> header = new HashMap<>();
		
		while (true) {
			String line = sanitize(reader.readLine());
			
			if (line.equals(HEADER_END))
				break;
			
			if (line.isEmpty())
				continue;
			
			String[] split = line.split(">");
			header.put((split[0] + '>').trim(),
					split.length == 1 ? null : split[1].trim());
		}
		
		return header;
	}
	
	private static String sanitize(String line) {
		return line.split(COMMENT_SIGN)[0].trim();
	}
	
	//////////////////// Writing ////////////////////
	
	public static void writeFlows(String outputFile, Network network, double[] flows, double[] costs) {
		try (BufferedWriter bfw = new BufferedWriter(new FileWriter(outputFile))) {
			
			bfw.write("From\tTo\tVolume\tCost\n");
			for (Network.Edge edge : network.getEdges()) {
				bfw.write((edge.startNode + 1) + "\t"
						+ (edge.endNode + 1) + "\t"
						+ String.format(Locale.ROOT, "%.15f", flows[edge.index]) + "\t"
						+ String.format(Locale.ROOT, "%.15f", costs[edge.index]) + "\n");
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeODmatrix(String outputFile, DoubleMatrix ODMatrix) {
		try (BufferedWriter bfw = new BufferedWriter(new FileWriter(outputFile))) {
			
			for (int i = 0; i < ODMatrix.n; i++) {
				bfw.write("\n\nOrigin\t" + (i + 1));
				
				int c = 0;
				for (int j = 0; j < ODMatrix.n; j++) {
					if (ODMatrix.get(i, j) == 0)
						continue;
					
					if (c % 5 == 0)
						bfw.write("\n");
					
					bfw.write("\t" + (j + 1) + "\t:\t" + ODMatrix.get(i, j) + ";");
					c++;
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
