package black0ut1.util;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.io.CSV;
import black0ut1.io.InputOutput;
import black0ut1.io.TNTP;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;

public class Util {
	
	public static void parallelLoop(ExecutorService executor, int to, IntConsumer task) {
		
		Collection<Callable<Void>> jobs = new Vector<>();
		for (int i = 0; i < to; i++) {
			int finalI = i;
			jobs.add(() -> {
				task.accept(finalI);
				return null;
			});
		}
		
		try {
			executor.invokeAll(jobs);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static double projectToInterval(double x, double min, double max) {
		return Math.max(Math.min(x, max), min);
	}
	
	@SafeVarargs
	public static <T> T[] concat(Class<?> type, T[]... arrays) {
		int length = 0;
		for (T[] array : arrays)
			length += array.length;
		
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(type, length);
		
		int i = 0;
		for (T[] array : arrays) {
			System.arraycopy(array, 0, result, i, array.length);
			i += array.length;
		}
		
		return result;
	}
	
	public static double min(double a, double b, double c) {
		double smallest = a;
		if (smallest > b)
			smallest = b;
		if (smallest > c)
			smallest = c;
		
		return smallest;
	}
	
	public static Pair<Network, DoubleMatrix> loadData(InputOutput io, String networkFile, String odmFile, String nodeFile) {
		System.out.print("Loading OD matrix... ");
		long startTime = System.currentTimeMillis();
		DoubleMatrix odMatrix = io.parseODMatrix(odmFile);
		long endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		System.out.print("Loading network... ");
		startTime = System.currentTimeMillis();
		Network network = io.parseNetwork(networkFile, nodeFile, odMatrix.n);
		endTime = System.currentTimeMillis();
		System.out.println("OK (" + (endTime - startTime) + "ms)");
		
		return new Pair<>(network, odMatrix);
	}
}
