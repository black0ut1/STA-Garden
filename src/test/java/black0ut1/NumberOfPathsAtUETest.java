package black0ut1;

import black0ut1.io.TNTP;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.Settings;
import black0ut1.static_.assignment.path.PathBasedAlgorithm;
import black0ut1.static_.assignment.path.ProjectedGradient;
import black0ut1.util.NetworkUtils;
import black0ut1.util.Util;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NumberOfPathsAtUETest {
	
	@Test
	void test() {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, null);
		
		Settings settings = new Settings(pair.first(), pair.second(), 300,
				new Convergence.Builder()
						.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 1e-14));
		settings.PBA_ENABLE_INNER_LOOP = false;
		
		PathBasedAlgorithm alg = new ProjectedGradient(settings);
		alg.assignFlows();
		
		var paths = alg.getPaths();
		var flows = alg.getFlows();
		NetworkUtils.checkPathFlows(pair.first(), pair.second(), paths, flows);
		
		int numODPairs = paths.n * paths.m;
		int numPaths = 0;
		int numOnePathODPairs = 0;
		int numZeroPathODPairs = 0;
		for (int i = 0; i < paths.n; i++)
			for (int j = 0; j < paths.m; j++) {
				if (paths.get(i, j) == null) {
					numZeroPathODPairs++;
					continue;
				}
				
				numPaths += paths.get(i, j).size();
				
				if (paths.get(i, j).size() == 1)
					numOnePathODPairs++;
			}
		
		System.out.println("Number of paths: " + numPaths);
		System.out.println("Number of OD pairs: " + numODPairs);
		System.out.println("Number of OD pairs with no paths: " + numZeroPathODPairs);
		System.out.println("Number of OD pairs with only one path: " + numOnePathODPairs);
		System.out.println("Number of OD pairs with more than one path: " + (numODPairs - numZeroPathODPairs - numOnePathODPairs));
	}
}
