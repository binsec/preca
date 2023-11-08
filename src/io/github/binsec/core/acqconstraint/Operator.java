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

import java.util.HashMap;

public enum Operator {

    NONE(), EQ(), LT(), GT(), NQ(), LE(), GE(), PL(), MN(), Dist();

	private static HashMap<String, Operator> operators = new HashMap<>();

    static {
        operators.put("@", Operator.NONE);
        operators.put("=", Operator.EQ);
        operators.put(">", Operator.GT);
        operators.put(">=", Operator.GE);
        operators.put("<", Operator.LT);
        operators.put("<=", Operator.LE);
        operators.put("!=", Operator.NQ);
        operators.put("+", Operator.PL);
        operators.put("-", Operator.MN);
        operators.put("abs", Operator.Dist);
    }

	public static Operator get(String name) {
        return operators.get(name);
	}

	@Override
	public String toString() {
		switch (this){
			case LT:return "<";
			case GT:return ">";
			case LE:return "<=";
			case GE:return ">=";
			case NQ:return "!=";
			case EQ:return "=";
			case PL:return "+";
			case MN:return "-";
			case Dist:return "dist";

			default:throw new UnsupportedOperationException();
		}
	}

	/**
	 * Flips the direction of an inequality
	 * @param operator op to flip
	 */
	public static String getFlip(String operator) {
		switch (get(operator)){
			case LT:return ">";
			case GT:return "<";
			case LE:return ">=";
			case GE:return "<=";
			default:return operator;
		}
	}

	public static Operator getOpposite(Operator operator) {
		switch (operator){
			case LT:return GE;
			case GT:return LE;
			case LE:return GT;
			case GE:return LT;
			case NQ:return EQ;
			case EQ:return NQ;
			case PL:return PL;			//NL: neutral negation on PL and MN
			case MN:return MN;
			default:throw new UnsupportedOperationException();
		}
	}
	
	public static Operator getOperator(String s) {
		System.out.println(s);
		switch(s) {
		case("EqualXY"):
			return Operator.EQ;
		case("DiffXY"):
			return Operator.NQ;
		case("GreaterOrEqualXY"):
			return Operator.GE;
		case("GreaterXY"):
			return Operator.GT;
		case("LessOrEqualXY"):
			return Operator.LE;
		case("LessXY"):
			return Operator.LT;
		case("EqualX"):
			return Operator.EQ;
		case("DiffX"):
			return Operator.NQ;
		case("GreaterOrEqualX"):
			return Operator.GE;
		case("GreaterX"):
			return Operator.GT;
		case("LessOrEqualX"):
			return Operator.LE;
		case("LessX"):
			return Operator.LT;
		default:
			return getOperatorUnary(s);
		}
	}
	
	public static Operator getOperatorUnary(String s) {
		if(s.matches("^DiffX.*"))
			return Operator.NQ;
		else if(s.matches("^EqualX.*"))
			return Operator.EQ;
		else if(s.matches("^GreaterOrEqualX.*"))
			return Operator.GE;
		else if(s.matches("^GreaterX.*"))
			return Operator.GT;
		else if(s.matches("^LessOrEqualX.*"))
			return Operator.LE;
		else if(s.matches("^LessX.*"))
			return Operator.LT;
		throw new UnsupportedOperationException();
	}
	
	public static String getName(Operator op) {
		switch (op){
		case LT:return "LessXY";
		case GT:return "GreaterXY";
		case LE:return "LessEqualXY";
		case GE:return "GreaterEqualXY";
		case NQ:return "DifferentXY";
		case EQ:return "EqualXY";
		case PL:return null; //TODO
		case MN:return null; // TODO
		default:throw new UnsupportedOperationException();
	}
	}
}
