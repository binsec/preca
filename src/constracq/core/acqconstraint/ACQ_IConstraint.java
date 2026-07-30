package constracq.core.acqconstraint;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import constracq.core.learner.ACQ_Query;
import constracq.core.learner.ACQ_Scope;

/**
 * Interface that lists the functions to be implemented by each constraint class
 * 
 *
 */
public interface ACQ_IConstraint extends Comparable<ACQ_IConstraint> {
    /**
     * Returns the name of this constraint
     * 
     * @return name of this constraint
     */
    public String getName();
    
    /**
     * Returns the scope of this constraint
     * 
     * @return scope of this constraint
     */
    public ACQ_Scope getScope();
    
    /**
     * Returns the number of variables of this constraint (size of the scope)
     * 
     * @return size of the scope
     */
    public int getArity();
    
    /**
     * Get the set of numeric values of the example query 
     * 
     * @param query Positive or negative example
     * @return set of numeric values
     */
    public int[] getProjection(ACQ_Query query);
    
    /**
     * Checks if the constraint is violated for a given set of values
     * 
     * @param values set of values to check
     * @return false if the set of values violate this constraint
     */
    public boolean checker(int[] values);
    
    /**
     * <p>Returns the negation of this constraint. For instance, a constraint with "=" 
     * as operator will return a constraint with the same scope but "!=" as operator</p>
     * 
     * @return Negation of this constraint
     */
    public ACQ_IConstraint getNegation();
    
    /**
     * get equivalent constraints for the choco solver
     * 
     * @param model model of the solver
     * @param intVars scope of the constraint
     * @return a set of constraints
     */
    public Constraint[] getChocoConstraints(Model model, IntVar... intVars);
	    
    
  
	    
    /**
     * Add this constraint to the model and reify it with a boolean variable
     * 
     * @param model model of the solver
     * @param b a BoolVar to reify the constraint
     * @param intVars scope of the constraint
     */
    public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars);

    
    public IntVar[] getVariables(IntVar[] fullVarSet);

	public int[] getVariables();

	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public abstract boolean check(ACQ_Query query);
	
	void setName(String name);

	public String getNegName();
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public String toSmtlib();
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public int getWeight();
}
