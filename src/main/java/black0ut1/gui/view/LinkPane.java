package black0ut1.gui.view;

import black0ut1.dynamic.loading.link.Link;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.fx.FXGraphics2D;

import java.awt.*;

public class LinkPane extends VBox {
	
	public LinkPane(Link link) {
		super();
		setPadding(new Insets(10));
		setMinWidth(0);
		
		Text title = new Text("Link " + link.index);
		title.setFont(Font.font(null, FontWeight.BOLD, 30));
		
		Text model = new Text("Model: " + link.getClass().getSimpleName());
		Text length = new Text("Length: " + link.length);
		Text head = new Text("Head: " + link.head.index);
		Text tail = new Text("Tail: " + link.tail.index);
		
		Text FDtitle = new Text("Fundamental diagram");
		FDtitle.setFont(Font.font(null, FontWeight.BOLD, 15));
		VBox.setMargin(FDtitle, new Insets(10, 0, 0, 0));
		
		Text capacity = new Text("Capacity: " + link.capacity);
		Text jamDensity = new Text("Jam density: " + link.jamDensity);
		Text freeFlowSpeed = new Text("Free flow speed: " + link.freeFlowSpeed);
		Text backwardWaveSpeed = new Text("Backward wave speed: " + link.backwardWaveSpeed);
		
		getChildren().addAll(title, model, length, head, tail,
				FDtitle, capacity, jamDensity, freeFlowSpeed, backwardWaveSpeed, getFDplot(link));
	}
	
	public Node getFDplot(Link link) {
		XYSeries series = new XYSeries("");
		series.add(0, 0);
		series.add(link.capacity / link.freeFlowSpeed, link.capacity);
		series.add(link.jamDensity, 0);
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createXYLineChart(null, "Density", "Flow", dataset);
		chart.removeLegend();
		
		Canvas canvas = new Canvas(0, 200);
		var gc = canvas.getGraphicsContext2D();
		var g2 = new FXGraphics2D(gc);
		canvas.widthProperty().bind(this.widthProperty().add(-20));
		canvas.widthProperty().addListener((_, _, newValue) -> {
			chart.draw(g2, new Rectangle(0, 0, newValue.intValue(), (int) canvas.getHeight()));
			gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
		});
		
		return canvas;
	}
}
