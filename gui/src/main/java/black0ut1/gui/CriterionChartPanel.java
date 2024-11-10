package black0ut1.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class CriterionChartPanel extends JPanel {
	
	private static final double MIN_VALUE = 1e-4;
	
	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	private final Map<String, Integer> iterations = new HashMap<>();
	
	private final LogAxis yAxis;
	
	public CriterionChartPanel(String title) {
		CategoryAxis xAxis = new CategoryAxis("Iterations");
		this.yAxis = new LogAxis("Value");
		
		yAxis.setAutoRange(false);
		yAxis.setTickUnit(new NumberTickUnit(1));
		yAxis.setRange(MIN_VALUE, 10);
		
		for (int i = 0; i < 10; i++)
			dataset.addValue(0, "", Integer.valueOf(i));
		
		LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
		CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		JFreeChart chart = new JFreeChart(title, plot);
		
		add(new ChartPanel(chart));
	}
	
	public void addValue(double value, String algorithmName) {
		if (!iterations.containsKey(algorithmName))
			iterations.put(algorithmName, 0);
		
		Integer i = iterations.get(algorithmName);
		dataset.addValue(value, algorithmName, i);
		iterations.put(algorithmName, i + 1);
		
		double logFloor = Math.pow(10, Math.floor(Math.log10(value)));
		yAxis.setRange(Math.min(logFloor, MIN_VALUE), 10);
	}
}
