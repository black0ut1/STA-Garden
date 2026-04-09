package black0ut1.gui.controller;

public enum VisualizationMode {
	FLOW_ACTUAL("Actual flow"),
	FLOW_PREDICTED("Predicted flow"),
	FLOW_DIFFERENCE("Flow difference"),
	CUMULATIVE_FLOW("Cumulative flow"),
	VOLUME_ACTUAL("Actual volume"),
	VOLUME_PREDICTED("Predicted volume"),
	VOLUME_DIFFERENCE("Volume difference");
	
	public final String name;
	
	VisualizationMode(String name) {
		this.name = name;
	}
}
