package constracq.core.acqsolver;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

import constracq.core.acqconstraint.ACQ_Network;
import constracq.core.learner.ACQ_Query;
import constracq.core.learner.ACQ_Scope;

/**
 * Abstract class that defines the main functions used by the solver
 * 
 *
 */
abstract public class ACQ_ConstraintSolver {
	/**
	 * Used to evaluate the performance of this solver
	 */
	protected boolean timeout=true;
	protected Long limit=(long) 1000;			// one second
	protected Long timeBound=(long) 60000;			// one minute
	protected boolean peeling_process=false;

    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);



	/**
	 * Checks if there is any solution of the specified constraint network
	 * 
	 * @param learned_network Constraint network to check
	 * @return true if there is at least one solution
	 */
	public abstract boolean solve(ACQ_Network learned_network);

	
	/**
	 * returns a query on the specified constraint network
	 * 
	 * @param learned_network Constraint network to solve
	 * @return query if there is at least one solution
	 */
	public abstract ACQ_Query solveQ(ACQ_Network learned_network);
	
	/**
	 * Function used to check if this solver exceed a definite time
	 * 
	 * @return true if the time is exceeded
	 */
	public boolean timeout_reached(){
		return this.timeout;
	}

	public ACQ_Query solveA(ACQ_Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}

	public Long getLimit() {
		return limit;
	}

	public Long getTimeBound() {
		return timeBound;
	}
	public void setLimit(Long limit) {
		this.limit = limit;
	}



	public boolean isPeeling_process() {
		return peeling_process;
	}

	public void setPeeling_process(boolean peeling_process) {
		this.peeling_process = peeling_process;
	}

	public abstract ACQ_Query peeling_process(ACQ_Network network_A, ACQ_Network network_B);


	public abstract void setVars(ACQ_Scope vars);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

	public void fireSolverEvent(String name,Object oldValue,Object newValue){
		pcs.firePropertyChange(name, oldValue, newValue);
	}


	public abstract ACQ_Query max_AnotB(ACQ_Network network1, ACQ_Network network2, ACQ_Heuristic heuristic);


	protected abstract ArrayList<ACQ_Query>  allSolutions(ACQ_Network learned_network);

	protected abstract void  setTimeoutReached(boolean timeoutReached);
	
	public abstract boolean  isTimeoutReached();


	public abstract ACQ_Network get2remove();


	public abstract void reset2remove();


	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	public abstract ACQ_IDomain getDomain();

	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	public boolean equiv(ACQ_Network net1, ACQ_Network net2, ACQ_Network known) {
		// TODO Auto-generated method stub
		return false;
	}



}
