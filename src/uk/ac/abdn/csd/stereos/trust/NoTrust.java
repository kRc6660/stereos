package uk.ac.abdn.csd.stereos.trust;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;

/**
 * A trust model for testing/evaluation purposes that does not actually consider
 * trust. It just assigns all agents the non informative prior
 * 
 * @author Chris Burnett
 * 
 */
public class NoTrust extends TrustModel
{

	public NoTrust()
	{
		super();
	}

	@Override
	public double query(Agent a)
	{
		// since this is the 'dumb' model, there are no stored experiences
		return -1;
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{
		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Agent a : agents)
			results.put(a, 0.5);
		return results;
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		return evaluate(agents, recommenders, time);
	}

	@Override
	public Map<String, Map<Agent, Double>> conditionallyEvaluate(List<Agent> candidates,
			Map<Agent, List<Agent>> filteredRecommenders, int timeStep)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void forget()
	{
		// TODO Auto-generated method stub
		
	}

}
