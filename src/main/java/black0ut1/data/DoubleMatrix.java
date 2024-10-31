package black0ut1.data;

public class DoubleMatrix {
	
	private final double[] arr;
	
	public final int n;
	
	public DoubleMatrix(int n) {
		this.arr = new double[n * n];
		this.n = n;
	}

	public double get(int i, int j) {
		return arr[i * n + j];
	}

	public void set(int i, int j, double value) {
		arr[i * n + j] = value;
	}
}
