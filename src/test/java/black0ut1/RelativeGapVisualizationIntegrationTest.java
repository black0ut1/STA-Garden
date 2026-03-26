package black0ut1;

import black0ut1.gui.CriterionChartPanel;
import black0ut1.gui.GUI;
import black0ut1.io.TNTP;
import black0ut1.static_.assignment.*;
import black0ut1.static_.assignment.bush.*;
import black0ut1.static_.assignment.link.*;
import black0ut1.static_.assignment.path.*;
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
			ImprovedSocialPressure.class, Greedy.class,
			iTAPAS.class,
	};
	
	static CriterionChartPanel panel;
	static Settings settings;
	
	@BeforeAll
	static void setUpBeforeAll() {
		String map = "ChicagoSketch";
		String networkFile = "data/" + map + "/" + map + "_net.tntp";
		String odmFile = "data/" + map + "/" + map + "_trips.tntp";
		var pair = Util.loadData(new TNTP(), networkFile, odmFile, null);
		
		panel = new CriterionChartPanel(map);
		settings = new Settings(pair.first(), pair.second(), 300,
				new Convergence.Builder()
						.addCriterion(Convergence.Criterion.RELATIVE_GAP_1, 1e-14));
		settings.PBA_ENABLE_INNER_LOOP = false;
		new GUI(panel);
	}
	
	@AfterAll
	static void tearDownAfterAll() throws InterruptedException {
		Thread.currentThread().join();
	}
	
	@ParameterizedTest
	@MethodSource("provideAlgorithms")
	void runAlgorithm(Class<? extends Algorithm> algorithm) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		settings.convergenceBuilder
				.setCallback(values -> {
					double relativeGap = values[Convergence.Criterion.RELATIVE_GAP_1.ordinal()];
					panel.addValue(relativeGap + 1e-14, algorithm.getSimpleName());
				});
		
		Algorithm alg = (Algorithm) Arrays.stream(algorithm.getDeclaredConstructors())
				.filter(constructor -> constructor.getParameterCount() == 1)
				.findFirst().get().newInstance(new Object[]{settings});
		
		alg.assignFlows();
	}
	
	static Stream<Arguments> provideAlgorithms() {
		return Stream.of(algorithms).map(Arguments::of);
	}
}
