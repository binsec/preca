/****************************************************************************/
/*  This file is part of PRECA.                                             */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.  */
/*                                                                          */
/*  Copyright (C) 2019-2023                                                 */
/*    CEA (Commissariat à l'énergie atomique et aux énergies                */
/*         alternatives)                                                    */
/*                                                                          */
/*  you can redistribute it and/or modify it under the terms of the GNU     */
/*  Lesser General Public License as published by the Free Software         */
/*  Foundation, version 2.1.                                                */
/*                                                                          */
/*  It is distributed in the hope that it will be useful,                   */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of          */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           */
/*  GNU Lesser General Public License for more details.                     */
/*                                                                          */
/*  See the GNU Lesser General Public License version 2.1                   */
/*  for more details (enclosed in the file licenses/LGPLv2.1).              */
/*                                                                          */
/****************************************************************************/

package io.github.binsec.core.acqconstraint;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import io.github.binsec.core.ACQ_Utils;
import io.github.binsec.core.acqconstraint.ConstraintFactory.ConstraintSet;
import io.github.binsec.core.learner.ACQ_Query;


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
		
		
	}
	
	public ACQ_MetaConstraint(ConstraintFactory constraintFactory, String name, ConstraintSet set) {

		super("meta", set.getVariables());
		this.constraintFactory = constraintFactory;
		this.constraintSet= set;
		
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

	@Override
	public String toSmtlib() {
		return null;
	}

	
	



}
