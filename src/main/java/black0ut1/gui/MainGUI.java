package black0ut1.gui;

import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.gui.view.MainStage;
import black0ut1.io.CSV;
import black0ut1.util.Util;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MainGUI extends Application {
	
	public static DynamicNetwork network;
	public static Network.Node[] nodes;
	public static CSV.LinkData[] predicted;
	public static CSV.LinkData[] actual;
	public static int totalTimeSteps;
	
	@Override
	public void start(Stage primaryStage) {
		MainStage mainStage = new MainStage();
		mainStage.setMaximized(true);
		mainStage.show();
	}
	
	public static void main(String[] args) {
		String map = "SiouxFalls";
		String networkFile = "data/" + map + "/link.csv";
		String odmFile = "data/" + map + "/demand.csv";
		String nodeFile = "data/" + map + "/node.csv";
		String predictedFile = "data/" + map + "/predicted.csv";
		String actualFile = "data/" + map + "/actual.csv";
		
		var pair = Util.loadData(new CSV(), networkFile, odmFile, nodeFile);
		
		double timeStep = 1;
		int odmSteps = 10;
		totalTimeSteps = getTotalTimeSteps(actualFile);
		
		TimeDependentODM odm = TimeDependentODM.fromStaticODM(pair.second(), odmSteps);
		DynamicNetwork network = DynamicNetwork.fromStaticNetwork(pair.first(), odm, timeStep, totalTimeSteps);
		
		predicted = new CSV().readLinkData(predictedFile, network, totalTimeSteps);
		actual = new CSV().readLinkData(actualFile, network, totalTimeSteps);
		
		MainGUI.network = network;
		MainGUI.nodes = pair.first().getNodes();
		launch(args);
	}
	
	public static int getTotalTimeSteps(String actualFile) {
		String lastLine = "";
		try (BufferedReader br = new BufferedReader(new FileReader(actualFile))) {
			
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				lastLine = sCurrentLine;
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return Integer.parseInt(lastLine.split(",")[2]) + 1;
	}
}
