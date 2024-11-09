package black0ut1.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;

public class CriterionGraphPanel extends JPanel {
	
	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	
	public CriterionGraphPanel(String title) {
		add(new ChartPanel(ChartFactory.createLineChart(
				title, "Iterations", "Value", dataset
		)));
	}
	
	public void addValue(double value, String algorithmName, int iteration) {
		dataset.addValue(value, algorithmName, Integer.valueOf(iteration));
	}
}
