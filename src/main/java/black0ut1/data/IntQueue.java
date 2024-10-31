package black0ut1.data;

public class IntQueue {
	
	private final int[] data;
	private int front;
	private int rear;
	private int size;
	
	public IntQueue(int capacity) {
		this.data = new int[capacity];
		this.rear = capacity - 1;
		this.front = 0;
		this.size = 0;
	}
	
	public void enqueue(int item) {
		rear = (rear + 1) % data.length;
		data[rear] = item;
		size++;
	}
	
	public int dequeue() {
		int item = data[front];
		front = (front + 1) % data.length;
		size--;
		
		return item;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
}
