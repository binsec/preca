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

public class Unit {
	
	protected String varname;
	final protected int minisatvariable;
	final protected ACQ_IConstraint constraint;
	protected Boolean negated;
	
	public Unit(ACQ_IConstraint constr, int var, Boolean neg) {
		// when using MiniSat solver
		varname = constr.getName();
		for (int vari : constr.getVariables()) {
			varname += "_" + vari;
		}
		constraint = constr;
		assert var > 0 : "minisat variables must be > 0";
		minisatvariable = var;
		negated = neg;
	}
	
	public void setNeg() {
		assert(negated == false);
		negated = true;
	}
	
	public void unsetNeg() {
		assert(negated == true);
		negated = false;
	}
	
	public Boolean isNeg() {
		return negated;
	}
	
	public int getMiniSatVar() {
		return minisatvariable;
	}
	
	public int toMiniSat() {
		if (negated) {
			return -minisatvariable;
		}
		else {
			return minisatvariable;
		}
	}
	
	public String toNaPS() {
		if (negated) {
			return "~x" + minisatvariable;
		}
		else {
			return "x" + minisatvariable;
		}
	}
	
	public Unit clone() {
		if(minisatvariable > 0) {
			return new Unit(constraint, minisatvariable, negated);
		}
		else {
			assert false : "Unknown unit configuration";
			return null;
		}
	}
	
	public ACQ_IConstraint getConstraint() {
		return constraint;
	}
	
	
	public Boolean equalsConstraint(ACQ_IConstraint constr) {
		
		if (!constr.getName().equals(constraint.getName()) 
				|| constr.getArity() != constraint.getArity())
			return false;
		
		int[] constrVars = constr.getVariables();
		int[] constraintVars = constraint.getVariables();
		for (int index=0; index < constrVars.length; index++) {
			if(constrVars[index] != constraintVars[index])
				return false;
		}
		return true;
	}
	
	public Boolean equalsConstraint(Unit unit) {
		return equalsConstraint(unit.getConstraint());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + varname.hashCode();
		result = prime * result + constraint.hashCode();
		result = prime * result + negated.hashCode();
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
		Unit u = (Unit) obj;
		return equals(u);
	}
	
	public Boolean equals(Unit unit) {
		return equalsConstraint(unit.getConstraint()) && unit.isNeg() == this.isNeg();
	}
	
	public Boolean isOpposite(Unit unit) {
		ACQ_IConstraint constr = unit.getConstraint();
		ACQ_IConstraint nega = constraint.getNegation();
		
		if (!constr.getName().equals(nega.getName()) 
				|| constr.getArity() != nega.getArity())
			return false;
		
		int[] constrVars = constr.getVariables();
		int[] negaVars = nega.getVariables();
		for (int index=0; index < constrVars.length; index++) {
			if(constrVars[index] != negaVars[index])
				return false;
		}
		return true;
	}
	
	public Boolean isOpposite(ACQ_IConstraint constr) {
		ACQ_IConstraint nega = constraint.getNegation();
		
		if (!constr.getName().equals(nega.getName()) 
				|| constr.getArity() != nega.getArity())
			return false;
		
		int[] constrVars = constr.getVariables();
		int[] negaVars = nega.getVariables();
		for (int index=0; index < constrVars.length; index++) {
			if(constrVars[index] != negaVars[index])
				return false;
		}
		return true;
	}
	
	public String toString() {
		if(negated) {
			return "~a(" + varname + ")";
		}
		else {
			return "a(" + varname + ")";
		}
	}
	
}
