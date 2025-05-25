package black0ut1.gui;

import black0ut1.Main;
import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.gui.view.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {
	
	public static DynamicNetwork network;
	public static Network.Node[] nodes;
	
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
		
		var pair = Main.loadData(networkFile, odmFile, nodeFile);
		
		double timeStep = 0.4;
		int odmSteps = 10;
		int totalSteps = 2000;
		
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, totalSteps);
		
		MainGUI.network = network;
		MainGUI.nodes = pair.first().getNodes();
		launch(args);
	}
}
