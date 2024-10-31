package black0ut1.data;

import java.util.Iterator;

public class ArrayView<T> implements Iterator<T>, Iterable<T> {
	
	private final T[] source;
	
	private final int last;
	
	private int first;

	public ArrayView(T[] source, int first, int last) {
		this.source = source;
		this.first = first;
		this.last = last;
	}
	
	@Override
	public boolean hasNext() {
		return first < last;
	}
	
	@Override
	public T next() {
		return source[first++];
	}
	
	@Override
	public Iterator<T> iterator() {
		return this;
	}
	
	public int size() {
		return last - first;
	}
}
