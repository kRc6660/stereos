package uk.ac.abdn.csd.stereos.agents.evaluators;

import uk.ac.abdn.csd.stereos.agents.Agent;

/**
 * This interface describes the common methods that must be implemented by
 * performance evaluator classes. These classes are responsible for taking a
 * real valued observed outcome and translating this into binary satisfaction or
 * dissappointment.
 * 
 * @author cburnett
 * 
 */
public interface PerformanceEvaluator
{

	public static final double OUTCOME_SUCCESS = 1;
	public static final double OUTCOME_FAILURE = -1;

	/**
	 * Evaluate a given observation and return an evaluation of satisfaction or
	 * disappointment.
	 * 
	 * @param trustee
	 *            the trustee agent who produced the observation
	 * @param observation
	 * @return a positive or negative double value indicating the degree of
	 *         satisfaction or disappointment.
	 */
	public double evaluate(Agent trustee, double observation);

}
