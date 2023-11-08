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

public class IntCellUnaryEqConstraint extends UnaryConstraint {
	ACQ_CellVariable cell1 = null;
	public int constant; 
	boolean negated = false; 
	ACQ_Variable[] vars;
	int weight = 1;
	
	public IntCellUnaryEqConstraint(ACQ_CellVariable cell1, int constant) {
		super("Eq_"+constant, cell1.getValue().id);
		
		this.cell1 = cell1;
		this.constant = constant;
		
		cell1.addValue(constant);
		cell1.addValue(constant+1);
		cell1.addValue(constant-1);
		
		ACQ_Variable v1 = cell1.getValue();
		if (v1.getType() != CellType.INT && v1.getType() != CellType.UINT) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {v1};
		}
	}
	
	public IntCellUnaryEqConstraint(ACQ_CellVariable cell1, int constant, boolean negated) {
		this(cell1, constant);
		if (negated) {
			this.setName("Neq_" + constant);
			this.negated = negated;
		}
	}
	
	public IntCellUnaryEqConstraint(ACQ_CellVariable cell1, int constant, int weight) {
		this(cell1, constant);
		this.weight = weight;
	}
	
	public IntCellUnaryEqConstraint(ACQ_CellVariable cell1, int constant, boolean negated, int weight) {
		this(cell1, constant, negated);
		this.weight = weight;
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new IntCellUnaryEqConstraint(this.cell1, this.constant, !this.negated);
	}
	
	public ACQ_IConstraint getNegation(int weight) {
		return new IntCellUnaryEqConstraint(this.cell1, this.constant, !this.negated, weight);
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
			cst = model.arithm(var0, "=", this.constant);
		} else {
			cst = model.arithm(var0, "!=", this.constant);
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
			model.arithm(var0, "=", this.constant).reifyWith(b);
		} else {
			model.arithm(var0, "!=", this.constant).reifyWith(b);
		}
		
	}

	@Override
	public String getNegName() {
		if (!negated) {
			return "Eq_" + this.constant;
		}
		else {
			return "Neq_" + this.constant;
		}
	}

	@Override
	public String toSmtlib() {
		String val1 = "v" + this.cell1.cellid;
		if (!negated) {
			return "(eq " + val1 + " " + String.format("#x%08x", this.constant) + ")";
		} else {
			return "(not (eq " + val1 + " " + String.format("#x%08x", this.constant) + "))";
		}
	}

	@Override
	protected boolean check(int value) {
		if (negated) {
			return value != this.constant;
		}
		else {
			return value == this.constant;
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
		result = prime * result + this.constant;
		
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
		IntCellUnaryEqConstraint other = (IntCellUnaryEqConstraint) obj;
		if (this.negated != other.negated) {
			return false;
		}
		
		if (this.constant != other.constant) {
			return false;
		}
		
		if (!this.cell1.equals(other.cell1)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		String val1 = "var" + this.cell1.cellid;
		return getName() + "(" + val1 + ", " + this.constant + ")";
	}
	
	@Override
	public int getWeight() {
		return this.weight;
	}
}
