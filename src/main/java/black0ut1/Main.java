package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.Network;
import black0ut1.io.TNTP;
import black0ut1.sta.assignment.Algorithm;
import black0ut1.sta.assignment.link.*;
import black0ut1.sta.assignment.bush.*;
import black0ut1.sta.Convergence;
import black0ut1.sta.cost.*;

public class Main {
	
	public static void main(String[] argv) {
//		Args args = new Args();
//		var jcom = new JCommander(args, null, argv);
//		if (args.help) {
//			jcom.usage();
//			return;
//		}
		
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String flowsFile = "data/" + map + "/" + map + "_flow.tntp";
		
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
		
		Algorithm.Parameters parameters = new Algorithm.Parameters(
				network, odMatrix, new CanonicalBPR(), 30,
				new Convergence.Builder()
						.addCriterion(Convergence.Criterion.OBJECTIVE_FUNCTION)
						.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 0.01));
		
		Algorithm a = new FrankWolfe(parameters);
		
		startTime = System.currentTimeMillis();
		a.run();
		endTime = System.currentTimeMillis();
		System.out.println("Static traffic assigment computation time is " + (endTime - startTime) + " ms.");
		
		TNTP.writeFlows(flowsFile, network, a.getFlows(), a.getCosts());
	}
}