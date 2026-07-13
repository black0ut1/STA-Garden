package black0ut1.data;

public class BitSet32 {
	
	protected int word = 0;
	public final int size;
	
	public BitSet32(int size) {
		if (size > 32)
			throw new IllegalArgumentException("BitSet32 supports only sizes up to 32. Size: " + size);
		
		this.size = size;
	}
	
	public static BitSet32 filled(int size) {
		BitSet32 set = new BitSet32(size);
		set.word = 0xFFFFFFFF;
		return set;
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++)
			sb.append(get(i) ? "1" : "0");
		return sb.reverse().toString();
	}
}
