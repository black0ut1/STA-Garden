package black0ut1.data;

public class BitSet32 {
	
	protected int word = 0;
	public final int size;
	
	public BitSet32(int size) {
		this.size = size;
	}
	
	public void set(int bit) {
		int mask = 1 << bit;
		word |= mask;
	}
	
	public void clear(int bit) {
		int mask = 1 << bit;
		word &= ~mask;
	}
	
	public boolean get(int bit) {
		int mask = 1 << bit;
		return (word & mask) != 0;
	}
	
	public void clearAll(BitSet32 set) {
		word &= ~set.word;
	}
	
	public boolean isClear() {
		return word == 0;
	}
}
