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
import io.github.binsec.core.acqvariable.ACQ_Variable;
import io.github.binsec.core.acqvariable.CellType;
import io.github.binsec.core.learner.ACQ_Query;

public class PtrCellBinaryGTConstraint extends BinaryConstraint {
	boolean negated = false;
	ACQ_CellVariable cell1;
	ACQ_CellVariable cell2;
	ACQ_Variable[] vars; // length = 2
	
	public PtrCellBinaryGTConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2) { 
		super("PtrGT", cell1.getValue().id, cell2.getValue().id); // TODO : WARNING I don't know if it is good
		
		this.cell1 = cell1;
		this.cell2= cell2;
		
		ACQ_Variable v1 = cell1.getValue();
		ACQ_Variable v2 = cell2.getValue();
		if (v1.getType() != CellType.PTR || v2.getType() != CellType.PTR) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {v1, v2};
		}
	}
	
	public PtrCellBinaryGTConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2, boolean negated) {
		this(cell1, cell2);
		if (negated) {
			this.setName("PtrLE");
			this.negated = negated;
		}
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new PtrCellBinaryGTConstraint(this.cell1, this.cell2, !this.negated);
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint cst;
		
		IntVar var0 = null;
		IntVar var1 = null;
		
		for (int i = 0; i < intVars.length && (var0 == null || var1 == null); i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			} else if (intVars[i].getName().equals(vars[1].getName())) {
				var1 = intVars[i];
			}
		}
		
		if (!negated) {
			cst = model.arithm(var0, ">", var1);
		} else {
			cst = model.arithm(var0, "<=", var1);
		}
		return new Constraint[] {cst};
	}

	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		
		IntVar var0 = null;
		IntVar var1 = null;
		
		for (int i = 0; i < intVars.length && (var0 == null || var1 == null); i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			} else if (intVars[i].getName().equals(vars[1].getName())) {
				var1 = intVars[i];
			}
		}
		
		if (!negated) {
			model.arithm(var0, ">", var1).reifyWith(b);
		} else {
			model.arithm(var0, "<=", var1).reifyWith(b);
		}
	}
	
	@Override
	public String getNegName() {
		if (!negated) {
			return "PtrGT";
		}
		else {
			return "PtrLE";
		}
	}

	@Override
	public String toSmtlib() {
		String p = "(ptrgt v" + this.cell1.cellid + " v" + this.cell2.cellid + ")";
		if (!negated) return p;
		else return "(not " + p + ")";
	}

	@Override
	protected boolean check(int value1, int value2) { // Better if prams could be cells
		if (negated) {
			return value1 <= value2;
		}
		else {
			return value1 > value2;
		}
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
		result = prime * result + cell2.hashCode();
		
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
		PtrCellBinaryGTConstraint other = (PtrCellBinaryGTConstraint) obj;
		if (this.negated != other.negated) {
			return false;
		}
		if (!this.cell1.equals(other.cell1)) {
			return false;
		}
		if (!this.cell2.equals(other.cell2)) {
			return false;
		}

		return true;
	}
	
	@Override
	public String toString() {
		String val1 = "var" + this.cell1.cellid;
		String val2 = "var" + this.cell2.cellid;
		return getName() + "(" + val1 + ", " + val2 + ")";
	}
}
