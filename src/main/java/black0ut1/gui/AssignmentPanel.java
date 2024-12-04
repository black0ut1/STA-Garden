package black0ut1.gui;

import black0ut1.data.Network;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

public class AssignmentPanel extends JPanel {
	
	private static final int NODE_RADIUS = 7;
	private static final int WHEEL_ROTATION_SCALE_FACTOR = 50;
	
	private final Network network;
	private final Network.Node[] normalizedNodes;
	private final Network.Node[] scaledNodes;
	
	private double scale = 500;
	private final Point2D.Double offset = new Point2D.Double(0, 0);
	
	private Point2D.Double tmpOffset = null;
	private Point dragStartPos = null;
	
	public AssignmentPanel(Network network) {
		this.normalizedNodes = new Network.Node[network.nodes];
		this.scaledNodes = new Network.Node[network.nodes];
		this.network = network;
		assert network.getNodes() != null;
		
		setPreferredSize(new Dimension(600, 600));
		setSize(600, 600);
		
		computeNormalizedNodes();
		recomputeNodes();
		
		addMouseWheelListener(event -> {
			int rot = event.getWheelRotation();
			scale -= WHEEL_ROTATION_SCALE_FACTOR * rot;
			if (scale < 0)
				scale = 0;
			
			recomputeNodes();
			repaint();
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				// offset must be scaled down and rescaled in recomputeNodes
				// because only then the zoom will work with respect to middle
				// of the window (and not w.r.t. the coordinates origin)
				double dX = (e.getX() - dragStartPos.x) / scale;
				double dY = (e.getY() - dragStartPos.y) / scale;
				
				offset.setLocation(tmpOffset.x + dX, tmpOffset.y + dY);
				
				recomputeNodes();
				repaint();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tmpOffset = (Point2D.Double) offset.clone();
				dragStartPos = e.getPoint();
			}
		});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				recomputeNodes();
				repaint();
			}
		});
	}
	
	private void computeNormalizedNodes() {
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
		double lenX = maxX - minX;
		double lenY = maxY - minY;
		double midX = (maxX + minX) / 2;
		double midY = (maxY + minY) / 2;
		
		for (int i = 0; i < network.nodes; i++) {
			Network.Node node = network.getNodes()[i];
			double normX = (node.x() - midX) / lenX;
			double normY = (node.y() - midY) / lenY;
			normalizedNodes[i] = new Network.Node(i, normX, normY);
		}
	}
	
	private void recomputeNodes() {
		for (int i = 0; i < network.nodes; i++) {
			Network.Node normalizedNode = normalizedNodes[i];
			double scaledX = (normalizedNode.x() + offset.x) * scale + getWidth() / 2.0;
			double scaledY = (normalizedNode.y() + offset.y) * scale + getHeight() / 2.0;
			scaledNodes[i] = new Network.Node(i, scaledX, scaledY);
		}
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
			Network.Node startNode = scaledNodes[edge.startNode];
			Network.Node endNode = scaledNodes[edge.endNode];
			
			g.drawLine((int) startNode.x(), (int) startNode.y(),
					(int) endNode.x(), (int) endNode.y());
		}
	}
	
	private void drawNodes(Graphics2D g) {
		g.setColor(Color.BLACK);
		
		for (Network.Node node : scaledNodes) {
			g.fillOval((int) node.x() - NODE_RADIUS, (int) node.y() - NODE_RADIUS,
					2 * NODE_RADIUS, 2 * NODE_RADIUS);
		}
	}
}
