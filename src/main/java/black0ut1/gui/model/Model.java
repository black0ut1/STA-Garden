package black0ut1.gui.model;

import black0ut1.gui.MainGUI;
import black0ut1.gui.view.DTANetworkPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.util.Duration;

public class Model {
	
	private static final Model instance = new Model();
	
	public final IntegerProperty currentTimeProperty = new SimpleIntegerProperty(0);
	public final BooleanProperty isPlayingProperty = new SimpleBooleanProperty(false);
	private final Timeline timeline = new Timeline();
	public final ObjectProperty<VisualizationMode> currentVisualizationModeProperty
			= new SimpleObjectProperty<>(VisualizationMode.DEFAULT);
	
	public final ObjectProperty<DTANetworkPane.Shape> selectedShapeProperty = new SimpleObjectProperty<>(null);
	public final ObjectProperty<DTANetworkPane.Shape> hoveredShapeProperty = new SimpleObjectProperty<>(null);
	
	public final DoubleProperty horizontalDividerProperty = new SimpleDoubleProperty(0.75);
	
	private Model() {
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(new KeyFrame(
				Duration.seconds(1), _ -> currentTimeProperty.set(currentTimeProperty.get() + 1)));
		isPlayingProperty.addListener((_, _, isNowPlaying) -> {
			if (isNowPlaying) {
				if (currentTimeProperty.get() == MainGUI.timeSteps - 1)
					currentTimeProperty.set(0);
				
				timeline.play();
			} else
				timeline.pause();
		});
		currentTimeProperty.addListener((_, _, newValue) -> {
			if (newValue.intValue() == MainGUI.timeSteps - 1)
				isPlayingProperty.set(false);
		});
	}
	
	public static Model getInstance() {
		return instance;
	}
}
