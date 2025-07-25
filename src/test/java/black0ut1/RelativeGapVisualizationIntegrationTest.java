package black0ut1;

import black0ut1.data.DoubleMatrix;
import black0ut1.data.network.Network;
import black0ut1.gui.CriterionChartPanel;
import black0ut1.gui.GUI;
import black0ut1.static_.assignment.Algorithm;
import black0ut1.static_.assignment.Convergence;
import black0ut1.static_.assignment.bush.iTAPAS;
import black0ut1.static_.assignment.link.*;
import black0ut1.static_.assignment.path.*;
import black0ut1.static_.cost.BPR;
import black0ut1.static_.cost.CostFunction;
import black0ut1.util.Util;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class RelativeGapVisualizationIntegrationTest {
	
	static Class<? extends Algorithm>[] algorithms = new Class[]{
			MSA.class, FrankWolfe.class, FukushimaFrankWolfe.class, ConjugateFrankWolfe.class,
			BiconjugateFrankWolfe.class, SimplicialDecomposition.class,
			PathEquilibration.class, GradientProjection.class, ProjectedGradient.class,
			iTAPAS.class,
	};
	
	static CriterionChartPanel panel;
	
	static Network network;
	static DoubleMatrix odm;
	static CostFunction costFunction = new BPR();
	static int maxIterations = 300;
	static Convergence.Builder builder = new Convergence.Builder().addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 1e-14);
	
	@BeforeAll
	static void setUpBeforeAll() {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		
		panel = new CriterionChartPanel(map);
		
		var pair = Util.loadData(networkFile, odmFile, null);
		network = pair.first();
		odm = pair.second();
		
		new GUI(panel);
	}
	
	@AfterAll
	static void tearDownAfterAll() throws InterruptedException {
		Thread.currentThread().join();
	}
	
	@ParameterizedTest
	@MethodSource("provideAlgorithms")
	void runAlgorithm(Class<? extends Algorithm> algorithm) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		Object[] arguments = new Object[]{network, odm, costFunction, maxIterations, builder.setCallback(values -> {
			double relativeGap = values[Convergence.Criterion.RELATIVE_GAP_1.ordinal()];
			panel.addValue(relativeGap, algorithm.getSimpleName());
		})};
		if (PathBasedAlgorithm.class.isAssignableFrom(algorithm)) {
			arguments = Util.concat(Object.class, arguments, new Object[] {PathBasedAlgorithm.ShortestPathStrategy.SSSP});
		}
		
		Object[] finalArguments = arguments;
		Algorithm alg = (Algorithm) Arrays.stream(algorithm.getDeclaredConstructors())
				.filter(constructor -> constructor.getParameterCount() == finalArguments.length)
				.findFirst().get().newInstance(arguments);
		
		alg.assignFlows();
	}
	
	static Stream<Arguments> provideAlgorithms() {
		return Stream.of(algorithms).map(Arguments::of);
	}
}
