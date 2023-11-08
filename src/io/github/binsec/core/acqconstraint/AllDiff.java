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

import java.util.Set;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import io.github.binsec.core.learner.ACQ_Query;

/**
 *
 * @author NADJIB
 */

public  class AllDiff extends ACQ_Constraint{

	public AllDiff(Set<Integer> clique) {

		super("AllDiff",clique);
	}
	@Override
	public boolean check(int... value) {
		return check(value[0],value[1]);
	}
	protected  boolean check(int value1,int value2) {
		//TODO
		return false;
	}
	@Override
	public ACQ_IConstraint getNegation() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		
		int[] alldiff_vars = this.getVariables();
		IntVar[] vars=new IntVar[alldiff_vars.length];
	
		
		for(int i=0; i<alldiff_vars.length; i++) {
				vars[i]=intVars[alldiff_vars[i]];
			
			 }
		return new Constraint[]{model.allDifferent(vars)};

	}
	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {

		model.allDifferent(intVars).reifyWith(b);

	}
	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}
	@Override
	public String toSmtlib() {
		// TODO Auto-generated method stub
		return null;
	}

}

