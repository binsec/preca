package constracq.core.learner;

import io.github.binsec.core.learner.Answer;

public interface ILearner {
	/**
	 * Ask this learner if the tuple represented by 
	 * the query e is a solution or not
	 * 
	 * @param e Example to classify as positive or negative
	 * @return true if the query e is positive
	 */
	public Answer ask(ACQ_Query e);
    
	/**
	 * 
	 * @param findC_example
	 */
	public Answer ask_query(ACQ_Query findC_example);

	public void non_asked_query(ACQ_Query query);
	


}
