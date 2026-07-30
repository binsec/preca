package constracq.core.acqconstraint;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.learner.ACQ_Query;
import io.github.binsec.core.ACQ_Utils;


public class ACQ_MetaConstraint extends ACQ_Constraint{

	public ConstraintSet constraintSet;
	
	String name;

	public ConstraintFactory constraintFactory;

	public ACQ_MetaConstraint(ConstraintFactory constraintFactory,String name, ACQ_IConstraint c1, ACQ_IConstraint c2) {

		super(name, ACQ_Utils.mergeWithoutDuplicates(c1.getVariables(), c2.getVariables()));
		this.constraintFactory = constraintFactory;
		this.constraintSet= this.constraintFactory.createSet();
		if (c1 instanceof ACQ_MetaConstraint)
			this.constraintSet.addAll(((ACQ_MetaConstraint) c1).constraintSet);	
		else
			this.constraintSet.add(c1);


		if (c2 instanceof ACQ_MetaConstraint)
			this.constraintSet.addAll(((ACQ_MetaConstraint) c2).constraintSet);	
		else
			this.constraintSet.add(c2);
		
		/* Start by Gregoire MENGUY (CEA LIST) */
		if (name == "disjunction" || name == "conjunction") {
			this.name = "";
			String op = name == "disjunction" ? "_or_" : "_and_";
			int i = 0;
			for (ACQ_IConstraint constr : constraintSet) {
				if (i == constraintSet.size()-1)
					break;
				this.name += constr.toString() + op;
				i++;
			}
			this.name += constraintSet.get_Constraint(i);
			this.setName(this.name);
		} else {
			this.name=name;
			for(ACQ_IConstraint c: constraintSet)
				this.name+=("_"+c.getName());
			this.setName(this.name);
		}
		/* End by Gregoire MENGUY (CEA LIST) */
		
		
	}
	
	public ACQ_MetaConstraint(ConstraintFactory constraintFactory, String name, ConstraintSet set) {

		super("meta", set.getVariables());
		this.constraintFactory = constraintFactory;
		this.constraintSet= set;

		/* Start by Gregoire MENGUY (CEA LIST) */
		if (name == "disjunction" || name == "conjunction") {
			this.name = "";
			String op = name == "disjunction" ? "_or_" : "_and_";
			int i = 0;
			for (ACQ_IConstraint constr : constraintSet) {
				if (i == constraintSet.size()-1)
					break;
				this.name += constr.toString() + op;
				i++;
			}
			this.name += constraintSet.get_Constraint(i);
			this.setName(this.name);
		} 
		else {
			this.name=name;
			for(ACQ_IConstraint c: constraintSet)
				this.name+=("_"+c.getName());
			this.setName(this.name);
		}
		/* End by Gregoire MENGUY (CEA LIST) */
		
	}
	

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint[] chocoConstraints=new Constraint[0];
		for(ACQ_IConstraint c: constraintSet)
			chocoConstraints=ArrayUtils.append(chocoConstraints,c.getChocoConstraints(model, intVars));

		return chocoConstraints;
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
	 * by Gregoire MENGUY (CEA LIST)
	 */
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}

	
	@Override
	public boolean check(int... value) {


		for(ACQ_IConstraint c: constraintSet)
			if(!((ACQ_Constraint) c).check(value))
				return false;

		return true;
	}

	public int getNbCsts() {

		return constraintSet.size();
	}

	@Override
	public ACQ_IConstraint getNegation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * by Gregoire Menguy (CEA LIST)
	 */
	@Override
	public String toSmtlib() {
		return null;
	}

	
	



}
