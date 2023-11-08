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

public class IntCellUnaryModConstraint extends UnaryConstraint {
	ACQ_CellVariable cell1 = null;
	public int mod; 
	public int res;
	boolean negated = false; 
	ACQ_Variable[] vars;
	int weight = 1;
	
	public IntCellUnaryModConstraint(ACQ_CellVariable cell1, int mod, int res) {
		super("Mod_"+mod+"_"+res, cell1.getValue().id);
		
		this.cell1 = cell1;
		this.mod = mod;
		this.res = res;
		
		cell1.addValue(mod);
		cell1.addValue(mod+1);
		cell1.addValue(mod-1);
		
		ACQ_Variable v1 = cell1.getValue();
		if (v1.getType() != CellType.INT && v1.getType() != CellType.UINT) {
			System.err.println("Constraint badly typed");
			System.exit(1);
		} else {
			this.vars = new ACQ_Variable[] {v1};
		}
	}
	
	public IntCellUnaryModConstraint(ACQ_CellVariable cell1, int mod, int res, boolean negated) {
		this(cell1, mod, res);
		if (negated) {
			this.setName("NotMod_" + mod + "_" + res);
			this.negated = negated;
		}
	}
	
	public IntCellUnaryModConstraint(ACQ_CellVariable cell1, int mod, int res, int weight) {
		this(cell1, mod, res);
		this.weight = weight;
	}
	
	public IntCellUnaryModConstraint(ACQ_CellVariable cell1, int mod, int res, boolean negated, int weight) {
		this(cell1, mod, res, negated);
		this.weight = weight;
	}


	@Override
	public ACQ_IConstraint getNegation() {
		return new IntCellUnaryModConstraint(this.cell1, mod, res, !this.negated);
	}
	
	public ACQ_IConstraint getNegation(int weight) {
		return new IntCellUnaryModConstraint(this.cell1, mod, res, !this.negated, weight);
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
		

		IntVar z = var0.mod(mod).intVar();
		if (!negated) {
			cst = model.arithm(z, "=", res);
		} else {
			cst = model.arithm(z, "!=", res);
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
		
		IntVar z = var0.mod(mod).intVar();
		if (!negated) {
			model.arithm(z, "=", res).reifyWith(b);
		} else {
			model.arithm(z, "!=", res).reifyWith(b);
		}
		
	}

	
	@Override
	public String getName() {
		if (!negated) {
			return "Mod_" + mod + "_" + res;
		}
		else {
			return "NotMod_" + mod + "_" + res;
		}
	}
	
	@Override
	public String getNegName() {
		if (!negated) {
			return "NotMod_" + mod + "_" + res;
		}
		else {
			return "Mod_" + mod + "_" + res;
		}
	}

	@Override
	public String toSmtlib() {
		String val1 = "v" + this.cell1.cellid; 
		if (!negated) {
			if (this.cell1.getType() == CellType.INT) {
				return "(smod " + val1 + " " + String.format("#x%08x", this.mod) + 
						" " + String.format("#x%08x", this.res)+ ")";
			}
			else if (this.cell1.getType() == CellType.UINT) {
				return "(umod " + val1 + " " + String.format("#x%08x", this.mod) + 
						" " + String.format("#x%08x", this.res)+ ")";
			}
			else {
				assert false;
				return null;
			}
		} else {
			if (this.cell1.getType() == CellType.INT) {
				return "(not (smod " + val1 + " " + String.format("#x%08x", this.mod) + 
						" " + String.format("#x%08x", this.res)+ "))";
			}
			else if (this.cell1.getType() == CellType.UINT) {
				return "(not (umod " + val1 + " " + String.format("#x%08x", this.mod) + 
						" " + String.format("#x%08x", this.res)+ "))";
			}
			else {
				assert false;
				return null;
			}
		}
	}

	@Override
	protected boolean check(int value) {
		if (negated) {
			return value % mod != res;
		}
		else {
			return value % mod == res;
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
		result = prime * result + this.mod;
		result = prime * result + this.res;
		
		
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
		IntCellUnaryModConstraint other = (IntCellUnaryModConstraint) obj;
		if (this.negated != other.negated) {
			return false;
		}
		
		if (this.mod != other.mod) {
			return false;
		}
		
		if (this.res != other.res) {
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
