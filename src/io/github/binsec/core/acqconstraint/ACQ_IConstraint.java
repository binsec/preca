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

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.ACQ_Scope;

/**
 * Interface that lists the functions to be implemented by each constraint class
 * 
 *
 */
public interface ACQ_IConstraint extends Comparable<ACQ_IConstraint> {
    /**
     * Returns the name of this constraint
     * 
     * @return name of this constraint
     */
    public String getName();
    
    /**
     * Returns the scope of this constraint
     * 
     * @return scope of this constraint
     */
    public ACQ_Scope getScope();
    
    /**
     * Returns the number of variables of this constraint (size of the scope)
     * 
     * @return size of the scope
     */
    public int getArity();
    
    /**
     * Get the set of numeric values of the example query 
     * 
     * @param query Positive or negative example
     * @return set of numeric values
     */
    public int[] getProjection(ACQ_Query query);
    
    /**
     * Checks if the constraint is violated for a given set of values
     * 
     * @param values set of values to check
     * @return false if the set of values violate this constraint
     */
    public boolean checker(int[] values);
    
    /**
     * <p>Returns the negation of this constraint. For instance, a constraint with "=" 
     * as operator will return a constraint with the same scope but "!=" as operator</p>
     * 
     * @return Negation of this constraint
     */
    public ACQ_IConstraint getNegation();
    
    /**
     * get equivalent constraints for the choco solver
     * 
     * @param model model of the solver
     * @param intVars scope of the constraint
     * @return a set of constraints
     */
    public Constraint[] getChocoConstraints(Model model, IntVar... intVars);
	    
    
  
	    
    /**
     * Add this constraint to the model and reify it with a boolean variable
     * 
     * @param model model of the solver
     * @param b a BoolVar to reify the constraint
     * @param intVars scope of the constraint
     */
    public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars);

    
    public IntVar[] getVariables(IntVar[] fullVarSet);

	public int[] getVariables();

	public abstract boolean check(ACQ_Query query);
	
	void setName(String name);

	public String getNegName();
	
	public String toSmtlib();
	
	public int getWeight();
}
