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

import io.github.binsec.core.learner.ACQ_Query;

public class TrueConstraint extends ACQ_Constraint {

	public TrueConstraint() {
		super("True", new int[] {});
		// TODO Auto-generated constructor stub
	}

	@Override
	public ACQ_IConstraint getNegation() {
		return new FalseConstraint();
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		return new Constraint[] {model.trueConstraint()};
	}

	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		model.falseConstraint().reifyWith(b);
		
	}

	@Override
	public String getNegName() {
		return "False";
	}

	@Override
	public String toSmtlib() {
		return "true";
	}

	@Override
	public boolean check(int... value) {
		return true;
	}

	@Override
	public boolean check(ACQ_Query query) {
		return true;
	}

}
