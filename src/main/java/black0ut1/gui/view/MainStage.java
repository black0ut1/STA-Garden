package black0ut1.gui.view;

import black0ut1.gui.MainGUI;
import black0ut1.gui.controller.MainStageController;
import black0ut1.gui.model.Model;
import black0ut1.gui.model.VisualizationMode;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

import static black0ut1.gui.Constants.HORIZONTAL_DIVIDER_POSITION;
import static black0ut1.gui.Constants.VERTICAL_DIVIDER_POSITION;

public class MainStage extends Stage {
	
	public BorderPane rootBorderPane;
	public SplitPane verticalRootSplitPane, horizontalRootSplitPane;
	public DTANetworkPane networkPane;
	
	public Button playBT;
	public Slider timeSlider;
	
	public ToggleGroup visualizationToggleGroup;
	
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
		
		Model.getInstance().selectedShapeProperty.addListener(controller::onSelectedShapeChanged);
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
		return networkPane;
	}
	
	public Node getBottomPane() {
		HBox bottomPane = new HBox(5);
		bottomPane.setPadding(new Insets(10));
		
		playBT = new Button();
		playBT.setPrefWidth(30);
		playBT.setOnAction(controller::onPlayButtonClicked);
		playBT.textProperty().bind(Model.getInstance().isPlayingProperty.map(bool -> bool ? "⏸" : "▶"));
		
		timeSlider = new Slider(0, MainGUI.timeSteps - 1, 0);
		timeSlider.setShowTickMarks(true);
		timeSlider.setShowTickLabels(true);
		timeSlider.setMinorTickCount(0);
		timeSlider.setMinWidth(400);
		timeSlider.valueProperty().addListener(controller::onSliderValueChanged);
		timeSlider.valueProperty().bindBidirectional(Model.getInstance().currentTimeProperty);
		timeSlider.setOnMousePressed(controller::onSliderInteracted);
		timeSlider.setOnMouseDragged(controller::onSliderInteracted);
		
		TextArea timeTA = new TextArea();
		timeTA.setPrefWidth(30);
		timeTA.textProperty().bindBidirectional(Model.getInstance().currentTimeProperty, new NumberStringConverter());
		timeTA.setMaxHeight(10);
		timeTA.setTextFormatter(controller.integerFilter);
		
		VBox togglePane = new VBox(5);
		visualizationToggleGroup = new ToggleGroup();
		for (VisualizationMode mode : VisualizationMode.values()) {
			RadioButton button = new RadioButton(mode.name);
			button.setToggleGroup(visualizationToggleGroup);
			button.setUserData(mode);
			togglePane.getChildren().add(button);
		}
		visualizationToggleGroup.selectedToggleProperty().addListener(controller::onVisualizationModeChanged);
		for (Toggle toggle : visualizationToggleGroup.getToggles())
			if (toggle.getUserData() == Model.getInstance().currentVisualizationModeProperty.get()) {
				toggle.setSelected(true);
				break;
			}
		
		bottomPane.getChildren().addAll(playBT, timeSlider, timeTA, togglePane);
		return bottomPane;
	}
	
	public Node getTopPane() {
		HBox topPane = new HBox();
		
		topPane.getChildren().add(new Button("ccc"));
		
		return topPane;
	}
}
