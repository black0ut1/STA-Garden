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
	
	public Button playBT;
	public Slider timeSlider;
	public TextField timeTA;
	
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
		
		playBT = new Button("▶");
		playBT.setPrefWidth(30);
		playBT.setOnAction(controller::onPlayButtonClicked);
		
		timeSlider = new Slider(0, MainGUI.totalTimeSteps - 1, 0);
		timeSlider.setShowTickMarks(true);
		timeSlider.setShowTickLabels(true);
		timeSlider.setSnapToTicks(true);
		timeSlider.setMinorTickCount(100);
		timeSlider.setMinWidth(400);
		timeSlider.valueProperty().addListener(controller::onSliderChanged);
		timeSlider.setOnMousePressed(controller::onSliderInteracted);
		timeSlider.setOnMouseDragged(controller::onSliderInteracted);
		
		timeTA = new TextField(" 0");
		timeTA.setEditable(false);
		timeTA.setPrefWidth(30);
		
		sliderPane.getChildren().addAll(playBT, timeSlider, timeTA);
		return sliderPane;
	}
	
	public Node getTogglePane() {
		VBox togglePane = new VBox(5);
		visualizationToggleGroup = new ToggleGroup();
		
		for (VisualizationMode mode : VisualizationMode.values()) {
			RadioButton button = new RadioButton(mode.name);
			button.setToggleGroup(visualizationToggleGroup);
			button.setUserData(mode);
			togglePane.getChildren().add(button);
		}
		
		Text volumeDesc = new Text("Shows the differences in amount of vehicles. Red means the predicted " +
				"amount is lower than the actual amount, blue means predicted amount is higher than the actual.");
		volumeDesc.setWrappingWidth(400);
		
		visualizationToggleGroup.selectedToggleProperty().addListener(controller::onVisualizationModeChanged);
		((RadioButton) togglePane.getChildren().getFirst()).setSelected(true);
		togglePane.getChildren().addAll(volumeDesc);
		return togglePane;
	}
}
