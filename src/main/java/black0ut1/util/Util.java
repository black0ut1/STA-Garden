package black0ut1.util;

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
}
