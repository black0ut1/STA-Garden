package black0ut1.data;

@SuppressWarnings("unchecked")
public class Matrix<T> {
	
	private final T[] arr;
	
	public final int m;
	public final int n;
	
	public Matrix(int m, int n) {
		this.arr = (T[]) new Object[m * n];
		this.m = m;
		this.n = n;
	}
	
	public Matrix(int n) {
		this(n, n);
	}
	
	public T get(int i, int j) {
		return arr[i * n + j];
	}
	
	public void set(int i, int j, T value) {
		arr[i * n + j] = value;
	}
}