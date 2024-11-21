package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Network;
import black0ut1.data.Pair;
import black0ut1.gui.CriterionChartPanel;
import black0ut1.gui.GUI;
import black0ut1.io.TNTP;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.bush.iTAPAS;
import black0ut1.sta.Convergence;
import black0ut1.sta.cost.*;

public class Main {
	
	public static void main(String[] argv) {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		
		var pair = loadData(networkFile, odmFile);
		Network network = pair.first();
		DoubleMatrix odMatrix = pair.second();
		
//		var chart = new CriterionChartPanel("Relative gap");
//		new GUI(chart);
		
		
		Algorithm.Parameters parameters = new Algorithm.Parameters(
				network, odMatrix, new BPR(), 50, new Convergence.Builder()
				.addCriterion(Convergence.Criterion.BECKMANN_FUNCTION)
				.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 1e-15)
//				.setCallback(doubles -> {
//					int i = Convergence.Criterion.RELATIVE_GAP_1.ordinal();
//					chart.addValue(doubles[i], "iTAPAS");
//				})
		);
		
		// Sydney 1e-10: 8it 400s 35GB
		// ChicagoRegional 1e-10: 25it 441s  0,000000000089524
		Algorithm fw = new iTAPAS(parameters);
		long startTime = System.currentTimeMillis();
		fw.run();
		long endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
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