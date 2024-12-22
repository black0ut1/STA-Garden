package black0ut1.static_.entropy;

import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Triplet;
import black0ut1.static_.assignment.bush.iTAPAS;
import black0ut1.util.SSSP;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class piTAPAS extends iTAPAS implements ProportionalityAlgorithm {
	
	private static final double POSTPROCESS_COST_EPSILON = 1e-10;
	
	private final int maxPostprocessIterations;
	private final double minProportionGap;
	
	protected double[][] nodeFlows, approachProportions;
	
	public piTAPAS(Parameters parameters, int maxPostprocessIterations, double minProportionGap) {
		super(parameters);
		this.maxPostprocessIterations = maxPostprocessIterations;
		this.minProportionGap = minProportionGap;
	}
	
	@Override
	public void proportionalizeFlows() {
		calculateNodeFlowAndApproachProportions();
		
		LinkedHashMap<PAS, int[]> pasSet = new LinkedHashMap<>();
		for (int i = 0; i < manager.getCountP(); i++) {
			PAS pas = manager.getPASes()[i];
			
			int[] origins = collectOrigins(pas);
			if (origins.length > 1)
				pasSet.put(pas, origins);
		}
		
		System.out.println("===================================");
		System.out.println("iTAPAS proportionality postprocessing");
		System.out.printf("Entropy: %.15f%n", calculateEntropy(nodeFlows));
		double averageProportionGap = Double.POSITIVE_INFINITY;
		System.out.printf("Average proportion gap: %.15f%n", averageProportionGap);
		System.out.println("===================================");
		
		for (int j = 0; j < maxPostprocessIterations && minProportionGap < averageProportionGap; j++) {
			System.out.println("Iteration " + (j + 1));
			System.out.println("No. of PASes: " + pasSet.size());
			
			addPASes(pasSet);
			
			for (Map.Entry<PAS, int[]> entry : pasSet.entrySet()) {
				PAS pas = entry.getKey();
				int[] origins = entry.getValue();
				
				shiftFlowsProportionally(pas, origins);
			}
			
			System.out.printf("Entropy: %.15f%n", calculateEntropy(nodeFlows));
			averageProportionGap = calculateAverageProportionGap(pasSet);
			System.out.printf("Average proportion gap: %.15f%n", averageProportionGap);
			System.out.println("-----------------------------------");
		}
	}
	
	protected void calculateNodeFlowAndApproachProportions() {
		nodeFlows = new double[network.zones][];
		approachProportions = new double[network.zones][];
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			// calculate origin-based node flow for each node
			double[] nodeFlow = new double[network.nodes];
			for (Network.Edge edge : network.getEdges()) {
				nodeFlow[edge.endNode] += bush.getEdgeFlow(edge.index);
			}
			nodeFlows[origin] = nodeFlow;
			
			// calculate origin-based approach proportions for each link
			double[] approachProportion = new double[network.edges];
			for (Network.Edge edge : network.getEdges()) {
				approachProportion[edge.index] = (nodeFlow[edge.endNode] == 0)
						? 0
						: bush.getEdgeFlow(edge.index) / nodeFlow[edge.endNode];
			}
			approachProportions[origin] = approachProportion;
		}
	}
	
	protected void addPASes(LinkedHashMap<PAS, int[]> pasSet) {
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			var pair1 = SSSP.dijkstra(network, origin, costs);
			Network.Edge[] minTree = pair1.first();
			double[] minDistance = pair1.second();
			
			for (Network.Edge edge : network.getEdges()) {
				
				double reducedCost = minDistance[edge.startNode] + costs[edge.index] - minDistance[edge.endNode];
				if (reducedCost <= POSTPROCESS_COST_EPSILON) {
					
					Network.Edge maxEdge = mostFlowIncomingEdge(edge.endNode, bush);
					if (edge == maxEdge)
						continue;
					
					PAS pas = MFS(maxEdge, minTree, bush, edge);
					if (pas != null) {
						int[] origins = collectOrigins(pas);
						
						if (origins.length > 1)
							pasSet.put(pas, origins);
					}
				}
			}
		}
	}
	
	protected int[] collectOrigins(PAS pas) {
		int[] origins = new int[network.zones];
		int count = 0;
		
		for (int origin = 0; origin < network.zones; origin++) {
			pas.origin = origin;
			double minSFB = pas.minSegmentFlowBound(bushes);
			double maxSFB = pas.maxSegmentFlowBound(bushes);
			if (minSFB + maxSFB > 1e-8) {
				origins[count++] = origin;
			}
		}
		
		return Arrays.copyOf(origins, count);
	}
	
	//////////////////// Shifting flows ////////////////////
	
	protected void shiftFlowsProportionally(PAS pas, int[] origins) {
		
		var triplet = flowsAndProportion(pas, origins);
		double[] minSegmentFlows = triplet.first();
		double[] maxSegmentFlows = triplet.second();
		double proportion = triplet.third();
		
		for (int i = 0; i < origins.length; i++) {
			int origin = origins[i];
			Bush bush = bushes[origin];
			
			double flowAdjustment = proportion *
					(minSegmentFlows[i] + maxSegmentFlows[i]) - minSegmentFlows[i];
			
			for (int k : pas.minSegment())
				bush.addFlow(k, flowAdjustment);
			
			for (int k : pas.maxSegment())
				bush.addFlow(k, -flowAdjustment);
			
			updateNodeFlowsAndApproachProportions(pas, origin);
		}
	}
	
	protected Triplet<double[], double[], Double> flowsAndProportion(PAS pas, int[] origins) {
		int head = pas.head(network);
		
		double totalMinSegmentFlow = 0;
		double totalMaxSegmentFlow = 0;
		
		double[] minSegmentFlows = new double[origins.length];
		double[] maxSegmentFlows = new double[origins.length];
		
		for (int i = 0; i < origins.length; i++) {
			int origin = origins[i];
			
			double minSegmentFlow = nodeFlows[origin][head];
			for (int k : pas.minSegment())
				minSegmentFlow *= approachProportions[origin][k];
			
			double maxSegmentFlow = nodeFlows[origin][head];
			for (int k : pas.maxSegment())
				maxSegmentFlow *= approachProportions[origin][k];
			
			minSegmentFlows[i] = minSegmentFlow;
			maxSegmentFlows[i] = maxSegmentFlow;
			totalMinSegmentFlow += minSegmentFlow;
			totalMaxSegmentFlow += maxSegmentFlow;
		}
		
		double proportion = totalMinSegmentFlow / (totalMinSegmentFlow + totalMaxSegmentFlow);
		
		return new Triplet<>(minSegmentFlows, maxSegmentFlows, proportion);
	}
	
	protected void updateNodeFlowsAndApproachProportions(PAS pas, int origin) {
		Network.Edge[] edges = network.getEdges();
		Bush bush = bushes[origin];
		
		for (int i : pas.maxSegment()) {
			int node = edges[i].endNode;
			
			double newNodeFlow = 0;
			for (Network.Edge edge : network.backwardStar(node)) {
				newNodeFlow += bush.getEdgeFlow(edge.index);
			}
			nodeFlows[origin][node] = newNodeFlow;
			
			for (Network.Edge edge : network.backwardStar(node)) {
				approachProportions[origin][edge.index] = (newNodeFlow == 0)
						? 0
						: bush.getEdgeFlow(edge.index) / newNodeFlow;
			}
		}
		
		for (int i : pas.minSegment()) {
			int node = edges[i].endNode;
			
			double newNodeFlow = 0;
			for (Network.Edge edge : network.backwardStar(node)) {
				newNodeFlow += bush.getEdgeFlow(edge.index);
			}
			nodeFlows[origin][node] = newNodeFlow;
			
			for (Network.Edge edge : network.backwardStar(node)) {
				approachProportions[origin][edge.index] = (newNodeFlow == 0)
						? 0
						: bush.getEdgeFlow(edge.index) / newNodeFlow;
			}
		}
	}
	
	//////////////////// Convergence criterions ////////////////////
	
	protected double calculateEntropy(double[][] nodeFlows) {
		double entropy = 0;
		
		for (int origin = 0; origin < network.zones; origin++) {
			Bush bush = bushes[origin];
			
			for (Network.Edge edge : network.getEdges()) {
				double x = bush.getEdgeFlow(edge.index);
				double n = nodeFlows[origin][edge.endNode];
				entropy += (x == 0) // 0 * ln(0) = 0
						? 0
						: x * Math.log(x / n);
			}
		}
		
		return -entropy;
	}
	
	protected double calculateAverageProportionGap(LinkedHashMap<PAS, int[]> pasSet) {
		double averageProportionGap = 0;
		
		for (Map.Entry<PAS, int[]> entry : pasSet.entrySet()) {
			PAS pas = entry.getKey();
			int[] origins = entry.getValue();
			
			var triplet = flowsAndProportion(pas, origins);
			double[] minSegmentFlows = triplet.first();
			double[] maxSegmentFlows = triplet.second();
			double proportion = triplet.third();
			
			double maxGap = 0;
			for (int i = 0; i < origins.length; i++) {
				double originProportion = minSegmentFlows[i] / (maxSegmentFlows[i] + minSegmentFlows[i]);
				double gap = Math.abs(originProportion - proportion);
				maxGap = Math.max(maxGap, gap);
			}
			averageProportionGap += maxGap;
		}
		
		return averageProportionGap / pasSet.size();
	}
}
