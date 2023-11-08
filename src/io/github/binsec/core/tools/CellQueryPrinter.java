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

package io.github.binsec.core.tools;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.binsec.core.acqvariable.CellType;
import io.github.binsec.core.learner.ACQ_Query;

public class CellQueryPrinter extends QueryPrinter {

	CellType[] types;
	boolean[] globals;
	
	public CellQueryPrinter(CellType[] types, boolean[] globals) {
		this.types = types;
		this.globals = globals;
	}
	
	@Override
	public String toString(ACQ_Query q) {
		ArrayList<String> res = new ArrayList<>();
		
		int[] values = q.getTuple();
		
		int typeindex = 0;
		int i = 0;
		while (i < values.length) {
			String ref = "_";
			if (this.globals[typeindex]) {
				ref = "@"+values[i];
				i++;
			}
			switch (types[typeindex]) {
			case PTR:
				String cell = "(" + ref + ", ";
				
				if (values[i] == 0) cell += "NL";
				else cell += "@"+values[i];
				i++;
				
				// Internally PreCA considers the string size with
				// the last '\0' but in the print we want the result 
				// of strlen i.e., PreCA size -1
				int mul = (values[i]-1) / 1000;
				int rem = (values[i]-1) % 1000;
				
				// Pretty print string size
				String v = "";
				String strmul = mul == 0 ? "" : mul == 1? "S" : mul+ " * S";
				if (rem == 0 && mul == 0) v = 0+"";
				else if (rem == 0) v = strmul;
				else v = strmul + (mul == 0 ? "" : " + ") + rem;
				cell += ", " + v + ")";
				
				res.add(cell);
				break;
			default:
				res.add("(" + ref + ", " + values[i] + ")");
				break;
			}
			i++;
			typeindex++;
		}
		
		return q.getScope() + "::[" + String.join(", ", res) + "]";
	}
}
