package black0ut1.gui;

import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.equilibrium.StaticAONRouteChoice;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.dnl.ILTM_DNL;
import black0ut1.gui.view.MainStage;
import black0ut1.io.TNTP;
import black0ut1.util.DynamicUtils;
import black0ut1.util.Util;
import javafx.application.Application;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;

import java.awt.*;

public class MainGUI extends Application {
	
	public static DynamicNetwork network;
	public static Network.Node[] nodes;
	public static int timeSteps = 200;
	public static double[][] travelTimes;
	
	@Override
	public void start(Stage primaryStage) {
		MainStage mainStage = new MainStage();
		mainStage.setMaximized(true);
		mainStage.show();
	}
	
	public static void main(String[] args) {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		String nodeFile = "data/" + map + "/" + map + "_node.tntp";
		
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, nodeFile);
		
		double timeStep = 1;
		int odmSteps = 10;
		
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, timeSteps);
		
		StaticAONRouteChoice rc = new StaticAONRouteChoice(pair.first(), network, pair.second(), timeSteps);
		var mfs = rc.computeInitialMixtureFractions();
		
		DynamicNetworkLoading dnl = new ILTM_DNL(network, odm, timeStep, timeSteps, 1e-8);
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
		
		travelTimes = new double[network.links.length][];
		for (int i = 0; i < network.links.length; i++) {
			travelTimes[i] = DynamicUtils.computeTravelTime(network.links[i].cumulativeInflow, network.links[i].cumulativeOutflow,
					timeStep, network.links[i].length / network.links[i].freeFlowSpeed);
		}
		
		StandardChartTheme theme = new StandardChartTheme("Default");
		theme.setPlotBackgroundPaint(Color.WHITE);
		theme.setDomainGridlinePaint(Color.LIGHT_GRAY);
		theme.setRangeGridlinePaint(Color.LIGHT_GRAY);
		ChartFactory.setChartTheme(theme);
		
		MainGUI.network = network;
		MainGUI.nodes = pair.first().getNodes();
		launch(args);
	}
}
