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


public class OverlapConstraint extends ScalarConstraint {

	boolean negated = false;
	int weight = 1;
	ACQ_CellVariable cell1;
	ACQ_CellVariable cell2;
	ACQ_Variable[] vars; // length = 4
	
	public OverlapConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2) {
		
		super("Overlap", 
				new int[] {cell1.getValue().id, cell1.getSize().id,cell2.getValue().id, cell2.getSize().id},
				new int[] {}, 0);
		
		cell1.setBigString();
		cell2.setBigString();
		this.cell1 = cell1;
		this.cell2= cell2;
		
		ACQ_Variable v1 = cell1.getValue();
		ACQ_Variable v2 = cell1.getSize();
		ACQ_Variable v3 = cell2.getValue();
		ACQ_Variable v4 = cell2.getSize();
		if (v1.getType() != CellType.PTR || v3.getType() != CellType.PTR) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {v1, v2, v3, v4};
		}
	}
	
	public OverlapConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2, boolean negated) {
		this(cell1, cell2);
		if (negated) {
			this.setName("NotOverlap");
			this.negated = negated;
		}
	}
	
	public OverlapConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2, int weight) {
		this(cell1, cell2);
		this.weight = weight;
	}
	
	public OverlapConstraint(ACQ_CellVariable cell1, ACQ_CellVariable cell2, boolean negated, int weight) {
		this(cell1, cell2, weight);
		if (negated) {
			this.setName("NotOverlap");
			this.negated = negated;
		}
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new OverlapConstraint(this.cell1, this.cell2, !this.negated);
	}
	
	public ACQ_IConstraint getNegation(int weight) {
		return new OverlapConstraint(this.cell1, this.cell2, !this.negated, weight);
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint cst;
		
		IntVar var0 = null;
		IntVar var0size = null;
		IntVar var1 = null;
		IntVar var1size = null;
		
		for (int i = 0; i < intVars.length && 
				(var0 == null || var0size == null || var1 == null || var1size == null); i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			} else if (intVars[i].getName().equals(vars[1].getName())) {
				var0size = intVars[i];
			} else if (intVars[i].getName().equals(vars[2].getName())) {
				var1 = intVars[i];
			} else if (intVars[i].getName().equals(vars[3].getName())) {
				var1size = intVars[i];
			}
		}
		
		var0 = model.intScaleView(var0, 1000);
		var1 = model.intScaleView(var1, 1000);
		
		if (!negated) {
			BoolVar[] reifyArray = model.boolVarArray(2);
			reifyArray[0] = model.and(
								model.arithm(var0, "<=", var1),
								model.arithm(var0, "+", var0size, ">", var1)).reify();
			reifyArray[1] = model.and(
					model.arithm(var1, "<=", var0),
					model.arithm(var1, "+", var1size, ">", var0)).reify();
			cst = model.sum(reifyArray, ">", 0);
		} else {
			BoolVar[] reifyArray1 = model.boolVarArray(2);
			BoolVar[] reifyArray2 = model.boolVarArray(2);
			
			reifyArray1[0] = model.arithm(var0, ">", var1).reify();
			reifyArray1[1] = model.arithm(var0, "+", var0size, "<=", var1).reify();
			
			reifyArray2[0] = model.arithm(var1, ">", var0).reify();
			reifyArray2[1] = model.arithm(var1, "+", var1size, "<=", var0).reify();
			
			cst = model.and(
					model.sum(reifyArray1, ">", 0),
					model.sum(reifyArray2, ">", 0));
		}
		return new Constraint[] {cst};
	}

	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		
		IntVar var0 = null;
		IntVar var0size = null;
		IntVar var1 = null;
		IntVar var1size = null;
		
		for (int i = 0; i < intVars.length && 
				(var0 == null || var0size == null || var1 == null || var1size == null); i++) {
			if (intVars[i].getName().equals(vars[0].getName())) {
				var0 = intVars[i];
			} else if (intVars[i].getName().equals(vars[1].getName())) {
				var0size = intVars[i];
			} else if (intVars[i].getName().equals(vars[2].getName())) {
				var1 = intVars[i];
			} else if (intVars[i].getName().equals(vars[3].getName())) {
				var1size = intVars[i];
			}
		}
		
		var0 = model.intScaleView(var0, 1000);
		var1 = model.intScaleView(var1, 1000);
		
		if (!negated) {
			BoolVar[] reifyArray = model.boolVarArray(2);
			reifyArray[0] = model.and(
								model.arithm(var0, "<=", var1),
								model.arithm(var0, "+", var0size, ">", var1)).reify();
			reifyArray[1] = model.and(
					model.arithm(var1, "<=", var0),
					model.arithm(var1, "+", var1size, ">", var0)).reify();
			model.sum(reifyArray, ">", 0).reifyWith(b);
		} else {
			BoolVar[] reifyArray1 = model.boolVarArray(2);
			BoolVar[] reifyArray2 = model.boolVarArray(2);
			
			reifyArray1[0] = model.arithm(var0, ">", var1).reify();
			reifyArray1[1] = model.arithm(var0, "+", var0size, "<=", var1).reify();
			
			reifyArray2[0] = model.arithm(var1, ">", var0).reify();
			reifyArray2[1] = model.arithm(var1, "+", var1size, "<=", var0).reify();
			
			model.and(
					model.sum(reifyArray1, ">", 0),
					model.sum(reifyArray2, ">", 0)).reifyWith(b);
		}
		
	}
	
	@Override
	public String getNegName() {
		if (!negated) {
			return "NotOverlap";
		}
		else {
			return "Overlap";
		}
	}

	@Override
	public String toSmtlib() {
		String o = "(overlap v" + this.cell1.cellid + " v" + this.cell2.cellid + ")";
		if (!negated) return o;
		else return "(not " + o + ")";
	}

	@Override
	public int[] getProjection(ACQ_Query query) {
		return new int[] {query.values[cell1.getValue().id], 
				query.values[cell1.getSize().id], 
				query.values[cell2.getValue().id], 
				query.values[cell2.getSize().id]};
	}
	
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value, null);
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
		OverlapConstraint other = (OverlapConstraint) obj;
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
	
	@Override
	public int getWeight() {
		return this.weight;
	}

	@Override
	protected boolean check(int[] value, int[] coeff) {
		int v0 = value[0];
		int v0size = value[1];
		int v1 = value[2];
		int v1size = value[3];
		
		v0 = 1000*v0;
		v1 = 1000*v1;
		
		if (!negated) {
			return (v0 <= v1 && v0 + v0size > v1) || (v1 <= v0 && v1 + v1size > v0); 
		}
		else {
			return (v0 > v1 || v0 + v0size <= v1) && (v1 > v0 || v1 + v1size <= v0);
		}
	}

}
