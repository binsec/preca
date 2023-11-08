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

public class RevConstraintMapping {
	protected HashMap<Integer, Unit> mapping;
	
	public RevConstraintMapping() {
		this.mapping = new HashMap<Integer, Unit>();
	}
	
	public void add(Integer i, Unit unit) {
		assert i > 0;
		assert(!unit.isNeg());
		this.mapping.put(i, unit);
	}
	
	public Unit get(Integer i) {
		assert i > 0;
		Unit res = this.mapping.get(i);
		assert(!res.isNeg());
		return res;
	}
	
	public Iterable<Unit> values(){
		return this.mapping.values();
	}
	
	public int size() {
		return this.mapping.size();
	}
}
