package black0ut1.dynamic.equilibrium;

import black0ut1.dynamic.loading.routing.MixtureOutgoingFractions;

public interface StaticRouteChoice {
	
	MixtureOutgoingFractions[] computeInitialMixtureFractions();
}
