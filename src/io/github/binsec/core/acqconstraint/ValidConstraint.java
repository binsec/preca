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

public class ValidConstraint extends UnaryConstraint {
	ACQ_CellVariable cell1 = null;
	boolean negated = false; 
	int weight = 1;
	ACQ_Variable[] vars;
	
	public ValidConstraint(ACQ_CellVariable cell1) {
		super("Valid", cell1.getValue().id);
		
		this.cell1 = cell1;
		
		ACQ_Variable v1 = cell1.getValue();
		if (v1.getType() != CellType.PTR) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {v1};
		}
	}
	
	public ValidConstraint(ACQ_CellVariable cell1, boolean negated) {
		this(cell1);
		if (negated) {
			this.setName("NotValid");
			this.negated = negated;
		}
	}
	
	
	public ValidConstraint(ACQ_CellVariable cell1, int weight) {
		this(cell1);
		this.weight = weight;
	}
	
	public ValidConstraint(ACQ_CellVariable cell1, boolean negated, int weight) {
		this(cell1, weight);
		if (negated) {
			this.setName("NotValid");
			this.negated = negated;
		}
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new ValidConstraint(this.cell1, !this.negated);
	}
	
	public ACQ_IConstraint getNegation(int weight) {
		return new ValidConstraint(this.cell1, !this.negated, weight);
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
			cst = model.arithm(var0, "!=", 0);
		} else {
			cst = model.arithm(var0, "=", 0);
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
			model.arithm(var0, "!=", 0).reifyWith(b);
		} else {
			model.arithm(var0, "=", 0).reifyWith(b);
		}
		
	}

	@Override
	public String getNegName() {
		if (!negated) {
			return "NotValid";
		}
		else {
			return "Valid";
		}
	}

	@Override
	public String toSmtlib() {
		
		String v = "(valid v" + this.cell1.cellid + ")";
		if (!negated) return v;
		else return "(not " + v + ")";
	}

	@Override
	protected boolean check(int value) {
		if (negated) {
			return value == 0;
		}
		else {
			return value != 0;
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
		ValidConstraint other = (ValidConstraint) obj;
		if (this.negated != other.negated) {
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
		return getName() + "(" + val1 + ")";
	}
	
	@Override
	public int getWeight() {
		return this.weight;
	}
}
