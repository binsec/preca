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

package io.github.binsec.core.acqsolver;

import java.util.ArrayList;

import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.RevConstraintMapping;
import io.github.binsec.core.acqconstraint.Unit;

public class MiniSatModel extends SATModel {

	ArrayList<Integer> model;
	RevConstraintMapping revmapping;
	
	public MiniSatModel(ArrayList<Integer> model, RevConstraintMapping revmap) {
		this.model = model;
		this.revmapping = revmap;
	}
	
	public MiniSatModel(ArrayList<Integer> model) {
		this(model, null);
	}
	
	@Override
	public Boolean get(Unit unit) {
		assert !unit.isNeg() : "unit cannot be negative";
		int constrid = unit.getMiniSatVar();
		for (int i = 0; i < model.size(); i++) {
			if(model.get(i) == constrid || model.get(i) == -constrid) {
				assert model.get(i) != 0;
				if (model.get(i) < 0) {
					assert false;
					return false;
				}
				else {
					return true;
				}
			}
		}
		return false; // By default if the literal is not in the model it is set to false
	}

	@Override
	public String toString() {
		String res = "(";
		for (Unit unit : revmapping.values()) {
			if (this.get(unit))
				res += unit.toString() + "= 1, ";
			else
				res += unit.toString() + "= 0, ";
		}
		res += ")";
		
		return res;
	}
	
	@Override
	public ACQ_IConstraint[] getPositive() {
		ACQ_IConstraint[] res = new ACQ_IConstraint[model.size()];
		for (int i = 0; i < model.size(); i++) {
			Unit u = revmapping.get(model.get(i));
			res[i] = u.getConstraint();
		}
		return res;
	}

}
