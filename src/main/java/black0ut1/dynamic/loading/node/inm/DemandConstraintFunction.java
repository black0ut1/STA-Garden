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
	
	/**
	 * Returns the constraints (ceilings) on each sending flow based in current flows.
	 * @param inflows Current inflows computed by the INMC model.
	 * @param outflows Current outflows computed by the INMC model.
	 * @return An array with length same as {@code inflows}.
	 */
	double[] demand(double[] inflows, double[] outflows);
}
