package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Network;
import black0ut1.data.Pair;
import black0ut1.gui.CriterionChartPanel;
import black0ut1.gui.GUI;
import black0ut1.io.TNTP;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.link.*;
import black0ut1.sta.Convergence;
import black0ut1.sta.cost.*;

public class Main {
	
	public static void main(String[] argv) {
		String map = "ChicagoRegional";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String flowsFile = "data/" + map + "/" + map + "_flow.tntp";
		
		// load network and OD matrix
		var pair = loadData(networkFile, odmFile);
		Network network = pair.first();
		DoubleMatrix odMatrix = pair.second();
		
		// prepare chart for plotting relative gap
		CriterionChartPanel relativeGapChart = new CriterionChartPanel("Relative gap");
		new GUI(relativeGapChart);
		
		// common convergence criteria for both algorithms
		Convergence.Builder builder = new Convergence.Builder()
				.addCriterion(Convergence.Criterion.BECKMANN_FUNCTION)
				.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 0.001);
		
		// parameters are same apart from updating the chart
		Algorithm.Parameters parametersFW = new Algorithm.Parameters(
				network, odMatrix, new BPR(), 50, builder
				.setCallback(iterationData -> {
					int i = Convergence.Criterion.RELATIVE_GAP_1.ordinal();
					relativeGapChart.addValue(iterationData[i], "Frank-Wolfe");
				}));
		
		Algorithm.Parameters parametersCFW = new Algorithm.Parameters(
				network, odMatrix, new BPR(), 50, builder
				.setCallback(iterationData -> {
					int i = Convergence.Criterion.RELATIVE_GAP_1.ordinal();
					relativeGapChart.addValue(iterationData[i], "Conjugate Frank-Wolfe");
				}));
		
		// execute both algorithms
		Algorithm fw = new FrankWolfe(parametersFW);
		long startTime = System.currentTimeMillis();
		fw.run();
		long endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
		
		Algorithm cfw = new ConjugateFrankWolfe(parametersCFW);
		startTime = System.currentTimeMillis();
		cfw.run();
		endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
		
		// write assigned flows to file
		TNTP.writeFlows(flowsFile, network, fw.getFlows(), fw.getCosts());
	}
	
	private static Pair<Network, DoubleMatrix> loadData(String networkFile, String odmFile) {
		System.out.print("Loading network... ");
		long startTime = System.currentTimeMillis();
		Network network = TNTP.parseNetwork(networkFile);
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