package uk.ac.abdn.csd.stereos.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * A reduced model meta learner that uses the RoFE method.
 * 
 * Instead of calculating 2^N models (for each combination of missing features)
 * we only compute models for single missing features, and when more than one
 * feature is missing, we take a weighted average of their predictions based
 * (weighted model accuracy)
 * 
 * @author cburnett
 * 
 */
public class ReFELearner implements Learner
{

	/**
	 * This model is our base model - the one containing the features currently
	 * considered salient. Once we have constructed the base model, more general
	 * models will be constructed on-line to deal with features not present in
	 * the base model.
	 */
	private Learner baseModel;

	/**
	 * Base signature of the base model
	 */
	private Set<String> baseSignature;

	/**
	 * This structure maps MF (missing feature) signiatures to their appropriate
	 * MF models.
	 */
	private Map<Set<String>, Learner> MFmodels;

	/**
	 * A list of models which should be updated
	 */
	// private List<Learner> modelsToUpdate;

	/**
	 * Cache of opinions for lazy training of MF models
	 */
	// private Map<Agent,Opinion> opinionCache; - note, don't need it for RoFE
	// because we're training all
	// models simulataneously

	public ReFELearner()
	{
		baseModel = new M5PLearner();
		MFmodels = new HashMap<Set<String>, Learner>();
		// modelsToUpdate = new ArrayList<Learner>();
	}

	/**
	 * This function should behave just like the normal classification function
	 * with one major exception - when an agent to be classified has a feature
	 * missing which is present in the base model, then a 'missing feature'
	 * model should be used. If an appropriate MF model cannot be found, then
	 * one should be created from the experience base. This should be done by
	 * training the new model with examples where the missing feature(s) in the
	 * classification instance are hidden in the examples.
	 * 
	 * So there's two steps to this - matching 'signatures' of missing/present
	 * features to appropriate models, and transforming training experiences
	 * into a form compatible with those models.
	 */
	public Map<Agent, Double> getBaseRates(List<Agent> agents)
	{

		Map<Agent, Double> biases = new HashMap<Agent, Double>();

		// first thing to do is convert the instances
		Instances data = LearningUtils.agentsToMP5Instances(agents);
		// loop through instances and classify
		// we need to make sure instances only go to the right models so that
		// weka
		// doesn't see any missing features
		for (int i = 0; i < data.numInstances(); i++) {
			Set<String> instanceSignature = new HashSet<String>();
			// we can check one by one of the attributes are missing
			for (String f : baseSignature) {
				// check to see if the current salient (base) feature is present
				if (data.instance(i).isMissing(data.attribute(f)))
					// we record that f is not present - empty set means no
					// missing salients
					instanceSignature.add(f);
			}

			// try to classify
			try {
				if (baseModel.isReady()) {
					Set<String> missingSalients = new HashSet<String>(instanceSignature);
					missingSalients.retainAll(baseSignature);

					// if we have a complete set of salient features, and a
					// ready model
					if (missingSalients.size() == 0) {
						biases.put(agents.get(i), baseModel.getClassifier().classifyInstance(data.instance(i)));
					} else {
						// we need to format the incoming data (classification)
						// set to match the model
						// for each missing feature, get a prediction (linear
						// time) then do the 'voting'

						// average predictions for vote/bagging
						double predictionSum = 0, predictionWeights = 0;

						for (String f : instanceSignature) {
							// create test single feature
							Set<String> fs = new HashSet<String>();
							fs.add(f);

							if (MFmodels.containsKey(fs)) {
								Learner m = MFmodels.get(fs);

								if (m.isReady()) {
									// biases.put(agents.get(i),
									// m.getClassifier().classifyInstance(data.instance(i)));
									List<Agent> target = new ArrayList<Agent>();
									target.add(agents.get(i));

									Map<Agent, Double> preds = m.getBaseRates(target);
									// biases.putAll(preds);

									// add to weighted mean
									double weight = m.getErrorRate();
									predictionWeights += weight;
									predictionSum += preds.get(agents.get(i)) * weight;
								}
								// else
								// biases.put(agents.get(i), 0.5); // assign
								// default bias for now

							}
							// otherwise create it but wait until the learning
							// interval is called
							else {
								MFmodels.put(fs, new M5PLearner());
								// biases.put(agents.get(i), 0.5); // assign
								// default bias for now
							}
						}

						// add the weighted mean from the single missing feature
						// models
						double wm = predictionSum / predictionWeights;
						biases.put(agents.get(i), wm);

					}
				}
			} catch (Exception e) {
				System.err.println("MP5Learner: Error while classifying new instances:");
				e.printStackTrace();
			}
		}
		return biases;
	}

	/**
	 * Should be properly extended - how do we calculate RMSE of multiple
	 * models? The base model is trained against all examples so it's not a good
	 * measure...
	 */
	public double getErrorRate()
	{

		// try the average error of the models
		double sum = 0, count = 0;

		for (Learner l : MFmodels.values()) {
			if (l.isReady()) {
				sum += l.getErrorRate();
				count++;
			}
		}

		sum += baseModel.getErrorRate();
		count++;

		double averageError = sum / count;

		return averageError;
	}

	public boolean isReady()
	{
		return baseModel.isReady();
	}

	/**
	 * Train a specific models based on current contents of experience base. We
	 * deal with MF models online, so they only get trained as necessary.
	 */
	public void train(Map<Agent, Opinion> opinions, Set<String> signature)
	{
		Set<String> nonDiagnostics = new HashSet<String>();
		Agent eg = LearningUtils.mostFeaturefulAgent(new ArrayList<Agent>(opinions.keySet()));
		// get rid of all features not indicated by base model as being salient
		// (mark as hidden) - hide all non-diagnostic features in sub models
		for (String f : eg.getFeatures().keySet())
			if (!baseSignature.contains(f))
				nonDiagnostics.add(f);

		// if this is the base signature just go ahead
		Set<String> missingSalients = new HashSet<String>(signature);
		missingSalients.retainAll(baseSignature);
		if (missingSalients.size() == 0) {
			train(opinions);
		} else {
			// given an MF signature get the correct model and train
			// use only singluarly reduced models here
			for (String f : signature) {
				nonDiagnostics.add(f);
				Set<String> fs = new HashSet<String>();
				fs.add(f);
				MFmodels.get(fs).train(opinions, nonDiagnostics);
			}
		}
	}

	/**
	 * Return the model signature for a particular model.
	 * 
	 * @param m
	 * @return
	 */
	public Set<String> getModelSignature(Learner m)
	{
		return m.getModelSignature();
	}

	/**
	 * Return the signature of the base model
	 */
	public Set<String> getModelSignature()
	{
		return baseModel.getModelSignature();
	}

	/**
	 * Train all models.
	 */
	public void train(Map<Agent, Opinion> opinions)
	{
		baseModel.train(opinions);
		baseSignature = getModelSignature();

		// drop all reduced models
		MFmodels = new HashMap<Set<String>, Learner>();

		// train a model for each missing feature
		for (String f : baseSignature) {
			Set<String> sig = new HashSet<String>();
			sig.add(f);
			Learner newLearner = new M5PLearner();
			MFmodels.put(sig, newLearner);
			train(opinions, sig);
		}
	}

	/**
	 * Simply return the base classifier
	 */
	public Classifier getClassifier()
	{
		return baseModel.getClassifier();
	}

}
