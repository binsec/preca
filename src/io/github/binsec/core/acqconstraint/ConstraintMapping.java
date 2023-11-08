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

public class ConstraintMapping {
	protected HashMap<ACQ_IConstraint, Unit> mapping;
	
	public ConstraintMapping() {
		mapping = new HashMap<ACQ_IConstraint, Unit>();
	}
	
	public void add(ACQ_IConstraint constr, Unit unit) {
		assert(!unit.isNeg());
		Unit u = mapping.put(constr, unit);
		assert u == null: "mapping where already containing an entry for constr"; 
	}
	
	public Unit get(ACQ_IConstraint constr) {
		Unit res = mapping.get(constr);
		assert(!res.isNeg());
		return res;
	}
	
	public Iterable<Unit> values(){
		return mapping.values();
	}
	
	public int size() {
		return mapping.size();
	}
}
