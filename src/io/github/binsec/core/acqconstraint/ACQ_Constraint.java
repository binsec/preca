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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.chocosolver.solver.variables.IntVar;

import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.ACQ_Scope;

/**
 * Abstract class that defines the functions common to all constraints
 * @author agutierr
 */
public abstract class ACQ_Constraint implements ACQ_IConstraint{
	/**
	 * Name of this constraint
	 */
    private  String name;
    /**
     * Variables of this constraint
     */
    
    final int[] variables;
    
    /**
     * Constraints
     */
    private ACQ_IConstraint cst1 ;
    private ACQ_IConstraint cst2 ;


    /**
     * Constructor of this constraint
     * 
     * @param name Name of this constraint
     * @param variables Variables of this constraint
     */
    public ACQ_Constraint(String name,int[] variables)
    {
        this.name=name;
        this.variables=variables;
    }
    public ACQ_Constraint(String name,Set<Integer> variables)
    {
        this.name=name;
        this.variables=new int[variables.size()];
        int i=0;
        for(Integer ii:variables)
        	this.variables[i++]=ii;	
    }
    /**
     * Constructor of this constraint
     * 
     * @param name Name of this constraint
     * @param Constraints 
     */
    public ACQ_Constraint(String name, ACQ_IConstraint cst1 ,ACQ_IConstraint cst2,int[] variables) {
		this.name=name;
		this.cst1=cst1;
		this.cst2=cst2;
        this.variables=variables;

	}
	/**
     * Returns a sub array of the specified IntVar array,
     * that only contains the variables involved into this constraint
     * 
     * @param fullVarSet All the variables of the solver model
     * @return A sub array containing the variables of this constraint
     * @author teddy
     */
    public IntVar[] getVariables(IntVar[] fullVarSet) {
    	ACQ_Scope scope = getScope();
    	ArrayList<IntVar> intVars = new ArrayList<>();
    	Iterator<Integer> iterator = scope.iterator();
    	while(iterator.hasNext()) {
    		intVars.add(fullVarSet[iterator.next()]);
    	}
    	return intVars.toArray(new IntVar[intVars.size()]);
    }
    
    /**
     * Returns the name of this constraint
     * 
     * @return name of this constraint
     */
    @Override
    public String getName(){
        return name;
    }
    
    @Override
    public void setName(String name){
         this.name=name;
    }
    /**
     * Returns the scope of this constraint
     * 
     * @return scope of this constraint
     */
    @Override
    public ACQ_Scope getScope() {
        return new ACQ_Scope(variables);
    }
    /**
     * Returns the number of variables of this constraint
     * 
     * @return number of variable of this constraint
     */
    @Override
    public int getArity() {
        return variables.length;
    }
    
    /**
     * Returns the number of variables of this constraint
     * 
     * @return number of variable of this constraint
     */
    @Override
    public int[] getVariables() {
        return variables;
    }
    
    /**
     * Get the numeric values of the example query
     * 
     * @param query Example positive or negative
     * @return numeric values of the example query
     */
    @Override
    public int[] getProjection(ACQ_Query query){
        int index=0;
        int [] values=new int[variables.length];
        for(int numvar:variables)
            values[index++]=query.getValue(numvar);
        return values;
    }
    /**
     * Checks if the constraint is violated for a given set of values
     * 
     * @param values set of values to check
     * @return false if the set of values violate this constraint
     */
    @Override
    public final boolean checker(int[] values){
        return check(values);
    }
    /**
     * Checks if the constraint is violated for a given set of values
     * 
     * @param value set of values to check
     * @return false if the set of values violate this constraint
     */
    public abstract boolean check(int... value);
    
    
    /**
     * Checks if the constraint is violated for a given query
     * 
     * @param the query
     * @return false if the query violates this constraint
     */
    public abstract boolean check(ACQ_Query query);
    
	@Override
	public String toString() {
		return name + Arrays.toString(variables);
	}
	
	public Operator getOperator() {
		String name =this.getName();
		if(name.contains("Different"))
			return Operator.NQ;
		else if(name.contains("Equal"))
			return Operator.EQ;
		else if(name.contains("LessEqual"))
			return Operator.LE;
		else if(name.contains("GreaterEqual"))
			return Operator.GE;
		else if(name.contains("Less"))
			return Operator.LT;
		else if(name.contains("Greater"))
			return Operator.GT;

		return Operator.NONE;
	}

	public int getWeight() {
		return 1;
	}
	
	@Override
    public int compareTo(ACQ_IConstraint other) {
        int otherweight = other.getWeight();
        /* For Ascending order*/
        return this.getWeight()-otherweight;
    }

}
