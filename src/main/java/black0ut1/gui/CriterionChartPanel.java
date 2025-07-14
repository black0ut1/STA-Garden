package black0ut1.gui;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.*;

import javax.swing.*;
import java.util.*;

public class CriterionChartPanel extends JPanel {
	
	private double minvalue = 1e-4;
	
	private final Map<String, XYSeries> seriesMap = new HashMap<>();
	private final Map<String, Integer> iterations = new HashMap<>();
	
	private final LogAxis yAxis;
	private final XYSeriesCollection dataset;
	
	public CriterionChartPanel(String title) {
		dataset = new XYSeriesCollection();
		
		NumberAxis xAxis = new NumberAxis("Iterations");
		yAxis = new LogAxis("Value of convergence criterion");
		
		yAxis.setAutoRange(false);
		yAxis.setTickUnit(new NumberTickUnit(1));
		yAxis.setRange(minvalue, 10);
		
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
		
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		
		add(new ChartPanel(chart));
	}
	
	public void addValue(double value, String algorithmName) {
		XYSeries series = seriesMap.computeIfAbsent(algorithmName, name -> {
			XYSeries newSeries = new XYSeries(name);
			dataset.addSeries(newSeries);
			return newSeries;
		});
		
		int iteration = iterations.getOrDefault(algorithmName, 0);
		series.add(iteration, value);
		iterations.put(algorithmName, iteration + 1);
		
		// Update Y-axis range
		minvalue = Math.min(minvalue, value);
		double logFloor = Math.pow(10, Math.floor(Math.log10(minvalue)));
		yAxis.setRange(logFloor, 10);
	}
}
