package black0ut1.gui.controller;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.node.Node;
import black0ut1.gui.MainGUI;
import black0ut1.gui.view.LinkPane;
import black0ut1.gui.view.MainStage;
import black0ut1.gui.view.NodePane;

public class MainStageController {
	
	public final MainStage mainStage;
	
	public MainStageController(MainStage mainStage) {
		this.mainStage = mainStage;
	}
	
	public void onShapeClicked(boolean isNode, int index) {
		double currDividerPosition = mainStage.horizontalRootSplitPane.getDividerPositions()[0];
		mainStage.horizontalRootSplitPane.getItems().removeLast();
		
		if (isNode) {
			Node node = MainGUI.network.intersections[index];
			mainStage.horizontalRootSplitPane.getItems().add(new NodePane(node));
		} else {
			Link link = MainGUI.network.links[index];
			mainStage.horizontalRootSplitPane.getItems().add(new LinkPane(link));
		}
		
		mainStage.horizontalRootSplitPane.setDividerPositions(currDividerPosition);
	}
}
