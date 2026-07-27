package black0ut1.dynamic.equilibrium;

import black0ut1.Main;
import black0ut1.dynamic.Convergence;
import black0ut1.dynamic.DynamicNetwork;
import black0ut1.dynamic.TimeDependentODM;
import black0ut1.dynamic.loading.dnl.DynamicNetworkLoading;
import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;
import black0ut1.dynamic.tdsp.DOT;
import black0ut1.util.DynamicUtils;
import black0ut1.util.Util;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Method of Successive Averages algorithm.
 */
public class MSA extends MOF_DUE {
	
	public MSA(DynamicNetwork network, TimeDependentODM odm, StaticRouteChoice initialRouteChoice,
			   DynamicNetworkLoading dnl, DOT tdsp, int maxIterations, double stepSize, Convergence convergence) {
		super(network, odm, initialRouteChoice, dnl, tdsp, maxIterations, stepSize, convergence);
	}
	
	public void run() {
		var mfs = initialRouteChoice.computeInitialMixtureFractions();
		dnl.setTurningFractions(mfs);
		dnl.loadNetwork();
//		dnl.checkDestinationInflows(false);
		
		double[][] travelTimes = new double[network.links.length][];
		for (int i = 0; i < network.links.length; i++)
			travelTimes[i] = DynamicUtils.computeTravelTime(network.links[i], stepSize);
		
		var pair = tdsp.shortestPaths(mfs, travelTimes);
		MixtureOutgoingFractions.Costs costs = pair.first();
		MixtureOutgoingFractions.Indices shortestOugoingLinks = pair.second();
		
		double[] criterions = convergence.computeAll(costs);
		System.out.println("[DUE] TSTT: " + criterions[0]);
		System.out.println("[DUE] SPTT: " + criterions[1]);
		System.out.println("[DUE] AEC:  " + criterions[2]);
		System.out.println("[DUE] RG:   " + criterions[3]);

		
		for (int i = 0; i < maxIterations; i++) {
			System.out.println("[DUE] Iteration: " + i);
			
			double lambda = 1.0 / (i + 2);
			
			for (int n = 0; n < mfs.intersections; n++) {
				MixtureOutgoingFractions.Intersection mof = mfs.get(n);
				mof.start();
				
				for (int t = 0; t < mfs.timeSteps; t++)
					for (int d = 0; d < network.zones.length; d++)
						for (int j = 0; j < network.routedIntersections[n].outgoingLinks.length; j++) {
							double fraction = mof.getFraction(t, d, j);
							
							double newFraction = (shortestOugoingLinks.getIndex(n, t, d) == j)
									? (1 - lambda) * fraction + lambda
									: (1 - lambda) * fraction;
							
							mof.setFraction(t, d, j, newFraction);
						}
				mof.compress();
			}
			
			dnl.loadNetwork();
			
			for (int j = 0; j < network.links.length; j++)
				travelTimes[j] = DynamicUtils.computeTravelTime(network.links[j], stepSize);
			
			pair = tdsp.shortestPaths(mfs, travelTimes);
			costs = pair.first();
			shortestOugoingLinks = pair.second();
			
			criterions = convergence.computeAll(costs);
			System.out.println("[DUE] TSTT: " + criterions[0]);
			System.out.println("[DUE] SPTT: " + criterions[1]);
			System.out.println("[DUE] AEC:  " + criterions[2]);
			System.out.println("[DUE] RG:   " + criterions[3]);
		}
		
		Util.dumpHeap(Main.network);
		System.out.println();
		
		System.out.println("\n===== General Statistics =====");
		System.out.println("Grid dimensions: " + mfs.timeSteps + "x" +
				mfs.destinations + " = " + mfs.timeSteps * mfs.destinations);
		
		double avgPoolSize = 0;
		double avgPoolLength = 0;
		int maxPoolSize = 0;
		for (MixtureOutgoingFractions.Intersection mofIntersection : mfs.mofIntersections) {
			int poolSize = mofIntersection.uniqueVectors.length / mofIntersection.J;
			maxPoolSize = Math.max(maxPoolSize, poolSize);
			avgPoolSize += poolSize;
			avgPoolLength += mofIntersection.uniqueVectors.length;
		}
		avgPoolSize /= mfs.intersections;
		avgPoolLength /= mfs.intersections;
		System.out.printf("Average pool size: %.2f\n", avgPoolSize);
		System.out.println("Max pool size: " + maxPoolSize);
		System.out.printf("Average pool length: %.2f\n", avgPoolLength);
		
		double avgSingletons = 0;
		double avgPointersToPool = 0;
		for (MixtureOutgoingFractions.Intersection mofIntersection : mfs.mofIntersections)
			for (int i : mofIntersection.grid) {
				if (i >= 0)
					avgSingletons++;
				else
					avgPointersToPool++;
			}
		avgSingletons /= mfs.intersections;
		avgPointersToPool /= mfs.intersections;
		System.out.printf("Average number of singletons: %.2f\n", avgSingletons);
		System.out.printf("Average number of pointers to pool: %.2f\n", avgPointersToPool);
		
		
		System.out.println("\n===== Pool Patterns =====");
		double epsilon = 1e-3;
		double avgReducedPoolSize = 0;
		for (MixtureOutgoingFractions.Intersection mofIntersection : mfs.mofIntersections) {
			int poolSize = mofIntersection.uniqueVectors.length / mofIntersection.J;
			
			TreeMap<double[], Integer> pseudoDuplicates = new TreeMap<>((o1, o2) -> {
				double distance = 0;
				for (int i = 0; i < mofIntersection.J; i++)
					distance += (o1[i] - o2[i]) * (o1[i] - o2[i]);
				distance = Math.sqrt(distance);
				
				if (distance < epsilon)
					return 0;
				
				for (int i = 0; i < mofIntersection.J; i++) {
					if (o1[i] < o2[i])
						return -1;
					if (o1[i] > o2[i])
						return 1;
				}
				
				throw new RuntimeException("This should not happen");
			});
			
			for (int i = 0; i < poolSize; i++) {
				double[] vector = Arrays.copyOfRange(mofIntersection.uniqueVectors, i * mofIntersection.J, (i + 1) * mofIntersection.J);
				int num = pseudoDuplicates.getOrDefault(vector, 0);
				pseudoDuplicates.put(vector, num + 1);
			}
			
			avgReducedPoolSize += pseudoDuplicates.size();
		}
		avgReducedPoolSize /= mfs.intersections;
		System.out.printf("Average reduced pool size (epsilon=%.0e): %.2f\n", epsilon, avgReducedPoolSize);
		
		
		System.out.println("\n===== Grid Patterns =====");
		// Average length of time periods of constant values
		int periodLengthSum = 0;
		int periodsCount = 0;
		for (MixtureOutgoingFractions.Intersection intersection : mfs.mofIntersections) {
			int[] grid = intersection.grid;
			
			for (int d = 0; d < mfs.destinations; d++) {
				
				int periodValue = -1;
				int periodLen = 0;
				for (int t = 0; t < mfs.timeSteps; t++) {
					int currPeriodValue = grid[t * mfs.destinations + d];
					
					if (currPeriodValue != periodValue) { // new period has begun
						if (periodValue != -1)
							periodsCount++;
						periodValue = currPeriodValue;
						periodLengthSum += periodLen;
						periodLen = 0;
					}
					
					periodLen++;
				}
				
				// last period
				periodsCount++;
				periodLengthSum += periodLen;
			}
		}
		System.out.printf("Average length of time periods for single destinations: %.2f\n",
				(double) periodLengthSum / periodsCount);
		
		int dperiodLengthSum = 0;
		int dperiodsCount = 0;
		for (MixtureOutgoingFractions.Intersection intersection : mfs.mofIntersections) {
			int[] grid = intersection.grid;
			
			for (int t = 0; t < mfs.timeSteps; t++) {
				
				int periodValue = -1;
				int periodLen = 0;
				for (int d = 0; d < mfs.destinations; d++) {
					int currPeriodValue = grid[t * mfs.destinations + d];
					
					if (currPeriodValue != periodValue) { // new period has begun
						if (periodValue != -1)
							dperiodsCount++;
						periodValue = currPeriodValue;
						dperiodLengthSum += periodLen;
						periodLen = 0;
					}
					
					periodLen++;
				}
				
				// last period
				dperiodsCount++;
				dperiodLengthSum += periodLen;
			}
		}
		System.out.printf("Average length of destination periods: %.2f\n", (double) dperiodLengthSum / dperiodsCount);
		
		periodLengthSum = 0;
		periodsCount = 0;
		for (MixtureOutgoingFractions.Intersection intersection : mfs.mofIntersections) {
			int[] grid = intersection.grid;
			
			
			int periodLen = 0;
			for (int t = 1; t < mfs.timeSteps; t++) {
				
				for (int d = 0; d < mfs.destinations; d++) {
					int currPeriodValue = grid[t * mfs.destinations + d];
					int prevPeriodValue = grid[(t - 1) * mfs.destinations + d];
					
					if (currPeriodValue != prevPeriodValue) {
						periodsCount++;
						periodLengthSum += periodLen;
						periodLen = 0;
						break;
					}
				}
				
				periodLen++;
			}
			
			// last period
			periodsCount++;
			periodLengthSum += periodLen;
			
		}
		System.out.printf("Average length of time periods for single destinations: %.2f\n",
				(double) periodLengthSum / periodsCount);
	}
}
