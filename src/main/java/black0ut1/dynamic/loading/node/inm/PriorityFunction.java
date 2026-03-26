package black0ut1.dynamic.loading.node.inm;

/**
 * This interface represents a priority function of an incoming link. The priority
 * function takes inflows and outflows of an intersection and produces a priority for the
 * incoming link it is associated with. It is denoted as phi^{in} in
 * (Flotterod and Rohde, 2011).
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public interface PriorityFunction {
	
	double priority(double[] inflows, double[] outflows);
}
