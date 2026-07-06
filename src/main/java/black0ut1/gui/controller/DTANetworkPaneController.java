package black0ut1.gui.controller;

import black0ut1.gui.Constants;
import black0ut1.gui.model.Model;
import black0ut1.gui.view.DTANetworkPane;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class DTANetworkPaneController {
	
	public final DTANetworkPane pane;
	
	private Point2D tmpOffset = null;
	private Point2D dragStart = null;
	
	public DTANetworkPaneController(DTANetworkPane pane) {
		this.pane = pane;
	}
	
	public void onMouseClicked(MouseEvent e) {
		if (!e.isStillSincePress())
			return;
		
		double x = (e.getX() - pane.getWidth() / 2) / pane.scale - pane.offset.getX();
		double y = (e.getY() - pane.getHeight() / 2) / pane.scale - pane.offset.getY();
		
		DTANetworkPane.Shape clickedShape = null;
		for (DTANetworkPane.LinkShape linkShape : pane.linkShapes) {
			if (linkShape.containsPoint(x, y))
				clickedShape = linkShape;
		}
		for (DTANetworkPane.NodeShape nodeShape : pane.nodeShapes) {
			if (nodeShape.containsPoint(x, y))
				clickedShape = nodeShape;
		}
		
		Model.getInstance().selectedShapeProperty.set(clickedShape);
	}
	
	public void onMouseMoved(MouseEvent e) {
		// transformation of canvas coordinates into node coordinates
		// (accounting for affine transforms)
		double x = (e.getX() - pane.getWidth() / 2) / pane.scale - pane.offset.getX();
		double y = (e.getY() - pane.getHeight() / 2) / pane.scale - pane.offset.getY();
		
		DTANetworkPane.Shape hoverShape = null;
		for (DTANetworkPane.LinkShape linkShape : pane.linkShapes) {
			if (linkShape.containsPoint(x, y))
				hoverShape = linkShape;
		}
		for (DTANetworkPane.NodeShape nodeShape : pane.nodeShapes) {
			if (nodeShape.containsPoint(x, y))
				hoverShape = nodeShape;
		}
		
		Model.getInstance().hoveredShapeProperty.set(hoverShape);
	}
	
	public void onScroll(ScrollEvent e) {
		double rot = e.getDeltaY();
		pane.scale += Constants.WHEEL_ROTATION_SCALE_FACTOR * rot * Math.abs(pane.scale / 200);
		if (pane.scale < 0)
			pane.scale = 0;
		
		pane.paint();
	}
	
	public void onMousePressed(MouseEvent e) {
		tmpOffset = pane.offset;
		this.dragStart = new Point2D(e.getX(), e.getY());
	}
	
	public void onMouseDragged(MouseEvent e) {
		double dX = (e.getX() - dragStart.getX()) / pane.scale;
		double dY = (e.getY() - dragStart.getY()) / pane.scale;
		pane.offset = tmpOffset.add(dX, dY);
		
		pane.paint();
	}
}
