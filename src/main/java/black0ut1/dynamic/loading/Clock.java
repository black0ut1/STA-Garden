package black0ut1.dynamic.loading;

/**
 * A class representing time in modelling DNL.
 */
public class Clock {
	
	/** The period of one time step [h]. */
	public final double timeStep;
	/** The number of time steps it takes for the whole period to go by. */
	public final int steps;
	/** The ordinal of current time step, is from interval [0, steps). */
	private int currentStep;
	
	public Clock(double timeStep, int steps) {
		this.timeStep = timeStep;
		this.steps = steps;
		this.currentStep = 0;
	}
	
	public int getCurrentStep() {
		return currentStep;
	}
	
	public void nextStep() {
		currentStep++;
	}
	
	public boolean ticking() {
		return currentStep < steps;
	}
}
