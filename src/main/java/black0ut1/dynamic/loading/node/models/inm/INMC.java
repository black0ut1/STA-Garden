package black0ut1.dynamic.loading.node.models.inm;

import black0ut1.dynamic.loading.node.models.NodeModel;

/**
 * This class represents the Incremental Node Model with node supply Constrains (INMC). It
 * extends an INM in functionality. This class essentially represents a solver for problem
 * (27) in (Flotterod and Rohde, 2011), where the INM solver is passed in the constructor.
 * <p>
 * Bibliography:																		  <br>
 * - (Flotterod and Rohde, 2011) Operational macroscopic modeling of complex urban road
 * intersections
 */
public abstract class INMC implements NodeModel {
	
	/** The backing INM solver whose {@link INM#computeTotalInflowsOutflows} method is used. */
	protected final INM inm;
	
	/** See {@link DemandConstraintFunction}. */
	protected final DemandConstraintFunction demandConstraints;
	
	public INMC(INM inm, DemandConstraintFunction demandConstraints) {
		this.inm = inm;
		this.demandConstraints = demandConstraints;
	}
}
