package black0ut1.gui.controller;

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
	
	public MainStageController(MainStage mainStage) {
		this.mainStage = mainStage;
	}
	
	public void onShapeClicked(boolean isNode, int index) {
	
	}
	
	public void onSliderChanged(ObservableValue<? extends Number> observable,
								Number oldVal, Number newVal) {
		int newValue = (int) Math.round(newVal.doubleValue());
		mainStage.timeSlider.setValue(newValue);
		
		mainStage.timeTA.setText(String.format("%2d", newValue));
		mainStage.networkPane.setTime(newValue);
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
				} else {
					mainStage.timeSlider.setValue(current + 1);
				}
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
	}
}
