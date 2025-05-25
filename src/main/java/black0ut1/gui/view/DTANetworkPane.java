package black0ut1.gui.view;

import black0ut1.data.network.Network;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.gui.Constants;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.function.BiConsumer;

import static black0ut1.gui.Constants.*;

public class DTANetworkPane extends Pane {
	
	private final Canvas canvas;
	private final GraphicsContext gc;
	
	private final LinkShape[] linkShapes;
	private final NodeShape[] nodeShapes;
	
	/** Coordinates of nodes normalized to [0, NORMALIZED_SCALE]. */
	private final double[] normalizedNodesX, normalizedNodesY;
	
	private double scale = 1;
	private Point2D offset = new Point2D(0, 0);
	private Point2D tmpOffset = null;
	private Point2D dragStart = null;
	
	private Shape hoverShape = null;
	
	public DTANetworkPane(DynamicNetwork network, Network.Node[] nodes) {
		super();
		
		this.canvas = new Canvas();
		this.getChildren().add(this.canvas);
		this.gc = canvas.getGraphicsContext2D();
		
		this.linkShapes = new LinkShape[network.links.length];
		for (int i = 0; i < network.links.length; i++)
			linkShapes[i] = new LinkShape(i, network.links[i].tail.index, network.links[i].head.index);
		
		this.nodeShapes = new NodeShape[nodes.length];
		for (int i = 0; i < nodes.length; i++)
			nodeShapes[i] = new NodeShape(i);
		
		this.normalizedNodesX = new double[network.intersections.length];
		this.normalizedNodesY = new double[network.intersections.length];
		
		setOnScroll(e -> {
			double rot = e.getDeltaY();
			scale += Constants.WHEEL_ROTATION_SCALE_FACTOR * rot * Math.abs(scale / 200);
			if (scale < 0)
				scale = 0;
			
			paint();
		});
		setOnMousePressed(e -> {
			tmpOffset = offset;
			this.dragStart = new Point2D(e.getX(), e.getY());
		});
		setOnMouseDragged(e -> {
			double dX = (e.getX() - dragStart.getX()) / scale;
			double dY = (e.getY() - dragStart.getY()) / scale;
			offset = tmpOffset.add(dX, dY);
			
			paint();
		});
		setOnMouseMoved(e -> {
			// transformation of canvas coordinates into node coordinates
			// (accounting for affine transforms)
			double x = (e.getX() - getWidth() / 2) / scale - offset.getX();
			double y = (e.getY() - getHeight() / 2) / scale - offset.getY();
			
			hoverShape = null;
			for (LinkShape linkShape : linkShapes) {
				if (linkShape.containsPoint(x, y))
					hoverShape = linkShape;
			}
			for (NodeShape nodeShape : nodeShapes) {
				if (nodeShape.containsPoint(x, y))
					hoverShape = nodeShape;
			}
			
			paint();
		});
		
		computeNormalizedNodes(nodes);
		paint();
	}
	
	public void setOnShapeClicked(BiConsumer<Boolean, Integer> event) {
		setOnMouseClicked(e -> {
			double x = (e.getX() - getWidth() / 2) / scale - offset.getX();
			double y = (e.getY() - getHeight() / 2) / scale - offset.getY();
			
			Shape clickedShape = null;
			for (LinkShape linkShape : linkShapes) {
				if (linkShape.containsPoint(x, y))
					clickedShape = linkShape;
			}
			for (NodeShape nodeShape : nodeShapes) {
				if (nodeShape.containsPoint(x, y))
					clickedShape = nodeShape;
			}
			
			if (clickedShape == null)
				return;
			
			event.accept(clickedShape instanceof NodeShape, clickedShape.index);
		});
	}
	
	private void paint() {
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, getWidth(), getHeight());
		
		gc.save();
		gc.translate(getWidth() / 2, getHeight() / 2);
		gc.scale(scale, scale);
		gc.translate(offset.getX(), offset.getY());
		
		gc.setLineWidth(LINK_WIDTH);
		for (LinkShape linkShape : linkShapes) {
			Paint color = linkShape == hoverShape
					? HOVER_COLOR
					: LINK_COLOR;
			gc.setStroke(color);
			
			linkShape.draw();
		}
		
		for (NodeShape nodeShape : nodeShapes) {
			Paint color = nodeShape == hoverShape
					? HOVER_COLOR
					: NODE_COLOR;
			gc.setFill(color);
			
			nodeShape.draw();
		}
		
		gc.restore();
	}
	
	private void computeNormalizedNodes(Network.Node[] nodes) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Network.Node node : nodes) {
			minX = Math.min(minX, node.x());
			minY = Math.min(minY, node.y());
			maxX = Math.max(maxX, node.x());
			maxY = Math.max(maxY, node.y());
		}
		double lenX = maxX - minX;
		double lenY = maxY - minY;
		double midX = (maxX + minX) / 2;
		double midY = (maxY + minY) / 2;
		
		for (int i = 0; i < nodes.length; i++) {
			Network.Node node = nodes[i];
			normalizedNodesX[i] = (node.x() - midX) / lenX * NORMALIZED_SCALE;
			normalizedNodesY[i] = (node.y() - midY) / lenY * NORMALIZED_SCALE;
		}
	}
	
	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		
		double w = getWidth();
		double h = getHeight();
		canvas.setWidth(w);
		canvas.setHeight(h);
		
		paint();
	}
	
	private abstract static class Shape {
		public final int index;
		
		private Shape(int index) {
			this.index = index;
		}
		
		public abstract void draw();
		public abstract boolean containsPoint(double x, double y);
	}
	
	private class LinkShape extends Shape {
		
		private final int tailIndex, headIndex;
		
		public LinkShape(int index, int tailIndex, int headIndex) {
			super(index);
			this.tailIndex = tailIndex;
			this.headIndex = headIndex;
		}
		
		public void draw() {
			double x1 = normalizedNodesX[tailIndex];
			double y1 = normalizedNodesY[tailIndex];
			double x2 = normalizedNodesX[headIndex];
			double y2 = normalizedNodesY[headIndex];
			
			double len = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
			double xOffset = LINK_OFFSET * (-y2 + y1) / len;
			double yOffset = LINK_OFFSET * (x2 - x1) / len;
			x1 += xOffset;
			y1 += yOffset;
			x2 += xOffset;
			y2 += yOffset;
			
			gc.strokeLine(x1, y1, x2, y2);
		}
		
		public boolean containsPoint(double x, double y) {
			double x1 = normalizedNodesX[tailIndex];
			double y1 = normalizedNodesY[tailIndex];
			double x2 = normalizedNodesX[headIndex];
			double y2 = normalizedNodesY[headIndex];
			
			double lenSq = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
			double len = Math.sqrt(lenSq);
			double xOffset = LINK_OFFSET * (-y2 + y1) / len;
			double yOffset = LINK_OFFSET * (x2 - x1) / len;
			x1 += xOffset;
			y1 += yOffset;
			x2 += xOffset;
			y2 += yOffset;
			
			double dot = (x - x1) * (x2 - x1) + (y - y1) * (y2 - y1);
			double p = dot / lenSq;
			
			if (p < 0 || p > 1) {
				return false;
			} else {
				double dx = x - (x1 + p * (x2 - x1));
				double dy = y - (y1 + p * (y2 - y1));
				return Math.sqrt(dx * dx + dy * dy) < LINK_WIDTH / 2;
			}
		}
	}
	
	private class NodeShape extends Shape {
		
		public NodeShape(int index) {
			super(index);
		}
		
		public void draw() {
			gc.fillOval(normalizedNodesX[index] - NODE_RADIUS,
					normalizedNodesY[index] - NODE_RADIUS,
					2 * NODE_RADIUS, 2 * NODE_RADIUS);
		}
		
		public boolean containsPoint(double x, double y) {
			double X = normalizedNodesX[index];
			double Y = normalizedNodesY[index];
			
			double dist = Math.sqrt((x - X) * (x - X) + (y - Y) * (y - Y));
			
			return dist < NODE_RADIUS;
		}
	}
}
