package black0ut1.gui.static_;

import black0ut1.data.ColorSpectrum;
import black0ut1.data.network.Network;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

public class AssignmentPanel extends JPanel {
	
	private static final int NODE_RADIUS = 7;
	private static final int WHEEL_ROTATION_SCALE_FACTOR = 50;
	private static final double EDGE_OFFSET = 3;
	private static final float EDGE_WIDTH = 4;
	
	// if MAX_COST_FACTOR is 4, then the cost freeFlow * 4 and
	// above is colored using the max color in costSpectrum
	private static final double MAX_COST_FACTOR = 4;
	
	private final Network network;
	private final double[] normalizedNodesX, normalizedNodesY;
	private final double[] scaledNodesX, scaledNodesY;
	private double[] costs = null;
	private final ColorSpectrum costSpectrum
			= new ColorSpectrum(Color.GREEN, Color.YELLOW, Color.RED);
	
	private double scale = 500;
	private final Point2D.Double offset = new Point2D.Double(0, 0);
	private Point2D.Double tmpOffset = null;
	private Point dragStartPos = null;
	
	public AssignmentPanel(Network network) {
		this.normalizedNodesX = new double[network.nodes];
		this.normalizedNodesY = new double[network.nodes];
		this.scaledNodesX = new double[network.nodes];
		this.scaledNodesY = new double[network.nodes];
		
		this.network = network;
		if (network.getNodes() == null)
			throw new RuntimeException("Network doesn't contain node information");
		
		setPreferredSize(new Dimension(600, 600));
		setSize(600, 600);
		
		computeNormalizedNodes();
		recomputeNodes();
		
		addMouseWheelListener(event -> {
			int rot = event.getWheelRotation();
			scale -= WHEEL_ROTATION_SCALE_FACTOR * rot * Math.abs(scale / 100);
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
	
	/* Normalized coordinates of nodes of input network */
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
			normalizedNodesX[i] = (node.x() - midX) / lenX;
			normalizedNodesY[i] = (node.y() - midY) / lenY;
		}
	}
	
	/* Scales and shifts normalized nodes according to current scale and offset */
	private void recomputeNodes() {
		for (int i = 0; i < network.nodes; i++) {
			scaledNodesX[i] = (normalizedNodesX[i] + offset.x) * scale + getWidth() / 2.0;
			scaledNodesY[i] = (normalizedNodesY[i] + offset.y) * scale + getHeight() / 2.0;
		}
	}
	
	public void setCosts(double[] costs) {
		this.costs = costs;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		drawEdges(g2);
		drawNodes(g2);
		drawLegend(g2);
	}
	
	private void drawEdges(Graphics2D g) {
		g.setColor(Color.LIGHT_GRAY);
		g.setStroke(new BasicStroke(EDGE_WIDTH));
		
		for (Network.Edge edge : network.getEdges()) {
			double x1 = scaledNodesX[edge.startNode];
			double y1 = scaledNodesY[edge.startNode];
			double x2 = scaledNodesX[edge.endNode];
			double y2 = scaledNodesY[edge.endNode];
			
			double len = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
			double xOffset = EDGE_OFFSET * (-y2 + y1) / len;
			double yOffset = EDGE_OFFSET * (x2 - x1) / len;
			
			if (costs != null) {
				double costRatio = costs[edge.index] / edge.freeFlow;
				double colorValue = Math.min(costRatio - 1, MAX_COST_FACTOR - 1) / (MAX_COST_FACTOR - 1);
				g.setColor(costSpectrum.getColor(colorValue));
			}
			
			g.drawLine(
					(int) (x1 + xOffset), (int) (y1 + yOffset),
					(int) (x2 + xOffset), (int) (y2 + yOffset));
		}
	}
	
	private void drawNodes(Graphics2D g) {
		for (int i = 0; i < network.nodes; i++) {
			
			g.setColor((i < network.zones) ? Color.BLACK : Color.GRAY);
			g.fillOval((int) scaledNodesX[i] - NODE_RADIUS, (int) scaledNodesY[i] - NODE_RADIUS,
					2 * NODE_RADIUS, 2 * NODE_RADIUS);
		}
	}
	
	private void drawLegend(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1));
		
		int width = 50;
		int height = 300;
		int offset = 20;
		
		int y = offset + height;
		int x1 = getWidth() - width - offset;
		int x2 = getWidth() - offset;
		
		for (int i = 0; i <= y - offset; i++) {
			double colorVal = i / (double) (y - offset);
			g.setColor(costSpectrum.getColor(colorVal));
			
			g.drawLine(x1, y - i, x2, y - i);
		}
		
		int textShiftLeft = 27, textShiftDown = 5;
		g.setColor(Color.BLACK);
		g.drawString("1.0x", x1 - textShiftLeft, y + textShiftDown);
		g.drawString(MAX_COST_FACTOR + "x", x1 - textShiftLeft, offset + textShiftDown);
	}
}
