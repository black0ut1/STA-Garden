package black0ut1.gui.view;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.gui.MainGUI;
import black0ut1.gui.controller.LinkPaneController;
import black0ut1.gui.model.Model;
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
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;

public class LinkPane extends VBox {
	
	public ValueMarker cvcMarker;
	public JFreeChart cvcChart;
	public Canvas cvcCanvas;
	
	public ValueMarker ttMarker;
	public JFreeChart ttChart;
	public Canvas ttCanvas;
	
	public final LinkPaneController controller;
	public final Link link;
	
	public LinkPane(Link link) {
		super();
		this.link = link;
		this.controller = new LinkPaneController(this);
		
		setPadding(new Insets(10));
		setMinWidth(0);
		
		Model.getInstance().currentTimeProperty.addListener(controller::onCurrentTimeChanged);
		
		getRoot();
	}
	
	public void getRoot() {
		Text title = new Text("Link " + link.index);
		title.setFont(Font.font(null, FontWeight.BOLD, 30));
		
		Text model = new Text("Model: " + link.getClass().getSimpleName());
		Text head = new Text("Head: " + link.head.index);
		Text tail = new Text("Tail: " + link.tail.index);
		
		getChildren().addAll(title, model, head, tail);
		
		////////////////////////
		
		String[] texts = {"Length", "Capacity", "Jam density", "Free flow speed", "Backward wave speed"};
		double[] values = {link.length, link.capacity, link.jamDensity, link.freeFlowSpeed, link.backwardWaveSpeed};
		
		for (int i = 0; i < texts.length; i++) {
			Text t = new Text(texts[i] + ": ");
			t.setFont(Font.font(null, FontWeight.BOLD, t.getFont().getSize()));
			TextFlow tf = new TextFlow(t, new Text(String.valueOf(values[i])));
			getChildren().add(tf);
		}
		
		////////////////////////
		
		TextFlow FDtitle = titleWithTooltip("Fundamental diagram", "Double click the chart to enlarge");
		TextFlow CVCtitle = titleWithTooltip("Cumulative vehicle counts", "Double click the chart to enlarge");
		TextFlow TTtitle = titleWithTooltip("Travel time", "Double click the chart to enlarge");
		
		////////////////////////
		
		Text detailsTitle = new Text("Details");
		detailsTitle.setFont(Font.font(null, FontWeight.BOLD, 15));
		VBox.setMargin(detailsTitle, new Insets(10, 0, 0, 0));
		
		Text volume = new Text("Volume: ");
		volume.setFont(Font.font(null, FontWeight.BOLD, volume.getFont().getSize()));
		Text volumeValue = new Text();
		volumeValue.textProperty().bindBidirectional(
				Model.getInstance().currentTimeProperty, controller.timeToVolumeConverter);
		
		Text travelTime = new Text("Travel time: ");
		travelTime.setFont(Font.font(null, FontWeight.BOLD, volume.getFont().getSize()));
		Text travelTimeValue = new Text();
		travelTimeValue.textProperty().bindBidirectional(
				Model.getInstance().currentTimeProperty, controller.timeToTravelTimeConverter);
		
		////////////////////////
		
		getChildren().addAll(
				FDtitle, getFDplot(),
				CVCtitle, getCVCplot(),
				TTtitle, getTTplot(),
				detailsTitle, new TextFlow(volume, volumeValue), new TextFlow(travelTime, travelTimeValue)
		);
	}
	
	public Node getFDplot() {
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
	
	public Node getCVCplot() {
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
		
		cvcChart = ChartFactory.createXYLineChart(null, "Time", "Vehicle count", dataset);
		
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, new Color(0, 0, 255, 128));
		renderer.setSeriesPaint(1, new Color(255, 0, 0, 128));
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setDrawSeriesLineAsPath(false);
		cvcChart.getXYPlot().setRenderer(renderer);
		
		cvcChart.getXYPlot().getRangeAxis().setLowerBound(0);
		
		int time = Model.getInstance().currentTimeProperty.get();
		cvcMarker = new ValueMarker(time);
		cvcMarker.setPaint(Color.MAGENTA);
		cvcChart.getXYPlot().addDomainMarker(cvcMarker);
		
		cvcCanvas = new Canvas(0, 200);
		cvcCanvas.widthProperty().bind(this.widthProperty().add(-20));
		cvcCanvas.widthProperty().addListener(controller::onChartCanvasWidthChange);
		cvcCanvas.setOnMouseClicked(e -> controller.onChartCanvasClicked(e, "Cumulative vehicle count", cvcChart));
		
		return cvcCanvas;
	}
	
	public Node getTTplot() {
		XYSeries series = new XYSeries("");
		XYSeries infinities = new XYSeries("Infinity");
		for (int t = 0; t < MainGUI.timeSteps; t++) {
			if (MainGUI.travelTimes[link.index][t] == Double.POSITIVE_INFINITY)
				infinities.add(t, 0);
			else
				series.add(t, MainGUI.travelTimes[link.index][t]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series);
		dataset.addSeries(infinities);
		
		ttChart = ChartFactory.createXYLineChart(null, "Time", "Travel time", dataset);
		ttChart.removeLegend();
		ttChart.getXYPlot().getDomainAxis().setUpperBound(MainGUI.timeSteps);
		ttChart.getXYPlot().getRangeAxis().setUpperBound(series.getMaxY() * 1.1);
		
		int time = Model.getInstance().currentTimeProperty.get();
		ttMarker = new ValueMarker(time);
		ttMarker.setPaint(Color.MAGENTA);
		ttChart.getXYPlot().addDomainMarker(ttMarker);
		
		double freeFlowTime = link.length / link.freeFlowSpeed;
		ValueMarker freeFlowTTMarker = new ValueMarker(freeFlowTime);
		freeFlowTTMarker.setPaint(Color.GREEN);
		ttChart.getXYPlot().addRangeMarker(freeFlowTTMarker);
		
		ttCanvas = new Canvas(0, 200);
		ttCanvas.widthProperty().bind(this.widthProperty().add(-20));
		ttCanvas.widthProperty().addListener((_, _, newValue) ->
				controller.onChartCanvasWidthChange(newValue.intValue(), ttCanvas, ttChart));
		ttCanvas.setOnMouseClicked(e -> controller.onChartCanvasClicked(e, "Travel time", ttChart));
		
		return ttCanvas;
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
