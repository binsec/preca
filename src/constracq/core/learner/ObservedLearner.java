package constracq.core.learner;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import io.github.binsec.core.learner.Answer;

/**
 * This class is a wrapper to observe a given learner with a PropertyChangeListener
 * @author agutierr
 */
public class ObservedLearner extends ACQ_Learner {
	private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	protected ILearner learner;

	public ObservedLearner(ILearner learner) {
		this.learner = learner;
	}
	@Override
	public Answer ask(ACQ_Query e) {
		Answer ret = learner.ask(e);
		pcs.firePropertyChange("ASK", ret, e);
		return ret;
	}
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	public Answer ask(ACQ_Query e, boolean fromMSS) {
		Answer ret = learner.ask(e);
		pcs.firePropertyChange(fromMSS ? "ASK_MSS" : "ASK", ret, e);
		return ret;
	}
	
	@Override
	public Answer ask_query(ACQ_Query query) {
		if (learner instanceof ACQ_Learner) {
			pcs.firePropertyChange("ASK", null, query);
			return learner.ask_query(query);
			
		}
		return Answer.NO;
		}
	@Override
		public void non_asked_query(ACQ_Query query) {
			if (learner instanceof ACQ_Learner) {
				learner.non_asked_query(query);
				pcs.firePropertyChange("NON_ASKED_QUERY", null, query);

			}
		

	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

}
