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
		return arr[i * m + j];
	}

	public void set(int i, int j, double value) {
		arr[i * m + j] = value;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++)
				sb.append(arr[i * n + j]).append(" ");
			sb.append('\n');
		}
		
		return sb.toString();
	}
}
