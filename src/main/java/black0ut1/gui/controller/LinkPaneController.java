package black0ut1.gui.controller;

import black0ut1.gui.view.LinkPane;
import javafx.scene.input.MouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.fx.FXGraphics2D;
import javafx.scene.canvas.Canvas;

import javax.swing.*;
import java.awt.*;

public class LinkPaneController {
	
	public final LinkPane linkPane;
	
	public LinkPaneController(LinkPane linkPane) {
		this.linkPane = linkPane;
	}
	
	public void onChartCanvasClicked(MouseEvent e, String title, JFreeChart chart) {
		if (e.getClickCount() == 2) {
			JFrame frame = new JFrame(title);
			frame.setSize(800, 450);
			frame.setContentPane(new ChartPanel(chart));
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
}
