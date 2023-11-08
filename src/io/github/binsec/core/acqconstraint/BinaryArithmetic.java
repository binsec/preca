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
import io.github.binsec.core.tools.NameService;


/**
 *	Constraint between two variables. It can either be a straight comparison between these two
 *  For example : X >= Y or X != Y
 *  Or it can be an operation followed by a comparison using a constant
 *  For example : X + 5 <= Y or X - Y = 19
 *	
 * @author agutierr
 */
public class BinaryArithmetic extends BinaryConstraint {
	/**
	 * Operator(s) used into this constraint. It must have at least one operator, 
	 * the second is optional depending on the constraint we want to build.
	 */
	private final Operator op1, op2;

	// required visibility to allow exportation
	/**
	 * Optional constant value to allow more comparison for the constraint
	 */
	protected final int cste;

	private String negation;
	private int weight;

	/**
	 * Constructor for a constraint between two variables. 
	 * 
	 * @param name Name of this constraint
	 * @param var1 Variable of this constraint
	 * @param op Operator of this constraint
	 * @param var2 Variable of this constraint
	 */
	public BinaryArithmetic(String name, int var1, Operator op, int var2, String negation_name) {

		super(name, var1, var2);
		this.op1 = op;
		this.op2 = Operator.NONE;
		this.cste = -1; // not used
		this.negation= negation_name;

	}
	
	/**
	 * Constructor for a constraint between two variables. 
	 * 
	 * @param name Name of this constraint
	 * @param var1 Variable of this constraint
	 * @param op Operator of this constraint
	 * @param var2 Variable of this constraint
	 * @param weight Weight of the constraint (is it likely in the target network) 
	 */
	public BinaryArithmetic(String name, int var1, Operator op, int var2, String negation_name, int weight) {

		super(name, var1, var2);
		this.op1 = op;
		this.op2 = Operator.NONE;
		this.cste = -1; // not used
		this.negation= negation_name;
		this.weight = weight;
	}


	/**
	 * Constructor for a constraint involving two variables and a constant value
	 * 
	 * @param name Name of this constraint
	 * @param var1 Variable of this constraint
	 * @param op1 Operator of this constraint
	 * @param var2 Variable of this constraint 
	 * @param op2 Operator of this constraint
	 * @param cste Constant value for this constraint
	 */
	public BinaryArithmetic(String name, int var1, Operator op1, int var2, Operator op2, int cste, String negation_name) {
		super(name, var1, var2);
		this.op1 = op1;
		this.op2 = op2;
		this.cste = cste;
		this.negation= negation_name;

	}




	/**
	 * Checks if this constraint has two operators
	 * 
	 * @return true if this constraint has two operators
	 */
	private boolean hasOperation() {
		return op2 != Operator.NONE;
	}

	/**
	 * Returns a new BinaryArithmetic constraint which is the negation of this constraint
	 * By instance, a constraint with "=" as operator will return a new constraint with the 
	 * same variables but with "!=" as operator 
	 * 
	 * @return A new BinaryArithmetic constraint, negation of this constraint
	 */
	@Override
	public BinaryArithmetic getNegation() {

		String negationName= this.getNegName(); 
		if(negationName.equals("UNKNOWN"))
			negationName="not_" + getName();


		if (op1 == Operator.PL || op1 == Operator.MN || op1 == Operator.Dist) {

			if(op1 == Operator.Dist && negationName.equals("AT_LE"))
				return new BinaryArithmetic(negationName, variables[0], op1, variables[1], Operator.LT,cste+1, "AT_GT");

			if(op1 == Operator.Dist && negationName.equals("AT_GE"))
				return new BinaryArithmetic(negationName, variables[0], op1, variables[1], Operator.GT,cste-1, "AT_LT");

			if(op1 == Operator.Dist && negationName.equals("AT_LT"))
				return new BinaryArithmetic(negationName, variables[0], op1, variables[1], Operator.LT, cste, "AT_GE");

			if(op1 == Operator.Dist && negationName.equals("AT_GT"))
				return new BinaryArithmetic(negationName, variables[0], op1, variables[1], Operator.GT, cste, "AT_LE");

			return new BinaryArithmetic(negationName, variables[0], op1, variables[1], Operator.getOpposite(op2), cste, this.negation);
		} else  {
			return new BinaryArithmetic(negationName, variables[0], Operator.getOpposite(op1), variables[1], op2, cste, getName());
		}

	}

	public String getNegName() {
		// TODO Auto-generated method stub
		return negation;
	}

	/**
	 * get the constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint cst;
		IntVar l=null;
		IntVar r=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(this.getVariables()[0])))
					l= v;
			if(v.getName().equals(NameService.getVarName(this.getVariables()[1])))
				r= v;
		}
		if (hasOperation()) {

			if(op1== Operator.Dist) {
				if(this.getVariables().length==intVars.length)
					cst=model.distance(l, r, op2.toString(), cste);
				else
					cst=model.distance(l, r, op2.toString(), cste);
			}else {
				if(this.getVariables().length==intVars.length)
					cst=model.arithm(l, op1.toString(), r, op2.toString(), cste);
				else
					cst=model.arithm(l, op1.toString(), r, op2.toString(), cste);
			}
		} else {
			if(this.getVariables().length==intVars.length)
				cst=model.arithm(l, op1.toString(), r);
			else	{
				cst=model.arithm(l, op1.toString(),r);

			}
		}
		return new Constraint[]{cst};
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cste;
		result = prime * result + ((op1 == null) ? 0 : op1.hashCode());
		result = prime * result + ((op2 == null) ? 0 : op2.hashCode());
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
		BinaryArithmetic other = (BinaryArithmetic) obj;
		if (cste != other.cste)
			return false;
		if (op1 != other.op1)
			return false;
		if (op2 != other.op2)
			return false;
		if (this.getVariables()[0] != other.getVariables()[0])
			return false;
		if (this.getVariables()[1] != other.getVariables()[1])
			return false;

		return true;
	}

	/**
	 * Add this constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public void toReifiedChoco(Model model, BoolVar b,IntVar... intVars) {
				int left=0, right=1;
		if(intVars.length>=this.getVariables().length)
		{
			left= this.getVariables()[0];
			right=this.getVariables()[1];
		}
		IntVar l=null;
		IntVar r=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(left)))
					l= v;
			if(v.getName().equals(NameService.getVarName(right)))
				r= v;
		}
		if (hasOperation()) {
			if(op1==Operator.Dist) 
				
				model.distance(l, r, op2.toString(), cste).reifyWith(b);
			
				else
				model.arithm(l, op1.toString(), r, op2.toString(), cste).reifyWith(b);
		} else {
			model.arithm(intVars[this.getVariables()[0]], 
					op1.toString(), 
					intVars[this.getVariables()[1]]).reifyWith(b);
		}

	}
	/**
	 * Checks this constraint for a specified set of values
	 * 
	 * @param value1 Value of the first variable of this constraint
	 * @param value2 Value of the second variable of this constraint
	 * @return true if this constraint is satisfied for the specified set of values
	 */
	@Override
	protected boolean check(int value1, int value2) {
		int val1, val2;
		Operator op;


		if(this.getName().equals("InDiag1"))
			return check_InDiag1(value1, value2);

		if(this.getName().equals("InDiag2"))
			return check_InDiag2(value1, value2);

		if(this.getName().equals("OutDiag1"))
			return !check_InDiag1(value1, value2);

		if(this.getName().equals("OutDiag2"))
			return !check_InDiag2(value1, value2);


		if(op2 == Operator.NONE) {
			val1 = value1;
			val2 = value2;
			op = op1;
		}
		else {
			if (isArithmOperation(op1)) {

				if (op1 == Operator.Dist) {
					val1 = Math.abs(value1 - value2);
				}else 
					if (op1 == Operator.PL) {
						val1 = value1 + value2;
					} else {
						val1 = value1 - value2;
					}
				val2 = cste;
				op = op2;
			} else {
				if (op2 == Operator.PL) {
					val2 = value2 + cste;
				} else {
					val2 = value2 - cste;
				}
				val1 = value1;
				op = op1;
			}
		}
		switch (op) {
		case EQ:
			return val1 == val2;
		case NQ:
			return val1 != val2;
		case GT:
			return val1 > val2;
		case GE:
			return val1 >= val2;
		case LT:
			return val1 < val2;
		case LE:
			return val1 <= val2;
		default:
			assert false: "Operator not handled";
			break;

		}
		return false;
	}


	private boolean check_InDiag1(int value1, int value2) {

		return value1+this.getVariables()[0]==value2+this.getVariables()[1];
	}

	private boolean check_InDiag2(int value1, int value2) {

		return value1-this.getVariables()[0]==value2-this.getVariables()[1];
	}

	/**
	 * Checks if the specified operator is an operation operator
	 * 
	 * @param operator Operator to verify
	 * @return true if the specified operator is Operator.PL or Operator.MN
	 */
	private static boolean isArithmOperation(Operator operator) {
		return operator.equals(Operator.PL) || operator.equals(Operator.MN) || operator.equals(Operator.Dist);
	}


	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}

	@Override
	public String toSmtlib() {
		assert op2 == Operator.NONE;
		String var1 = "x" + variables[0];
		String var2 = "x" + variables[1];
		switch (op1) {
		case EQ:
			return "(= " + var1 + " " + var2 + ")";
		case NQ:
			return "(distinct " + var1 + " " + var2 + ")";
		case GT:
			return "(> " + var1 + " " + var2 + ")";
		case GE:
			return "(>= " + var1 + " " + var2 + ")";
		case LT:
			return "(< " + var1 + " " + var2 + ")";
		case LE:
			return "(<= " + var1 + " " + var2 + ")";
		default:
			assert false : "unknown operator";

		}
		return null;
	}

	@Override
	public int getWeight() {
		return weight;
	}

}
