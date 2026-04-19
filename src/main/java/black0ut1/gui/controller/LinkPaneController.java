package black0ut1.gui.controller;

import black0ut1.gui.MainGUI;
import black0ut1.gui.view.LinkPane;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.fx.FXGraphics2D;
import javafx.scene.canvas.Canvas;

import javax.swing.*;
import java.awt.*;

public class LinkPaneController {
	
	public final StringConverter<Number> timeToVolumeConverter = new StringConverter<>() {
		@Override
		public String toString(Number object) {
			int time = object.intValue();
			double volume = linkPane.link.cumulativeInflow[time] - linkPane.link.cumulativeOutflow[time];
			return String.format("%.2f", volume);
		}
		
		@Override
		public Number fromString(String string) {
			return null;
		}
	};
	
	public final StringConverter<Number> timeToTravelTimeConverter = new StringConverter<>() {
		@Override
		public String toString(Number object) {
			int time = object.intValue();
			return String.format("%.2f", MainGUI.travelTimes[linkPane.link.index][time]);
		}
		
		@Override
		public Number fromString(String string) {
			return null;
		}
	};
	
	public final LinkPane linkPane;
	
	private final Object lock = new Object();
	
	public LinkPaneController(LinkPane linkPane) {
		this.linkPane = linkPane;
	}
	
	public void onChartCanvasClicked(MouseEvent e, String title, JFreeChart chart) {
		if (e.getClickCount() == 2) {
			JFrame frame = new JFrame(title);
			frame.setSize(800, 450);
			frame.setContentPane(new ChartPanel(chart) {
				@Override
				public void paintComponent(Graphics g) {
					synchronized (lock) {
						super.paintComponent(g);
					}
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}
	}
	
	public void onChartCanvasWidthChange(int newWidth, Canvas canvas, JFreeChart chart) {
		var gc = canvas.getGraphicsContext2D();
		var g2 = new FXGraphics2D(gc);
		chart.draw(g2, new Rectangle(0, 0, newWidth, (int) canvas.getHeight()));
		gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}
	
	public void onChartCanvasWidthChange(ObservableValue<? extends Number> observable,
										 Number oldValue, Number newValue) {
		synchronized (lock) {
			var gc = linkPane.cvcCanvas.getGraphicsContext2D();
			var g2 = new FXGraphics2D(gc);
			linkPane.cvcChart.draw(g2, new Rectangle(0, 0, newValue.intValue(), (int) linkPane.cvcCanvas.getHeight()));
			gc.strokeRect(0, 0, linkPane.cvcCanvas.getWidth(), linkPane.cvcCanvas.getHeight());
			
			gc = linkPane.ttCanvas.getGraphicsContext2D();
			g2 = new FXGraphics2D(gc);
			linkPane.ttChart.draw(g2, new Rectangle(0, 0, newValue.intValue(), (int) linkPane.ttCanvas.getHeight()));
			gc.strokeRect(0, 0, linkPane.ttCanvas.getWidth(), linkPane.ttCanvas.getHeight());
		}
	}
	
	public void onCurrentTimeChanged(Observable observable, Number oldValue, Number newValue) {
		synchronized (lock) {
			linkPane.cvcMarker.setValue(newValue.intValue());
			
			var gc = linkPane.cvcCanvas.getGraphicsContext2D();
			var g2 = new FXGraphics2D(gc);
			linkPane.cvcChart.draw(g2, new Rectangle(0, 0,
					(int) linkPane.cvcCanvas.getWidth(), (int) linkPane.cvcCanvas.getHeight()));
			gc.strokeRect(0, 0, linkPane.cvcCanvas.getWidth(), linkPane.cvcCanvas.getHeight());
			
			linkPane.ttMarker.setValue(newValue.intValue());
			
			gc = linkPane.ttCanvas.getGraphicsContext2D();
			g2 = new FXGraphics2D(gc);
			linkPane.ttChart.draw(g2, new Rectangle(0, 0,
					(int) linkPane.ttCanvas.getWidth(), (int) linkPane.ttCanvas.getHeight()));
			gc.strokeRect(0, 0, linkPane.ttCanvas.getWidth(), linkPane.ttCanvas.getHeight());
		}
	}
}
