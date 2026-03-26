package black0ut1.data;

import java.util.Arrays;

public class PriorityQueue {
	
	private int[] values;
	private double[] priorities;
	
	private final double growthFactor;
	private int count = 0;
	
	public PriorityQueue() {
		this(500, 1.5);
	}
	
	public PriorityQueue(int initialCapacity, double growthFactor) {
		this.values = new int[initialCapacity + 1];
		this.priorities = new double[initialCapacity + 1];
		this.growthFactor = growthFactor;
	}
	
	public void add(int value, double priority) {
		if (count == values.length - 1)
			expand();
		
		count++;
		priorities[count] = priority;
		values[count] = value;
		fixUp(count);
	}
	
	public double getMinPriority() {
		return priorities[1];
	}
	
	public int popMin() {
		int min = values[1];
		
		values[1] = values[count];
		priorities[1] = priorities[count];
		count--;
		fixDown(1);
		
		return min;
	}
	
	public void setLowerPriority(int value, double newPriority) {
		for (int i = 1; i <= count; i++)
			if (values[i] == value) {
				priorities[i] = newPriority;
				fixUp(i);
				return;
			}
	}
	
	public void decreasePriority(int value, double priorityDelta) {
		for (int i = 1; i <= count; i++)
			if (values[i] == value) {
				priorities[i] -= priorityDelta;
				fixUp(i);
				return;
			}
	}
	
	public boolean isEmpty() {
		return count == 0;
	}
	
	public void reset() {
		count = 0;
	}
	
	private void fixUp(int n) {
		
		while (n != 1) {
			int p = n / 2;
			
			if (priorities[p] > priorities[n]) {
				swap(p, n);
				n = p;
			} else
				return;
		}
	}
	
	private void fixDown(int n) {
		while (2 * n <= count) {
			int j = 2 * n;
			
			if (j + 1 <= count)
				if (priorities[j + 1] < priorities[j])
					j++;
			
			if (priorities[n] < priorities[j])
				return;
			else {
				swap(j, n);
				n = j;
			}
		}
	}
	
	private void swap(int x, int y) {
		double tmp = priorities[x];
		priorities[x] = priorities[y];
		priorities[y] = tmp;
		
		int tmp2 = values[x];
		values[x] = values[y];
		values[y] = tmp2;
	}
	
	private void expand() {
		values = Arrays.copyOf(values, (int) (growthFactor * values.length));
		priorities = Arrays.copyOf(priorities, (int) (growthFactor * priorities.length));
	}
}
