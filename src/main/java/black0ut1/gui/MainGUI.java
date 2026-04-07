package black0ut1.gui;

import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.gui.view.MainStage;
import black0ut1.io.CSV;
import black0ut1.util.Util;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {
	
	public static DynamicNetwork network;
	public static Network.Node[] nodes;
	public static CSV.LinkData[] predicted;
	public static CSV.LinkData[] actual;
	public static int totalTimeSteps = 49;
	
	@Override
	public void start(Stage primaryStage) {
		MainStage mainStage = new MainStage();
		mainStage.setMaximized(true);
		mainStage.show();
	}
	
	public static void main(String[] args) {
		String map = "17_Sioux_Falls";
		String networkFile = "data/" + map + "/link.csv";
		String odmFile = "data/" + map + "/demand.csv";
		String nodeFile = "data/" + map + "/node.csv";
		
		var pair = Util.loadData(new CSV(), networkFile, odmFile, nodeFile);
		
		double timeStep = 1;
		int odmSteps = 10;
		
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, totalTimeSteps);
		
		predicted = new CSV().readLinkData("data/predicted.csv", network, totalTimeSteps);
		actual = new CSV().readLinkData("data/actual.csv", network, totalTimeSteps);
		
		MainGUI.network = network;
		MainGUI.nodes = pair.first().getNodes();
		launch(args);
	}
}
