package constracq.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.learner.ACQ_Query;


public class ACQ_DisjunctionConstraint extends ACQ_MetaConstraint{

	int weight = 1;

	public ACQ_DisjunctionConstraint(ConstraintFactory constraintFactory,ACQ_IConstraint c1, ACQ_IConstraint c2) {

		super(constraintFactory,"disjunction", c1, c2);

	}

	public ACQ_DisjunctionConstraint(ConstraintFactory constraintFactory, ConstraintSet set) {

		super(constraintFactory, "disjunction", set);

	}
	
	public ACQ_DisjunctionConstraint(ConstraintFactory constraintFactory,ACQ_IConstraint c1, ACQ_IConstraint c2, int weight) {

		this(constraintFactory, c1, c2);
		this.weight = weight;

	}
	
	public ACQ_DisjunctionConstraint(ConstraintFactory constraintFactory, ConstraintSet set, int weight) {

		this(constraintFactory, set);
		this.weight = weight;

	}
	
	@Override
	public ACQ_IConstraint getNegation() {
		if (this.constraintSet.size() == 1) {
			assert false : "Disjunction constraint cannot contain one constraint only";
			return null;
		}
		if (this.constraintSet.size() == 2) {
			ACQ_IConstraint c0 = this.constraintSet.get_Constraint(0).getNegation();
			ACQ_IConstraint c1 = this.constraintSet.get_Constraint(1).getNegation();
			return new ACQ_ConjunctionConstraint(this.constraintFactory, c0, c1);
		}
		else {
			ConstraintSet set = constraintFactory.createSet();
			for (int i = 0; i < this.constraintSet.size(); i++) {
				set.add(this.constraintSet.get_Constraint(i).getNegation());
			} 
			return new ACQ_ConjunctionConstraint(constraintFactory, set);
		}
	}
	

	public void remove(ACQ_IConstraint c) {
		this.remove(c);
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public ACQ_IConstraint getNegation(int weight) {
		if (this.constraintSet.size() == 1) {
			assert false : "Disjunction constraint cannot contain one constraint only";
			return null;
		}
		if (this.constraintSet.size() == 2) {
			ACQ_IConstraint c0 = this.constraintSet.get_Constraint(0).getNegation();
			ACQ_IConstraint c1 = this.constraintSet.get_Constraint(1).getNegation();
			return new ACQ_ConjunctionConstraint(this.constraintFactory, c0, c1, weight);
		}
		else {
			ConstraintSet set = constraintFactory.createSet();
			for (int i = 0; i < this.constraintSet.size(); i++) {
				set.add(this.constraintSet.get_Constraint(i).getNegation());
			} 
			return new ACQ_ConjunctionConstraint(constraintFactory, set, weight);
		}
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {

		BoolVar[] reifyArray = model.boolVarArray(constraintSet.size());

		int i=0;
		for(ACQ_IConstraint c: constraintSet)
		{
			c.toReifiedChoco(model, reifyArray[i], intVars);
			i++;
		}

		return new Constraint[]{model.sum(reifyArray, ">", 0)};

	}

	@Override
	/****
	 * b <=> C1 and C2 :
	 * C1 <=> b1
	 * C2 <=> b2
	 * b=b1*b2
	 */
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		BoolVar[] reifyArray = model.boolVarArray(constraintSet.size());

		int i=0;
		for(ACQ_IConstraint c: constraintSet)
		{
			c.toReifiedChoco(model, reifyArray[i], intVars);
			i++;
		}

		model.min(b, reifyArray).post();
		//	model.arithm(reifyArray[0], "*", reifyArray[1], "=", b).post();

	}

	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public boolean check(ACQ_Query query) {
		for(ACQ_IConstraint c: constraintSet) {
			int value[] = c.getProjection(query);
			if(((ACQ_Constraint) c).check(value))
				return true;
		}
		return false;
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	protected int findIndex(int[] l, int a) {
		for (int i = 0; i < l.length; i++) {
			if (l[i] == a)
				return i;
		}
		return -1;
	}
	
	@Override
	public boolean check(int... value) {


		for(ACQ_IConstraint c: constraintSet) {
			int cvars[] = c.getVariables();
			int projection[] = new int[cvars.length];
			int index = 0;
			for (int var: cvars) {
				int valindex = findIndex(this.variables, var);
				assert valindex >= 0;
				projection[index] = value[valindex];
				index++;
			}
			
			if(((ACQ_Constraint) c).check(projection))
				return true;
		}
		return false;
	}
	
	

	public int getNbCsts() {

		return constraintSet.size();
	}

	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.constraintSet.size();
		for (int i = 0; i < this.constraintSet.size(); i++) {
			ACQ_IConstraint acqconstr = this.constraintSet.get_Constraint(i);
			result = prime * result + acqconstr.hashCode();
		}
		return result;
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACQ_DisjunctionConstraint other = (ACQ_DisjunctionConstraint) obj;
		
		if (this.constraintSet.size() != other.constraintSet.size())
			return false;
		for(int i = 0; i < this.constraintSet.size(); i++) {
			if (!this.constraintSet.get_Constraint(i).equals(other.constraintSet.get_Constraint(i)))
				return false;
		}
		
		if(!Arrays.equals(this.getVariables(), other.getVariables()))
			return false;
		return true;
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public boolean contains(ACQ_IConstraint constr) {
		for (ACQ_IConstraint subconstr : constraintSet) {
			if (constr.equals(subconstr))
				return true;
		}
		return false;
	}

	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	public boolean contains(ACQ_Network net) {
		for (ACQ_IConstraint c : net) {
			if (!this.contains(c)) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public String toSmtlib() {
		String res = "(or";
		for (ACQ_IConstraint c: constraintSet) {
			res += " " + c.toSmtlib();
		}
		res += ")";
		return res;
	}
	
	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public int getWeight() {
		return this.weight;
	}
}
