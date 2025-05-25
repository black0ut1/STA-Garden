package black0ut1.gui.view;

import black0ut1.dynamic.loading.node.Node;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Arrays;

public class NodePane extends VBox {
	
	public NodePane(Node node) {
		super();
		setPadding(new Insets(10));
		setMinWidth(0);
		
		Text title = new Text("Node " + node.index);
		title.setFont(Font.font(null, FontWeight.BOLD, 30));
		
		Text modelLabel = new Text("Model: " + node.getClass().getSimpleName());
		
		Text incomingLinks = new Text("Incoming links: " + String.join(", ",
				Arrays.stream(node.incomingLinks)
						.map(a -> String.valueOf(a.index))
						.toList()
		));
		
		Text outgoingLinks = new Text("Outgoing links: " + String.join(", ",
				Arrays.stream(node.outgoingLinks)
						.map(a -> String.valueOf(a.index))
						.toList()
		));
		
		getChildren().addAll(title, modelLabel, incomingLinks, outgoingLinks);
	}
}
