package black0ut1.gui.view;

import black0ut1.dynamic.loading.link.Link;
import black0ut1.gui.MainGUI;
import black0ut1.gui.controller.LinkPaneController;
import black0ut1.gui.model.Model;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.fx.FXGraphics2D;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

public class LinkPane extends ScrollPane {
	
	public final LinkPaneController controller;
	public final Link link;
	
	public LinkPane(Link link) {
		super();
		this.link = link;
		this.controller = new LinkPaneController(this);
		
		getRoot();
		
		Model.getInstance().currentTimeProperty.set(Model.getInstance().currentTimeProperty.get() + 1);
		Model.getInstance().currentTimeProperty.set(Model.getInstance().currentTimeProperty.get() - 1);
	}
	
	public void getRoot() {
		VBox root = new VBox();
		root.setPadding(new Insets(10));
		root.setMinWidth(0);
		
		TextFlow FDtitle = titleWithTooltip("Fundamental diagram", "Double click the chart to enlarge");
		TextFlow CVCtitle = titleWithTooltip("Cumulative vehicle counts", "Double click the chart to enlarge");
		TextFlow TTtitle = titleWithTooltip("Travel time", "Double click the chart to enlarge");
		TextFlow flowTitle = titleWithTooltip("Inflow and outflow", "Double click the chart to enlarge");
		
		root.getChildren().addAll(getDetailsPane(),
				FDtitle, getFDplot(),
				CVCtitle, getCVCplot(),
				TTtitle, getTTplot(),
				flowTitle, getFlowPlot()
		);
		setContent(root);
	}
	
	public Node getDetailsPane() {
		VBox detailsPane = new VBox();
		detailsPane.setPadding(new Insets(10));
		
		Text title = new Text("Link " + link.index);
		title.setFont(Font.font(null, FontWeight.BOLD, 30));
		
		Text model = new Text("Model: " + link.getClass().getSimpleName());
		Text head = new Text("Head: " + link.head.index);
		Text tail = new Text("Tail: " + link.tail.index);
		
		detailsPane.getChildren().addAll(title, model, head, tail);
		
		////////////////////////
		
		String[] texts = {"Length", "Capacity", "Jam density", "Free flow speed", "Backward wave speed"};
		double[] values = {link.length, link.capacity, link.jamDensity, link.freeFlowSpeed, link.backwardWaveSpeed};
		
		for (int i = 0; i < texts.length; i++) {
			Text t = new Text(texts[i] + ": ");
			t.setFont(Font.font(null, FontWeight.BOLD, t.getFont().getSize()));
			TextFlow tf = new TextFlow(t, new Text(String.valueOf(values[i])));
			detailsPane.getChildren().add(tf);
		}
		
		return detailsPane;
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
	
	public Node getFDplot() {
		XYSeries series = new XYSeries("");
		series.add(0, 0);
		series.add(link.capacity / link.freeFlowSpeed, link.capacity);
		series.add(link.jamDensity, 0);
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createXYLineChart(null, "Density", "Flow", dataset);
		chart.removeLegend();
		
		Canvas canvas = new Canvas(400, 200);
		var gc = canvas.getGraphicsContext2D();
		var g2 = new FXGraphics2D(gc);
		chart.draw(g2, new Rectangle(0, 0, (int) canvas.getWidth(), (int) canvas.getHeight()));
		gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.setOnMouseClicked(e -> controller.onChartCanvasClicked(e, "Fundamental diagram", chart));
		
		return canvas;
	}
	
	public Node getCVCplot() {
		XYSeries cumulativeInflow = new XYSeries("Cumulative inflow");
		XYSeries cumulativeOutflow = new XYSeries("Cumulative outflow");
		
		for (int t = 0; t < link.cumulativeInflow.length; t++) {
			cumulativeInflow.add(t, link.cumulativeInflow[t]);
			cumulativeOutflow.add(t, link.cumulativeOutflow[t]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(cumulativeInflow);
		dataset.addSeries(cumulativeOutflow);
		
		return createTimeChart(dataset, "Time", "Vehicle count", "Cumulative vehicle count",
				(_, r) -> {
					r.setSeriesPaint(0, new Color(0, 0, 255, 128));
					r.setSeriesPaint(1, new Color(255, 0, 0, 128));
					r.setSeriesShapesVisible(0, false);
					r.setSeriesShapesVisible(1, false);
				},
				time -> {
					double cinflow = link.cumulativeInflow[time];
					double coutflow = link.cumulativeOutflow[time];
					return String.format("%.3f%n%.3f", cinflow, coutflow);
				}
		);
	}
	
	public Node getTTplot() {
		XYSeries travelTime = new XYSeries("Travel time");
		for (int t = 0; t < MainGUI.timeSteps; t++)
			if (MainGUI.travelTimes[link.index][t] != Double.POSITIVE_INFINITY)
				travelTime.add(t, MainGUI.travelTimes[link.index][t]);
		
		double freeFlowTime = link.length / link.freeFlowSpeed;
		XYSeries freeFlow = new XYSeries("Free flow travel time");
		freeFlow.add(0, freeFlowTime);
		freeFlow.add(MainGUI.timeSteps, freeFlowTime);
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(travelTime);
		dataset.addSeries(freeFlow);
		
		return createTimeChart(dataset, "Time", "Travel time", "Travel time",
				(_, r) -> {
					r.setSeriesPaint(0, Color.RED);
					r.setSeriesPaint(1, new Color(0, 128, 0));
					r.setSeriesShapesVisible(0, false);
					r.setSeriesShapesVisible(1, false);
				},
				time -> String.format("%.3f", MainGUI.travelTimes[link.index][time])
		);
	}
	
	public Node getFlowPlot() {
		XYSeries cumulativeInflow = new XYSeries("Inflow");
		XYSeries cumulativeOutflow = new XYSeries("Outflow");
		
		for (int t = 0; t < link.inflow.length; t++) {
			cumulativeInflow.add(t, link.inflow[t].totalFlow);
			cumulativeOutflow.add(t, link.outflow[t].totalFlow);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(cumulativeInflow);
		dataset.addSeries(cumulativeOutflow);
		
		return createTimeChart(dataset, "Time", "Flow", "Inflow and outflow",
				(_, r) -> {
					r.setSeriesPaint(0, new Color(0, 0, 255, 128));
					r.setSeriesPaint(1, new Color(255, 0, 0, 128));
					r.setSeriesShapesVisible(0, false);
					r.setSeriesShapesVisible(1, false);
				},
				time -> {
					double cinflow = link.inflow[time].totalFlow;
					double coutflow = link.outflow[time].totalFlow;
					return String.format("%.3f%n%.3f", cinflow, coutflow);
				}
		);
	}
	
	private Canvas createTimeChart(XYSeriesCollection dataset, String xLabel, String yLabel, String popupTitle,
	                               BiConsumer<XYPlot, XYLineAndShapeRenderer> plotCustomizer, IntFunction<String> markerLabelSupplier) {
		JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, yLabel, dataset);
		
		XYPlot plot = chart.getXYPlot();
		double maxY = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (Object series : dataset.getSeries()) {
			maxY = Math.max(maxY, ((XYSeries) series).getMaxY());
			minY = Math.min(minY, ((XYSeries) series).getMinY());
		}
		plot.getRangeAxis().setUpperBound(maxY * 1.2);
		plot.getRangeAxis().setLowerBound(minY * 0.8);
		
		if (plotCustomizer != null) {
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
			plotCustomizer.accept(plot, r);
			plot.setRenderer(r);
		}
		
		int time = Model.getInstance().currentTimeProperty.get();
		ValueMarker marker = new ValueMarker(time);
		marker.setPaint(Color.BLACK);
		marker.setLabelBackgroundColor(Color.WHITE);
		marker.setLabelFont(new java.awt.Font(null, java.awt.Font.BOLD, 12));
		marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
		if (markerLabelSupplier != null)
			marker.setLabel(markerLabelSupplier.apply(time));
		plot.addDomainMarker(marker);
		
		Canvas canvas = new Canvas(400, 200);
		
		canvas.setOnMouseClicked(e ->
				controller.onChartCanvasClicked(e, popupTitle, chart)
		);
		
		LinkPaneController.TimeBinding tb = new LinkPaneController.TimeBinding(
				marker, markerLabelSupplier, canvas, chart);
		controller.timeBindings.add(tb);
		
		return canvas;
	}
}
