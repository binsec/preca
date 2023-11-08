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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.binsec.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.tools.NameService;

/**
 *
 * @author agutierr
 */
public class UnaryArithmetic extends UnaryConstraint {
	final private Operator op;
	final private int cste;
	public UnaryArithmetic(String name, int var, Operator op, int cste){
		super(name,var);
		this.op=op;
		this.cste=cste;
	}
	@Override
	protected boolean check(int value) {
		switch (op) {
		case EQ:
			return value == cste;
		case NQ:
			return value != cste;
		case GT:
			return value > cste;
		case GE:
			return value >= cste;
		case LT:
			return value < cste;
		case LE:
			return value <= cste;
		default:
			assert false : "Operator not handled";
			break;
		}
		return false;

	}
	@Override
	public UnaryArithmetic getNegation(){
		return new UnaryArithmetic(getNegName(),getScope().getFirst(),Operator.getOpposite(op),cste);
	}

	@Override
	public Constraint[] getChocoConstraints(Model model,IntVar... intVars) {
		IntVar l=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(this.getVariables()[0])))
					l= v;
		}
		
		return new Constraint[]{model.arithm(l, op.toString(), cste)};
		/*
		try {
			return new Constraint[]{model.arithm(intVars[this.getVariables()[0]], op.toString(), cste)};
		}
		catch (Exception e) {
			return new Constraint[0];
		}*/
	}
	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		/*int left=0;
		if(intVars.length>=this.getVariables().length)
		{
			left= this.getVariables()[0];
		}
		IntVar l=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(left)))
					l= v;
		}*/
		
		model.arithm(intVars[this.getVariables()[0]], op.toString(), cste).reifyWith(b);

	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cste;
		result = prime * result + ((op == null) ? 0 : op.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnaryArithmetic other = (UnaryArithmetic) obj;
		if (cste != other.cste)
			return false;
		if (op != other.op)
			return false;
		if(!Arrays.equals(this.getVariables(), other.getVariables()))
			return false;
		return true;
	}
	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		String newname = this.getName();
		if(this.getName().startsWith("Not")) {
			newname = newname.substring(3);
		}
		else {
			newname = "Not" + newname;
		}
		
		return newname;
	}
	
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}
	
	@Override
	public String toSmtlib() {
		String var1 = "x" + variables[0];
		switch (op) {
		case EQ:
			return "(= " + var1 + " " + cste + ")";
		case NQ:
			return "(distinct " + var1 + " " + cste + ")";
		case GT:
			return "(bvsgt " + var1 + " " + cste + ")";
		case GE:
			return "(bvsge " + var1 + " " + cste + ")";
		case LT:
			return "(bvslt " + var1 + " " + cste + ")";
		case LE:
			return "(bvsle " + var1 + " " + cste + ")";
		default:
			assert false : "unknown operator";

		}
		return null;
	}
	
	@Override
	public String toString() {
		return getName() + "_" + cste + Arrays.toString(variables);
	}
}
