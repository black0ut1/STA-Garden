package black0ut1.gui.controller;

public enum VisualizationMode {
	FLOW_ACTUAL("Actual flow", "a"),
	FLOW_PREDICTED("Predicted flow", "b"),
	FLOW_DIFFERENCE("Flow difference", "c"),
	CUMULATIVE_FLOW("Cumulative flow", "d"),
	VOLUME_ACTUAL("Actual volume", "e"),
	VOLUME_PREDICTED("Predicted volume", "f"),
	VOLUME_DIFFERENCE("Volume difference", "g");
	
	public final String name;
	public final String description;
	
	VisualizationMode(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
