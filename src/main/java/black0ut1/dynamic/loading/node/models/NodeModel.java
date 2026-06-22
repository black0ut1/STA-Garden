package black0ut1.dynamic.loading.node.models;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.node.routing.RoutedIntersection;

/**
 * Represents a node model, i.e. an algorithm which shifts the total flows at an
 * intersection. Only {@link RoutedIntersection} needs a
 * node model.
 */
public interface NodeModel {
	
	/**
	 * Method which executes the node model. Needs total turning fractions of the
	 * intersection at which the model is applied, sending flows of incoming links and
	 * receiving flows of outgoing links. If the node model needs other information, pass
	 * it through the constructor.
	 * @param totalTurningFractions Matrix of doubles, where {@code totalTurningFractions.get(i, j)}
	 * is the fraction of flow entering the intersection from incoming link i that exits
	 * using outgoing link j.
	 * @param sendingFlows Array of doubles, where {@code sendingFlows[i]} is the
	 * sending flow of i-th incoming link.
	 * @param receivingFlows Array of doubles, where {@code receivingFlows[j]} is the
	 * receiving flow of j-th outgoing link.
	 * @return Pair of total inflows and outflows. {@code pair.first()[i]} is the total
	 * inflow of i-th incoming link and {@code pair.second()[j]} is the total outflow of
	 * j-th outgoing link.
	 */
	Pair<double[], double[]> computeTotalInflowsOutflows(
			DoubleMatrix totalTurningFractions, double[] sendingFlows, double[] receivingFlows);
}
