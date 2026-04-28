package black0ut1.gui.controller;

import black0ut1.gui.MainGUI;
import black0ut1.gui.model.Model;
import black0ut1.gui.view.LinkPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.fx.FXGraphics2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class LinkPaneController {
	
	public final LinkPane linkPane;
	public final List<TimeBinding> timeBindings = new ArrayList<>();
	
	public LinkPaneController(LinkPane linkPane) {
		this.linkPane = linkPane;
		
		Model.getInstance().currentTimeProperty.addListener((_, _, newValue) -> {
			int time = newValue.intValue();
			
			for (TimeBinding tb : timeBindings) {
				// change marker
				tb.marker.setValue(time);
				if (tb.labelSupplier != null)
					tb.marker.setLabel(tb.labelSupplier.apply(time));
				if (newValue.intValue() <= MainGUI.timeSteps / 2) {
					tb.marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
					tb.marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
				} else {
					tb.marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
					tb.marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
				}
				
				// redraw canvas
				synchronized (tb.chart) {
					var gc = tb.canvas.getGraphicsContext2D();
					var g2 = new FXGraphics2D(gc);
					tb.chart.draw(g2, new Rectangle(0, 0,
							(int) tb.canvas.getWidth(), (int) tb.canvas.getHeight()));
					gc.strokeRect(0, 0, tb.canvas.getWidth(), tb.canvas.getHeight());
				}
			}
		});
	}
	
	public void onChartCanvasClicked(MouseEvent e, String title, JFreeChart chart) {
		if (e.getClickCount() == 2) {
			JFrame frame = new JFrame(title);
			frame.setSize(800, 450);
			frame.setContentPane(new ChartPanel(chart) {
				@Override
				public void paintComponent(Graphics g) {
					synchronized (chart) {
						super.paintComponent(g);
					}
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}
	}
	
	public record TimeBinding(
			ValueMarker marker,
			IntFunction<String> labelSupplier,
			Canvas canvas,
			JFreeChart chart
	) {}
}
