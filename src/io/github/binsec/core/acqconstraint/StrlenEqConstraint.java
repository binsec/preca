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

import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.acqvariable.ACQ_ValueVariable;
import io.github.binsec.core.acqvariable.ACQ_Variable;
import io.github.binsec.core.acqvariable.CellType;
import io.github.binsec.core.learner.ACQ_Query;

public class StrlenEqConstraint extends UnaryConstraint {
	ACQ_CellVariable cell1 = null;
	int cst;
	boolean negated = false; 
	int weight = 1;
	ACQ_Variable[] vars;
	
	public StrlenEqConstraint(ACQ_CellVariable cell1, int cst) {
		super("StrlenEq" + (cst-1), cell1.getValue().id);
		
		cell1.setBigString();
		this.cell1 = cell1;
		this.cst = cst;
		
		if (cell1.getType() != CellType.PTR) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {cell1.getSize()};
		}
	}
	
	public StrlenEqConstraint(ACQ_CellVariable cell1, int cst, boolean negated) {
		this(cell1, cst);
		if (negated) {
			this.setName("NotStrlenEq" + (this.cst-1));
			this.negated = negated;
		}
	}
	
	
	public StrlenEqConstraint(ACQ_CellVariable cell1, int cst, int weight) {
		this(cell1, cst);
		this.weight = weight;
	}
	
	public StrlenEqConstraint(ACQ_CellVariable cell1, int cst, boolean negated, int weight) {
		this(cell1, cst, weight);
		if (negated) {
			this.setName("NotStrlenEq" + (this.cst-1));
			this.negated = negated;
		}
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new StrlenEqConstraint(this.cell1, this.cst, !this.negated);
	}
	
	public ACQ_IConstraint getNegation(int weight) {
		return new StrlenEqConstraint(this.cell1, this.cst, !this.negated, weight);
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint cst;
		
		IntVar var0 = null;
		
		for (int i = 0; i < intVars.length && var0 == null; i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			}
		}
		
		if (!negated) {
			cst = model.arithm(var0, "=", this.cst);
		} else {
			cst = model.arithm(var0, "!=", this.cst);
		}
		return new Constraint[] {cst};
	}

	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		
		IntVar var0 = null;
		
		for (int i = 0; i < intVars.length && var0 == null; i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			}
		}
		
		if (!negated) {
			model.arithm(var0, "=", this.cst).reifyWith(b);
		} else {
			model.arithm(var0, "!=", this.cst).reifyWith(b);
		}
		
	}

	@Override
	public String getNegName() {
		if (!negated) {
			return "NotStrlenEq" + this.cst;
		}
		else {
			return "StrlenEq" + this.cst;
		}
	}

	@Override
	public String toSmtlib() {
		String val1 = "v" + this.cell1.cellid; 
		if (!negated) {
			return "(strleneq " + val1 + " #x" + String.format("%08x", this.cst-1) + ")";
		} else {
			return "(not (strleneq " + val1 + " #x" + String.format("%08x", this.cst-1) + "))";
		}
	}
	
	@Override
	protected boolean check(int value) {
		if (negated) {
			return value != this.cst;
		}
		else {
			return value == this.cst;
		}
	}

	@Override
	public int[] getProjection(ACQ_Query query) {
		return new int[] {query.values[((ACQ_ValueVariable)this.vars[0]).id]};
	}
	
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cell1.hashCode();
		
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
		StrlenEqConstraint other = (StrlenEqConstraint) obj;
		if (this.negated != other.negated) {
			return false;
		}
		if (!this.cell1.equals(other.cell1)) {
			return false;
		}

		if (this.cst != other.cst) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		String val1 = "var" + this.cell1.cellid;
		return getName() + "(" + val1 + ")";
	}
	
	@Override
	public int getWeight() {
		return this.weight;
	}
	
	@Override
	public int[] getVariables() {
		return new int[] {cell1.getSize().id};
	}
}
