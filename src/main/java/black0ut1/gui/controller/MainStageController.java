package black0ut1.gui.controller;

import black0ut1.gui.MainGUI;
import black0ut1.gui.view.MainStage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Toggle;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public class MainStageController {
	
	public final MainStage mainStage;
	private Timeline timeline;
	private boolean isPlaying = false;
	private int selectedLink = -1;
	
	public MainStageController(MainStage mainStage) {
		this.mainStage = mainStage;
	}
	
	public void onShapeClicked(boolean isNode, int index) {
		if (!isNode) {
			selectedLink = index;
			mainStage.networkPane.setSelectedShape(false, index);
			
			changeDetails();
		}
	}
	
	public void onSliderValueChanged(ObservableValue<? extends Number> observable,
	                                 Number oldVal, Number newVal) {
		int newValue = (int) Math.round(newVal.doubleValue());
		mainStage.timeSlider.setValue(newValue);
		
		mainStage.timeTA.setText(String.format("%2d", newValue));
		mainStage.networkPane.setTime(newValue);
		
		changeDetails();
	}
	
	public void onPlayButtonClicked(ActionEvent value) {
		if (isPlaying) {
			timeline.stop();
			mainStage.playBT.setText("▶");
			isPlaying = false;
		} else {
			timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
				double current = mainStage.timeSlider.getValue();
				double max = mainStage.timeSlider.getMax();
				if (current >= max) {
					timeline.stop();
					mainStage.playBT.setText("▶");
					isPlaying = false;
				} else
					mainStage.timeSlider.setValue(current + 1);
			}));
			timeline.setCycleCount(Timeline.INDEFINITE);
			timeline.play();
			mainStage.playBT.setText("⏸");
			isPlaying = true;
		}
	}
	
	public void onSliderInteracted(MouseEvent event) {
		if (isPlaying) {
			timeline.stop();
			mainStage.playBT.setText("▶");
			isPlaying = false;
		}
	}
	
	public void onVisualizationModeChanged(ObservableValue<? extends Toggle> observable,
	                                       Toggle oldVal, Toggle newVal) {
		VisualizationMode mode = (VisualizationMode) newVal.getUserData();
		mainStage.networkPane.setVisuzalizationMode(mode);
		mainStage.volumeDesc.setText(mode.description);
		
		changeDetails();
	}
	
	public void changeDetails() {
		if (mainStage.details1LB == null || mainStage.details2LB == null)
			return;
		
		if (selectedLink == -1) {
			mainStage.details1LB.setText("Select link to see details");
			mainStage.details2LB.setText("");
			return;
		}
		
		VisualizationMode mode = (VisualizationMode) mainStage.visualizationToggleGroup
				.getSelectedToggle().getUserData();
		int time = (int) mainStage.timeSlider.getValue();
		
		switch (mode) {
			case FLOW_ACTUAL:
				double actualInflow = MainGUI.actual[selectedLink].inflow()[time];
				double actualOutflow = MainGUI.actual[selectedLink].outflow()[time];
				mainStage.details1LB.setText("Actual inflow:        " + actualInflow);
				mainStage.details2LB.setText("Actual outflow:      " + actualOutflow);
				break;
			case FLOW_PREDICTED:
				double predictedInflow = MainGUI.predicted[selectedLink].inflow()[time];
				double predictedOutflow = MainGUI.predicted[selectedLink].outflow()[time];
				mainStage.details1LB.setText("Predicted inflow:   " + predictedInflow);
				mainStage.details2LB.setText("Predicted outflow: " + predictedOutflow);
				break;
			default:
				mainStage.details1LB.setText("Select link to see details");
				mainStage.details2LB.setText("");
		}
	}
}
