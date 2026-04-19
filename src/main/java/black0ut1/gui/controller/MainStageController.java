package black0ut1.gui.controller;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Intersection;
import black0ut1.gui.MainGUI;
import black0ut1.gui.model.Model;
import black0ut1.gui.model.VisualizationMode;
import black0ut1.gui.view.DTANetworkPane;
import black0ut1.gui.view.LinkPane;
import black0ut1.gui.view.MainStage;
import black0ut1.gui.view.NodePane;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.input.MouseEvent;

public class MainStageController {
	
	public final MainStage mainStage;
	
	private final Model model = Model.getInstance();
	
	public final TextFormatter<String> integerFilter = new TextFormatter<>(change -> {
		String text = change.getControlNewText();
		
		if (text.isEmpty() || text.matches("\\d+"))
			return change;
			
		return null;
	});
	
	public MainStageController(MainStage mainStage) {
		this.mainStage = mainStage;
		
		model.currentVisualizationModeProperty.addListener((_, _, newState) -> {
			if (newState == null) {
				mainStage.visualizationToggleGroup.selectToggle(null);
				return;
			}
			
			for (Toggle toggle : mainStage.visualizationToggleGroup.getToggles())
				if (newState.equals(toggle.getUserData())) {
					mainStage.visualizationToggleGroup.selectToggle(toggle);
					break;
				}
		});
	}
	
	public void onSelectedShapeChanged(ObservableValue<? extends DTANetworkPane.Shape> observable,
	                                   DTANetworkPane.Shape oldValue, DTANetworkPane.Shape newValue) {
		if (newValue == null)
			return;
		
		double currDividerPosition = mainStage.horizontalRootSplitPane.getDividerPositions()[0];
		mainStage.horizontalRootSplitPane.getItems().removeLast();
		
		if (newValue instanceof DTANetworkPane.NodeShape) {
			Intersection node = MainGUI.network.intersections[newValue.index];
			mainStage.horizontalRootSplitPane.getItems().add(new NodePane(node));
		} else if (newValue instanceof DTANetworkPane.LinkShape) {
			Link link = MainGUI.network.links[newValue.index];
			mainStage.horizontalRootSplitPane.getItems().add(new LinkPane(link));
		}
		
		mainStage.horizontalRootSplitPane.setDividerPositions(currDividerPosition);
	}
	
	public void onSliderValueChanged(ObservableValue<? extends Number> observable,
	                                 Number oldVal, Number newVal) {
		int newValue = (int) Math.round(newVal.doubleValue());
		mainStage.timeSlider.setValue(newValue);
	}
	
	public void onPlayButtonClicked(ActionEvent value) {
		model.isPlayingProperty.setValue(!model.isPlayingProperty.get());
	}
	
	public void onSliderInteracted(MouseEvent event) {
		model.isPlayingProperty.setValue(false);
	}
	
	public void onVisualizationModeChanged(Observable observable, Toggle oldVal, Toggle newVal) {
		VisualizationMode mode = (VisualizationMode) newVal.getUserData();
		model.currentVisualizationModeProperty.setValue(mode);
	}
}
