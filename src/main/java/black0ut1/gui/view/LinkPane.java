package black0ut1.gui.view;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.gui.controller.LinkPaneController;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;

public class LinkPane extends VBox {
	
	public final LinkPaneController controller;
	
	public LinkPane(Link link) {
		super();
		setPadding(new Insets(10));
		setMinWidth(0);
		
		this.controller = new LinkPaneController(this);
		
		
		Text title = new Text("Link " + link.index);
		title.setFont(Font.font(null, FontWeight.BOLD, 30));
		
		Text model = new Text("Model: " + link.getClass().getSimpleName());
		Text length = new Text("Length: " + link.length);
		Text head = new Text("Head: " + link.head.index);
		Text tail = new Text("Tail: " + link.tail.index);
		
		TextFlow FDtitle = titleWithTooltip("Fundamental diagram", "Double click the chart to enlarge");
		
		Text capacity = new Text("Capacity: " + link.capacity);
		Text jamDensity = new Text("Jam density: " + link.jamDensity);
		Text freeFlowSpeed = new Text("Free flow speed: " + link.freeFlowSpeed);
		Text backwardWaveSpeed = new Text("Backward wave speed: " + link.backwardWaveSpeed);
		
		TextFlow CVCtitle = titleWithTooltip("Cumulative vehicle counts", "Double click the chart to enlarge");
		
		getChildren().addAll(title, model, length, head, tail,
				FDtitle, capacity, jamDensity, freeFlowSpeed, backwardWaveSpeed, getFDplot(link),
				CVCtitle, getCVCplot(link));
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
		canvas.widthProperty().bind(this.widthProperty().add(-20));
		canvas.widthProperty().addListener((_, _, newValue) ->
				controller.onChartCanvasWidthChange(newValue.intValue(), canvas, chart));
		canvas.setOnMouseClicked(e -> controller.onChartCanvasClicked(e, "Fundamental diagram", chart));
		
		return canvas;
	}
	
	public Node getCVCplot(Link link) {
		XYSeries cumulativeInflow = new XYSeries("Cumulative inflow");
		for (int t = 0; t < link.cumulativeInflow.length; t++) {
			if (t > 0 && link.cumulativeInflow[t - 1] > 0 && link.cumulativeInflow[t] == 0)
				break;
			
			cumulativeInflow.add(t, link.cumulativeInflow[t]);
		}
		
		XYSeries cumulativeOutflow = new XYSeries("Cumulative outflow");
		for (int t = 0; t < link.cumulativeOutflow.length; t++) {
			if (t > 0 && link.cumulativeInflow[t - 1] > 0 && link.cumulativeInflow[t] == 0)
				break;
			
			cumulativeOutflow.add(t, link.cumulativeOutflow[t]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(cumulativeInflow);
		dataset.addSeries(cumulativeOutflow);
		
		JFreeChart chart = ChartFactory.createXYLineChart(null, "Time", "Vehicle count", dataset);
		
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, new Color(0, 0, 255, 128));
		renderer.setSeriesPaint(1, new Color(255, 0, 0, 128));
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setDrawSeriesLineAsPath(false);
		chart.getXYPlot().setRenderer(renderer);
		
		chart.getXYPlot().getRangeAxis().setLowerBound(0);
		
		Canvas canvas = new Canvas(0, 200);
		canvas.widthProperty().bind(this.widthProperty().add(-20));
		canvas.widthProperty().addListener((_, _, newValue) ->
				controller.onChartCanvasWidthChange(newValue.intValue(), canvas, chart));
		canvas.setOnMouseClicked(e -> controller.onChartCanvasClicked(e, "Cumulative vehicle count", chart));
		
		return canvas;
	}
	
	public TextFlow titleWithTooltip(String titleText, String tooltipText) {
		Text title = new Text(titleText + " ");
		title.setFont(Font.font(null, FontWeight.BOLD, 15));
		
		Text questionMark = new Text("(?)");
		questionMark.setFont(Font.font(null, FontWeight.BOLD, 15));
		questionMark.setUnderline(true);
		
		Tooltip tooltip = new Tooltip(tooltipText);
		tooltip.setShowDelay(new Duration(500));
		Tooltip.install(questionMark, tooltip);
		
		TextFlow tf = new TextFlow(title, questionMark);
		VBox.setMargin(tf, new Insets(10, 0, 0, 0));
		return tf;
	}
}
