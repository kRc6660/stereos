package uk.ac.abdn.csd.stereos.reputation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;

/**
 * The default reputation filter just returns all the recommenders given to it.
 * 
 * @author cburnett
 * 
 */
public class DefaultFilter implements ReputationFilter
{

	public Map<Agent, List<Agent>> filterRecommenders(Agent self, List<Agent> candidates, List<Agent> recommenders)
	{

		Map<Agent, List<Agent>> result = new HashMap<Agent, List<Agent>>();

		for (Agent a : candidates)
			result.put(a, recommenders);

		return result;
	}

	public void visualise()
	{
		// TODO Auto-generated method stub

	}

}
