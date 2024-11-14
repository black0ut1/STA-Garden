//package black0ut1.sta.assignment.bush;
//
//import black0ut1.data.*;
//import black0ut1.sta.assignment.Algorithm;
//import black0ut1.util.Util;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//@SuppressWarnings("unchecked")
//public class ParallelB extends B {
//
//	protected final int threads;
//	protected final ExecutorService executor;
//
//	public ParallelB(Algorithm.Parameters parameters, int threads) {
//		super(parameters);
//		this.threads = threads;
//		this.executor = Executors.newFixedThreadPool(threads);
//	}
//
//	@Override
//	protected void init() {
//		Util.parallelLoop(executor, bushes.length, i -> bushes[i] = createBush(i));
//
//		for (Bush bush : bushes)
//			for (Network.Edge edge : network.getEdges())
//				flows[edge.index] += bush.getEdgeFlow(edge.index);
//
//		updateCosts();
//	}
//
//	@Override
//	protected void updateFlows() {
//		// #threads bushes are processed concurrently
//		// for example, when started with 6 threads, 6 bushes are processed at once
//		for (int i = 0; i < bushes.length; i += threads) {
//			int finalI = i;
//
//			Triplet<Network.Edge[], double[], Network.Edge[]>[] trees = new Triplet[threads];
//			int[][] divNodes = new int[threads][];
//
//			Util.parallelLoop(executor, threads, j -> {
//				if (finalI + j >= bushes.length)
//					return;
//
//				Bush bush = bushes[finalI + j];
//
//				improveBush(bush);
//				trees[j] = getTrees(bush);
//
//				divNodes[j] = new int[network.nodes];
//				for (int node = 0; node < network.nodes; node++) {
//					divNodes[j][node] = findDivergenceNode(trees[j].first(),
//							trees[j].third(), node, trees[j].second());
//				}
//			});
//
//			// this is where things must go sequentially
//			for (int j = 0; j < threads; j++) {
//				if (i + j == bushes.length)
//					break;
//				Bush bush = bushes[i + j];
//
//				var minTree = trees[j].first();
//				var maxTree = trees[j].third();
//				for (int node = 0; node < network.nodes; node++) {
//					int divNode = divNodes[j][node];
//					if (divNode == -1)
//						continue;
//
//					double deltaX = findFlowDelta(minTree, maxTree, bush, node, divNode);
//					if (deltaX == 0)
//						continue;
//
//					shiftFlows(minTree, maxTree, bush, node, divNode, deltaX);
//				}
//			}
//
//			Util.parallelLoop(executor, threads, j -> {
//				if (finalI + j >= bushes.length)
//					return;
//
//				Bush bush = bushes[finalI + j];
//				removeUnusedArcs(bush, trees[j].first());
//			});
//
//			updateCosts();
//		}
//	}
//
//	@Override
//	protected void cleanUp() {
//		executor.shutdown();
//		try {
//			if (!executor.awaitTermination(10, TimeUnit.MILLISECONDS))
//				System.out.println("Waiting for thread pool termination timed out");
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//	}
//}
