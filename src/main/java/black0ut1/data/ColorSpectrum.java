package black0ut1.data;

import java.awt.*;

public class ColorSpectrum {
	
	private final Color[] colors;
	
	public ColorSpectrum(Color... colors) {
		this.colors = colors;
	}
	
	public Color getColor(double value) {
		if (value > 1 || value < 0) {
			throw new IllegalArgumentException("Value must be between 0 and 1 (included). Value: " + value);
		}
		
		if (value == 1)
			return colors[colors.length - 1];
		
		double segmentLen = 1.0 / (colors.length - 1);
		int segment = (int) (value / segmentLen);
		double segmentValue = value - segment * segmentLen;
		
		int r = interpolateColor(colors[segment].getRed(), colors[segment + 1].getRed(), segmentValue);
		int g = interpolateColor(colors[segment].getGreen(), colors[segment + 1].getGreen(), segmentValue);
		int b = interpolateColor(colors[segment].getBlue(), colors[segment + 1].getBlue(), segmentValue);
		return new Color(r, g, b);
	}
	
	private static int interpolateColor(int x1, int x2, double val) {
		return x1 + (int) Math.round(val * (x2 - x1));
	}
}
