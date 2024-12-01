package black0ut1.gui;

import black0ut1.data.Network;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class AssignmentPanel extends JPanel {
	
	private static final int NODE_RADIUS = 7;
	
	/** Number of pixels from each side */
	private static final int PADDING = 20;
	
	private final Network network;
	private final Network.Node[] normalizedNodes;
	
	public AssignmentPanel(Network network) {
		this.normalizedNodes = new Network.Node[network.nodes];
		this.network = network;
		assert network.getNodes() != null;
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Network.Node node : network.getNodes()) {
			minX = Math.min(minX, node.x());
			minY = Math.min(minY, node.y());
			maxX = Math.max(maxX, node.x());
			maxY = Math.max(maxY, node.y());
		}
		double finalMinX = minX;
		double finalMinY = minY;
		double lenX = maxX - minX;
		double lenY = maxY - minY;
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				for (int i = 0; i < network.nodes; i++) {
					Network.Node node = network.getNodes()[i];
					
					double normX = (node.x() - finalMinX) / lenX;
					double normY = (node.y() - finalMinY) / lenY;
					
					double width = getWidth() - 2 * PADDING;
					double height = getHeight() - 2 * PADDING;
					double scaledX = PADDING + Math.round(normX * width);
					double scaledY = PADDING + Math.round(normY * height);
					
					normalizedNodes[i] = new Network.Node(i, scaledX, scaledY);
				}
			}
		});
		setPreferredSize(new Dimension(600, 600));
		setSize(600, 600);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		drawEdges(g2);
		drawNodes(g2);
	}
	
	private void drawEdges(Graphics2D g) {
		g.setColor(Color.BLACK);
		
		for (Network.Edge edge : network.getEdges()) {
			Network.Node startNode = normalizedNodes[edge.startNode];
			Network.Node endNode = normalizedNodes[edge.endNode];
			
			g.drawLine((int) startNode.x(), (int) startNode.y(),
					(int) endNode.x(), (int) endNode.y());
		}
	}
	
	private void drawNodes(Graphics2D g) {
		g.setColor(Color.BLACK);
		
		for (Network.Node node : normalizedNodes) {
			g.fillOval((int) node.x() - NODE_RADIUS, (int) node.y() - NODE_RADIUS,
					2 * NODE_RADIUS, 2 * NODE_RADIUS);
		}
	}
}
