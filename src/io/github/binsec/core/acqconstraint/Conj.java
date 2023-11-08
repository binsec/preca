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

import java.util.ArrayList;
import java.util.Iterator;

public class Conj implements Iterable<Unit>{

	protected ArrayList<Unit> units;
	protected int size;
	protected Boolean marked = false; 
	
	public Conj() {
		units = new ArrayList<Unit>();
		size = 0;
	}
	
	public Conj(Iterable<Unit> iterator) {
		this(); // Call Conj() constructor
		for (Unit unit : iterator) {
			this.add(unit);
		}
	}
	
	public void add(Unit unit) {
		units.add(unit);
		size = size +1;
	}
	
	public String toString() {
		String s = "";
		for (Unit unit : units) {
			if(s.length() == 0)
				s += unit.toString();
			else
				s += " and " + unit.toString();
		}
		return s;
	}

	@Override
	public Iterator<Unit> iterator() {
		return units.iterator();
	}
	
}
