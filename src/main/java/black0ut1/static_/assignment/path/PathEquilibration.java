package black0ut1.static_.assignment.path;

import black0ut1.data.network.Path;
import black0ut1.static_.assignment.Settings;

public class PathEquilibration extends GradientProjection {
	
	public PathEquilibration(Settings settings) {
		super(settings);
	}
	
	@Override
	protected void equilibratePaths(int origin, int destination, Path basicPath) {
		Path maxPath = null;
		double maxPathCost = Double.NEGATIVE_INFINITY;
		for (Path path : odPairs.get(origin, destination)) {
			double pathCost = path.getCost(costs);
			if (pathCost > maxPathCost) {
				maxPath = path;
				maxPathCost = pathCost;
			}
		}
		
		if (maxPath == null || maxPath == basicPath)
			return;
		
		double flowDelta = computeFlowDelta(basicPath, maxPath);
		if (flowDelta == 0)
			return;
		
		shiftFlows(flowDelta, basicPath, maxPath);
	}
}
