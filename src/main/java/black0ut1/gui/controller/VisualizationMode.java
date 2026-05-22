package black0ut1.gui.controller;

import black0ut1.gui.Constants;

public enum VisualizationMode {
	FLOW_ACTUAL("Actual flow", "Shows the actual inflows/outflows simulated by the DNL." +
			" Each link is divided into two halves, first half visualize inflow and second" +
			" half outflow (assuming cars drive on the right side). Pure grey means zero" +
			" flow while pure blue means " + Constants.FLOW_MAX + " or more flow."),
	FLOW_PREDICTED("Predicted flow", "Shows the predicted inflows/outflows computed by the" +
			" metamodel. Each link is divided into two halves, first half visualize inflow" +
			" and second half outflow (assuming cars drive on the right side). Pure grey means" +
			" zero flow while pure red means " + Constants.FLOW_MAX + " or more flow."),
	FLOW_DIFFERENCE("Flow difference", "Shows the differences between actual and predicted" +
			" flows, specifically (actual - predicted). Blue color means that predicted flow is" +
			" smaller than actual, red means predicted flow is larger than actual."),
	VOLUME_ACTUAL("Actual volume", "Shows the amount of vehicles present on each link computed" +
			" from actual flows. Pure grey means zero vehicles on link, while pure blue means" +
			" " + Constants.VOLUME_MAX + " or more vehicles."),
	VOLUME_PREDICTED("Predicted volume", "Shows the amount of vehicles present on each link" +
			" computed from predicted flows. Pure grey means zero vehicles on link, pure red" +
			" means " + Constants.VOLUME_MAX + " or more vehicles, and blue color means negative" +
			" amount of vehicles on link (since the metamodel does not conserve vehicles)."),
	VOLUME_DIFFERENCE("Volume difference", "Shows the differences between actual and predicted" +
			" amounts of vehicles, specifically (actual - predicted). Blue color means predicted" +
			" volume is smaller than actual, red means predicted volume is larger than actual.");
	
	public final String name;
	public final String description;
	
	VisualizationMode(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
