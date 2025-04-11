package black0ut1.data;

public class IntSet {
	
	protected int word = 0;
	public final int size;
	
	public IntSet(int size) {
		this.size = size;
	}
	
	public void add(int i) {
		int mask = 1 << i;
		word |= mask;
	}
	
	public void remove(int i) {
		int mask = 1 << i;
		word &= ~mask;
	}
	
	public void removeAll(IntSet set) {
		word &= ~set.word;
	}
	
	public boolean isEmpty() {
		return word == 0;
	}
	
	public boolean contains(int i) {
		int mask = 1 << i;
		return (word & mask) != 0;
	}
}
