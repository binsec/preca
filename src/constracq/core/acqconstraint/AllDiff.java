package constracq.core.acqconstraint;

import java.util.Set;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import constracq.core.learner.ACQ_Query;

/**
 *
 * @author NADJIB
 */

public  class AllDiff extends ACQ_Constraint{

	public AllDiff(Set<Integer> clique) {

		super("AllDiff",clique);
	}
	@Override
	public boolean check(int... value) {
		return check(value[0],value[1]);
	}
	protected  boolean check(int value1,int value2) {
		//TODO
		return false;
	}
	@Override
	public ACQ_IConstraint getNegation() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		
		int[] alldiff_vars = this.getVariables();
		IntVar[] vars=new IntVar[alldiff_vars.length];
	
		
		for(int i=0; i<alldiff_vars.length; i++) {
				vars[i]=intVars[alldiff_vars[i]];
			
			 }
		return new Constraint[]{model.allDifferent(vars)};

	}
	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {

		model.allDifferent(intVars).reifyWith(b);

	}
	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	@Override
	public String toSmtlib() {
		// TODO Auto-generated method stub
		return null;
	}

}

