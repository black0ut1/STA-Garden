package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.MOF_DUE;
import black0ut1.dynamic.equilibrium.MSA;
import black0ut1.dynamic.equilibrium.StaticAONRouteChoice;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.dynamic.tdsp.DOT;
import black0ut1.io.TNTP;
import black0ut1.util.Util;


public class Main {
	
	// step size, odm steps, time steps, msa steps
	public static final Object[] siouxFallsParams = {1.0, 30, 110, 100, 10.0};
	public static final Object[] chicagoSketchParams = {0.1, 30, 350, 5, 2.0};
	
	public static String network;
	
	public static void main(String[] args) {
		network = args[0];
		Object[] params = ("SiouxFalls".equals(network))
				? siouxFallsParams
				: chicagoSketchParams;
		
		String networkFile = "data/" + network + "/" + network + "_net.tntp";
		String odmFile = "data/" + network + "/" + network + "_trips.tntp";
		String nodeFile = "data/" + network + "/" + network + "_node.tntp";
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
		Network network = pair.first();
		DoubleMatrix odm = pair.second();
		
		double stepSize = (double) params[0];
		int odmSteps = (int) params[1];
		int timeSteps = (int) params[2];
		int msaSteps = (int) params[3];
		double odmScale = (double) params[4];
		
		TimeDependentODM tdodm = TimeDependentODM
				.fromStaticGaussian(odm, odmSteps)
				.scale(odmScale);
		DynamicNetwork dynamicNetwork = DynamicNetwork.fromStaticNetwork(pair.first(), tdodm, stepSize, timeSteps);
		
		StaticRouteChoice routeChoice = new StaticAONRouteChoice(network, dynamicNetwork, odm, timeSteps);
		DynamicNetworkLoading dnl = new ILTM_DNL(dynamicNetwork, tdodm, stepSize, timeSteps, 1e-8);
		DOT tdsp = new DOT(dynamicNetwork, stepSize, timeSteps, false);
		
		black0ut1.dynamic.Convergence convergence = new black0ut1.dynamic.Convergence(dynamicNetwork, tdodm, stepSize, null);
		
		MOF_DUE msa = new MSA(dynamicNetwork, tdodm, routeChoice, dnl, tdsp, msaSteps, stepSize, convergence);
		long tick = System.currentTimeMillis();
		msa.run();
		long tock = System.currentTimeMillis();
		System.out.println("DTA took: " + (tock - tick) + "ms");
	}
}