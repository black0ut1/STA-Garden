package black0ut1.static_.assignment.bush;

import black0ut1.data.network.Bush;
import black0ut1.data.network.Network;
import black0ut1.data.network.PAS;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.Convergence;
import black0ut1.util.SSSP;
import black0ut1.util.Util;

import java.util.concurrent.*;

public class ParalleliTAPAS extends iTAPAS {
	
	protected final int threads;
	protected final ExecutorService threadPool;
	
	public ParalleliTAPAS(Settings settings, int threads) {
		super(settings);
		this.threads = threads;
		this.threadPool = Executors.newFixedThreadPool(threads);
	}
	
	
	@Override
	protected void initialize() {
		Util.parallelLoop(threadPool, network.zones, origin -> {
			bushes[origin] = createBush(origin);
		});
		
		for (Bush bush : bushes)
			for (Network.Edge edge : network.getEdges())
				flows[edge.index] += bush.getEdgeFlow(edge.index);
		
		updateCosts();
	}
	
	@Override
	protected void mainLoopIteration() {
		double minReducedCost = switch (iteration) {
			case 0:
				yield 0.1;
			case 1:
				yield 0.001;
			default:
				double convIndicator = convergence.getData().getLast()[Convergence.Criterion.RELATIVE_GAP_1.ordinal()];
				yield convIndicator / 100;
		};
		
		for (int i = 0; i < network.zones; i += threads) {
			
			Network.Edge[][] minTrees = new Network.Edge[threads][];
			double[][] minDistances = new double[threads][];
			Network.Edge[][] potentialLinks = new Network.Edge[threads][];
			
			int finalI = i;
			Util.parallelLoop(threadPool, threads, j -> {
				int origin = finalI + j;
				if (origin >= network.zones)
					return;
				
				var pair = SSSP.dijkstra(network, origin, costs);
				minTrees[j] = pair.first();
				minDistances[j] = pair.second();
				
				potentialLinks[j] = findPotentialLinks(minTrees[j], origin);
			});
			
			for (int j = 0; j < threads; j++) {
				int origin = finalI + j;
				if (origin >= network.zones)
					return;
				
				Network.Edge[] minTree = minTrees[j];
				double[] minDistance = minDistances[j];
				
				for (Network.Edge edge : potentialLinks[j]) {
					if (edge == null)
						break;
					
					if (bushes[origin].getEdgeFlow(edge.index) <= FLOW_EPSILON)
						continue;
					
					double reducedCost = minDistance[edge.tail] + costs[edge.index] - minDistance[edge.head];
					if (reducedCost < minReducedCost)
						continue;
					
					PAS found = matchPAS(edge, reducedCost);
					if (found != null) {
						shiftFlows(found);
						
						if (found.maxSegmentFlowBound(bushes) > FLOW_EPSILON
								&& found.minSegmentFlowBound(bushes) > FLOW_EPSILON)
							continue;
					}
					
					PAS newPas = MFS(edge, minTree, bushes[origin], null);
					if (newPas != null)
						manager.addPAS(newPas);
				}
				
				randomShifts();
			}
		}
		
		eliminatePASes();
	}
	
	@Override
	protected void postProcess() {
		super.postProcess();
		threadPool.shutdown();
	}
}
