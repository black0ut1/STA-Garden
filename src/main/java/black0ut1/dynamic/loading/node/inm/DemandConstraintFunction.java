package black0ut1.dynamic.loading.node.inm;

/**
 * This interface represents a demand constraint function of an incoming link. It takes
 * the inflows and outflows of an intersection and outputs a constraint on the sending
 * flow on that link. It is denoted as Delta-hat(q) in (Flotterod and Rohde, 2011).
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public interface DemandConstraintFunction {
	
	double demand(double[] inflows, double[] outflows);
}
