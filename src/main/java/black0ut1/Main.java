package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.io.TNTP;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.bush.iTAPAS;
import black0ut1.sta.Convergence;
import black0ut1.sta.cost.*;

public class Main {
	
	public static void main(String[] argv) {
		String map = "Sydney";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = loadData(networkFile, odmFile, nodeFile);
		Network network = pair.first();
		DoubleMatrix odMatrix = pair.second();
		
		Algorithm.Parameters parameters = new Algorithm.Parameters(
				network, odMatrix, new BPR(), 50, new Convergence.Builder()
				.addCriterion(Convergence.Criterion.BECKMANN_FUNCTION)
				.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 1e-10));
		
		
//		Iteration 8
//		Beckmann function: 9743292,109383011000000
//		Relative gap 1: 0,000000000005369
//		-----------------------------------
		Algorithm fw = new iTAPAS(parameters);
		long startTime = System.currentTimeMillis();
		fw.run();
		long endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
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