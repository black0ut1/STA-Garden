package black0ut1.gui.model;

public enum VisualizationMode {
	DEFAULT("Default"),
	VOLUME("Volume"),
	FLOW("Flow"),
	TRAVEL_TIME("Travel Time");
	
	public final String name;
	
	VisualizationMode(String name) {
		this.name = name;
	}
}
