package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.MSA;
import black0ut1.dynamic.equilibrium.StaticAONRouteChoice;
import black0ut1.dynamic.equilibrium.StaticRouteChoice;
import black0ut1.dynamic.loading.dnl.BasicDNL;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.tdsp.DestinationShortestPaths;
import black0ut1.io.TNTP;
import black0ut1.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class SiouxFallsDTA {
	
	static Network network;
	static DoubleMatrix odm;
	static double stepSize = 2;
	static int odmSteps = 10;
	static int timeSteps = 150;
	
	@BeforeAll
	static void setUpBeforeAll() {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
		network = pair.first();
		odm = pair.second();
	}
	
	@Test
	void test() {
		TimeDependentODM tdodm = TimeDependentODM.fromStaticODM(odm, odmSteps);
		DynamicNetwork dynamicNetwork = DynamicNetwork.fromStaticNetwork(network, tdodm, stepSize, timeSteps);
		
		StaticRouteChoice routeChoice = new StaticAONRouteChoice(network, dynamicNetwork, odm, timeSteps);
		DynamicNetworkLoading dnl = new BasicDNL(dynamicNetwork, tdodm, stepSize, timeSteps);
		DestinationShortestPaths tdsp = new DestinationShortestPaths(dynamicNetwork, stepSize, timeSteps);
		
		MSA msa = new MSA(dynamicNetwork, tdodm, routeChoice, dnl, tdsp, 100, stepSize);
		msa.run();
		dnl.checkDestinationInflows(true);
	}
}
