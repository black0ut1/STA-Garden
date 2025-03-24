package black0ut1.data;

public class DoubleMatrix {
	
	private final double[] arr;
	
	public final int n;
	public final int m;
	
	public DoubleMatrix(int n, int m) {
		this.arr = new double[n * m];
		this.n = n;
		this.m = m;
	}
	
	public DoubleMatrix(int n) {
		this(n, n);
	}

	public double get(int i, int j) {
		return arr[i * n + j];
	}

	public void set(int i, int j, double value) {
		arr[i * n + j] = value;
	}
}
