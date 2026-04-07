package black0ut1.gui.view;

import black0ut1.gui.MainGUI;
import black0ut1.gui.controller.MainStageController;
import black0ut1.gui.controller.VisualizationMode;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MainStage extends Stage {
	
	public BorderPane rootBorderPane;
	public VBox controlPane;
	public DTANetworkPane networkPane;
	
	public Button playButton;
	public Slider timeSlider;
	public Label timeLabel;
	
	public ToggleGroup visualizationToggleGroup;
	
	public final MainStageController controller;
	
	public MainStage() {
		super();
		this.controller = new MainStageController(this);
		
		setScene(new Scene(getRoot()));
	}
	
	public Parent getRoot() {
		rootBorderPane = new BorderPane();
		rootBorderPane.setCenter(getNetworkPane());
		rootBorderPane.setRight(getControlPane());
		return rootBorderPane;
	}
	
	public Node getNetworkPane() {
		networkPane = new DTANetworkPane(MainGUI.network, MainGUI.nodes);
		networkPane.setOnShapeClicked(controller::onShapeClicked);
		
		return networkPane;
	}
	
	public Node getControlPane() {
		controlPane = new VBox(10);
		controlPane.setPadding(new Insets(10));
		
		Label sliderTitle = new Label("Time controls");
		sliderTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");
		
		Label toggleTitle = new Label("Toggle visualization");
		toggleTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");
		
		controlPane.getChildren().addAll(sliderTitle, getSliderPane(), toggleTitle,
				getTogglePane());
		return controlPane;
	}
	
	public Node getSliderPane() {
		HBox sliderPane = new HBox(10);
		
		playButton = new Button("▶");
		playButton.setPrefWidth(30);
		playButton.setOnAction(controller::onPlayButtonClicked);
		
		timeSlider = new Slider(0, MainGUI.totalTimeSteps - 1, 0);
		timeSlider.setShowTickMarks(true);
		timeSlider.setShowTickLabels(true);
		timeSlider.setSnapToTicks(true);
		timeSlider.setMinorTickCount(100);
		timeSlider.setMinWidth(400);
		timeSlider.valueProperty().addListener(controller::onSliderChanged);
		timeSlider.setOnMousePressed(controller::onSliderInteracted);
		timeSlider.setOnMouseDragged(controller::onSliderInteracted);
		
		timeLabel = new Label(" 0");
		timeLabel.setPrefWidth(25);
		
		sliderPane.getChildren().addAll(playButton, timeSlider, timeLabel);
		return sliderPane;
	}
	
	public Node getTogglePane() {
		VBox togglePane = new VBox(5);
		visualizationToggleGroup = new ToggleGroup();
		
		RadioButton flowButton = new RadioButton("Flow differences");
		flowButton.setToggleGroup(visualizationToggleGroup);
		flowButton.setUserData(VisualizationMode.FLOW);
		flowButton.setSelected(true);
		
		Text flowDesc = new Text("Shows the differences in amount of vehicles. Red means the predicted " +
				"amount is lower than the actual amount, blue means predicted amount is higher than the actual.");
		flowDesc.setWrappingWidth(400);
		
		RadioButton cumulativeFlowButton = new RadioButton("Cumulative flow");
		cumulativeFlowButton.setToggleGroup(visualizationToggleGroup);
		cumulativeFlowButton.setUserData(VisualizationMode.CUMULATIVE_FLOW);
		
		Text cflowDesc = new Text("Shows the differences in amount of vehicles. Red means the predicted " +
				"amount is lower than the actual amount, blue means predicted amount is higher than the actual.");
		cflowDesc.setWrappingWidth(400);
		
		RadioButton volumeButton = new RadioButton("Volume differences");
		volumeButton.setToggleGroup(visualizationToggleGroup);
		volumeButton.setUserData(VisualizationMode.VOLUME);
		
		Text volumeDesc = new Text("Shows the differences in amount of vehicles. Red means the predicted " +
				"amount is lower than the actual amount, blue means predicted amount is higher than the actual.");
		volumeDesc.setWrappingWidth(400);
		
		visualizationToggleGroup.selectedToggleProperty().addListener(controller::onVisualizationModeChanged);
		togglePane.getChildren().addAll(flowButton, flowDesc, cumulativeFlowButton, cflowDesc, volumeButton,
				volumeDesc);
		return togglePane;
	}
}
