package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.gui.CriterionChartPanel;
import black0ut1.gui.GUI;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.STAAlgorithm;
import black0ut1.static_.assignment.STAConvergence;
import black0ut1.static_.assignment.link.*;
import black0ut1.static_.cost.*;

public class Main {
	
	public static void main(String[] argv) {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		String flowsFile = "data/" + map + "/" + map + "_flow.tntp";
		
		// load network and OD matrix
		var pair = loadData(networkFile, odmFile, nodeFile);
		Network network = pair.first();
		DoubleMatrix odMatrix = pair.second();
		
		// prepare chart for plotting relative gap
		CriterionChartPanel relativeGapChart = new CriterionChartPanel("Relative gap");
		new GUI(relativeGapChart);
		
		// common convergence criteria for both algorithms
		STAConvergence.Builder builder = new STAConvergence.Builder()
				.addCriterion(STAConvergence.Criterion.BECKMANN_FUNCTION)
				.addCriterion(STAConvergence.Criterion.RELATIVE_GAP_1, 0.001);
		
		// parameters are same apart from updating the chart
		STAAlgorithm.Parameters parametersFW = new STAAlgorithm.Parameters(
				network, odMatrix, new BPR(), 50, builder
				.setCallback(iterationData -> {
					int i = STAConvergence.Criterion.RELATIVE_GAP_1.ordinal();
					relativeGapChart.addValue(iterationData[i], "Frank-Wolfe");
				}));
		
		STAAlgorithm.Parameters parametersCFW = new STAAlgorithm.Parameters(
				network, odMatrix, new BPR(), 50, builder
				.setCallback(iterationData -> {
					int i = STAConvergence.Criterion.RELATIVE_GAP_1.ordinal();
					relativeGapChart.addValue(iterationData[i], "Conjugate Frank-Wolfe");
				}));
		
		// execute both algorithms
		STAAlgorithm fw = new FrankWolfe(parametersFW);
		long startTime = System.currentTimeMillis();
		fw.assignFlows();
		long endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
		
		STAAlgorithm cfw = new ConjugateFrankWolfe(parametersCFW);
		startTime = System.currentTimeMillis();
		cfw.assignFlows();
		endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
		
		// write assigned flows to file
		TNTP.writeFlows(flowsFile, network, fw.getFlows(), fw.getCosts());
	}
	
	private static Pair<Network, DoubleMatrix> loadData(String networkFile, String odmFile, String nodeFile) {
		System.out.print("Loading network... ");
		long startTime = System.currentTimeMillis();
		Network network = TNTP.parseNetwork(networkFile, nodeFile);
		long endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		System.out.print("Loading OD matrix... ");
		startTime = System.currentTimeMillis();
		DoubleMatrix odMatrix = TNTP.parseODMatrix(odmFile);
		endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		return new Pair<>(network, odMatrix);
	}
}