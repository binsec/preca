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
import java.util.function.Function;

import io.github.binsec.core.learner.ACQ_Query;

public class Clause implements Iterable<Unit> {
	
	protected ArrayList<Unit> units;
	protected Boolean marked = false;
	protected int weight = 0;
	protected ACQ_Query originQuery;
	
	public Clause() {
		units = new ArrayList<Unit>();
	}
	
	public Clause(Unit unit) {
		units = new ArrayList<Unit>();
		units.add(unit);
		weight += unit.getConstraint().getWeight();
	}
	
	public Clause clone() {
		Clause res = new Clause();
		res.setOriginQuery(getOriginQuery());
		if(marked) {
			res.mark();
		}
		for (Unit unit : units) {
			res.add(unit.clone());
		}
		assert(res.getSize() == this.getSize());
		return res;
	}
	
	public void add(Unit unit) {
		assert(unit != null);
		units.add(unit);
		weight += unit.getConstraint().getWeight();
	}
	
	public Unit get(int index) {
		return units.get(index);
	}
	
	public int getNvars() {
		ArrayList<Unit> l = new ArrayList<>();
		for (Unit u : units) {
			if (!l.contains(u)) {
				l.add(u);
			}
		}
		return l.size();
	}
	
	public Boolean isMarked() {
		return marked;
	}
	
	public void mark() {
		marked = true;
	}
	
	public void unmark() {
		marked = false;
	}
	
	public int getSize() {
		return units.size();
	}
	
	public Boolean isEmpty() {
		return this.getSize() == 0;
	}
	
	public boolean remove(Function<Unit, Boolean> foo) {
		boolean res = false;
		for (int i=0; i< units.size(); i++) {
			if (foo.apply(units.get(i))) {
				Unit rem = units.remove(i);
				weight -= rem.getConstraint().getWeight();
				res = true;
			}
		}
		return res;
	}
	
	public Boolean contains(Function<Unit, Boolean> foo) {
		for (int i=0; i< units.size(); i++) {
			if (foo.apply(units.get(i))) {
				return true;
			}
		}
		return false;
	}
	
	public Boolean contains(ACQ_IConstraint constr) {
		for (Unit unit : units) {
			if (unit.equalsConstraint(constr)) {
				return true;
			}
		}
		return false;
	}
	
	public Boolean containsConstraint(Unit unit) {
		for (Unit u : units) {
			if (unit.equalsConstraint(u)) {
				return true;
			}
		}
		return false;
	}
	
	
	public Boolean contains(Unit unit) {
		for (Unit u : units) {
			if (unit.equals(u)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String s = "";
		for (Unit unit : units) {
			if (s.length() == 0)
				s += unit.toString();
			else
				s += " or " + unit.toString();
				
		}
		return s;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (Unit unit : units) {
			result = prime * result + unit.hashCode();
		}
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
		Clause clause = (Clause) obj;
		//System.out.println("In Clause.equals()");
		Clause tmp = clause.clone();
		for(Unit unit : units) {
			if(tmp.contains(unit)) {
				assert	tmp.remove((Unit u) -> {
					if(u.equals(unit)) return true; else return false;
				});
			}
		}
		return tmp.isEmpty();
	}

	public boolean subsumed(Clause cl) {
		for (Unit unit : units) {
			if (!cl.contains(unit)) return false;
		}
		return false;
	}
	
	@Override
	public Iterator<Unit> iterator() {
		return units.iterator();
	}
	
	public int getWeight() {
		return this.weight;
	}
	
	public void setOriginQuery(ACQ_Query e) {
		originQuery = e;
	}
	
	public ACQ_Query getOriginQuery() {
		return originQuery;
	}
}
