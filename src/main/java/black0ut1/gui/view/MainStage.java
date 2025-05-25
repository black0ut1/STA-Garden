package black0ut1.gui.view;

import black0ut1.gui.MainGUI;
import black0ut1.gui.controller.MainStageController;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import static black0ut1.gui.Constants.HORIZONTAL_DIVIDER_POSITION;
import static black0ut1.gui.Constants.VERTICAL_DIVIDER_POSITION;

public class MainStage extends Stage {
	
	public BorderPane rootBorderPane;
	public SplitPane verticalRootSplitPane, horizontalRootSplitPane;
	public DTANetworkPane networkPane;
	
	public final MainStageController controller;
	
	public MainStage() {
		super();
		this.controller = new MainStageController(this);
		
		setScene(new Scene(getRoot()));
		showingProperty().addListener((_, _, newValue) -> {
			if (newValue) {
				verticalRootSplitPane.setDividerPositions(VERTICAL_DIVIDER_POSITION);
				horizontalRootSplitPane.setDividerPositions(HORIZONTAL_DIVIDER_POSITION);
			}
		});
	}
	
	public Parent getRoot() {
		horizontalRootSplitPane = new SplitPane(getNetworkPane(), new Pane());
		
		verticalRootSplitPane = new SplitPane(horizontalRootSplitPane, getBottomPane());
		verticalRootSplitPane.setOrientation(Orientation.VERTICAL);
		
		rootBorderPane = new BorderPane();
		rootBorderPane.setCenter(verticalRootSplitPane);
		rootBorderPane.setTop(getTopPane());
		return rootBorderPane;
	}
	
	public Node getNetworkPane() {
		networkPane = new DTANetworkPane(MainGUI.network, MainGUI.nodes);
		networkPane.setOnShapeClicked(controller::onShapeClicked);
		
		return networkPane;
	}
	
	public Node getBottomPane() {
		HBox bottomPane = new HBox();
		
		bottomPane.getChildren().add(new Button("bbb"));
		
		return bottomPane;
	}
	
	public Node getTopPane() {
		HBox topPane = new HBox();
		
		topPane.getChildren().add(new Button("ccc"));
		
		return topPane;
	}
}
