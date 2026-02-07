package black0ut1.data;

public class DoubleMatrix {
	
	private final double[] arr;
	
	public final int m;
	public final int n;
	
	public DoubleMatrix(int m, int n) {
		this.arr = new double[m * n];
		this.m = m;
		this.n = n;
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
	
	public DoubleMatrix scale(double factor) {
		for (int i = 0; i < arr.length; i++)
			arr[i] *= factor;
		
		return this;
	}
	
	public DoubleMatrix plus(DoubleMatrix matrix) {
		for (int i = 0; i < arr.length; i++)
			arr[i] += matrix.arr[i];
		
		return this;
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
