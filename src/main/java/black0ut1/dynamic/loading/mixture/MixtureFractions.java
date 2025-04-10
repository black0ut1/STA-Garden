package black0ut1.dynamic.loading.mixture;

/**
 * Interface for defining, how MixtureFlow turns at an intersection.
 * It decomposes turning fractions of some intersection by
 * destinations. Turning fraction tf[i][j] is a number from interval
 * [0, 1] that expresses portion of flow entering intersection from
 * incoming link i, that exits using outgoing link j.
 */
public interface MixtureFractions {
	
	double[][] getDestinationFractions(int destination);
	
	void forEach(MixtureFractions.Consumer consumer);
	
	void checkPartialFractions();
	
	@FunctionalInterface
	interface Consumer {
		void accept(int destination, double[][] destinationFractions);
	}
}
