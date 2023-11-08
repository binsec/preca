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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.tools.Chrono;

public class CNF implements Iterable<Clause> {
	
	protected Set<Clause> clauses;
	
	public CNF() {
		clauses = new HashSet<Clause>();
	}

	
	public CNF clone() {
		CNF res = new CNF();
		for (Clause cl : clauses) {
			res.add(cl.clone());
		}
		return res;
	}
	
	public int size() {
		return clauses.size();
	}
	
	public void add(Clause clause) {
		clauses.add(clause);
	}
	
	public void addChecked(Clause clause) {
		Iterator<Clause> iter = clauses.iterator();
		while(iter.hasNext()) {
			Clause cl = iter.next();
			if (cl.subsumed(clause)) {
				return;
			}
			else if (clause.subsumed(cl)){
				iter.remove();
			}
		}
		clauses.add(clause);
	}
	
	public Boolean isMonomial() {
		for (Clause cl : clauses) {
			int size = cl.getSize();
			assert(size > 0);
			if (size > 1) {
				return false;
			}
		}
		return true;
	}
	
	public Clause getUnmarkedNonUnaryClauseMaxWeight() {
		Clause res = null;
		Integer maxweight = null; 
		for (Clause cl : clauses) {
			if (cl.isMarked() == false && cl.getSize()>1) {
				int clweight = cl.getWeight();
				if (maxweight == null || clweight > maxweight) {
					res = cl;
					maxweight = clweight;
				}
			}
		}
		if (res != null) return res;
		
		if (this.allMarked()) {
			this.unmarkAll();
			return this.getUnmarkedNonUnaryClause();
		} 
		else {
			return new Clause(); // return empty clause
		}
	}
	
	public Clause getUnmarkedNonUnaryClause() {
		for (Clause cl : clauses) {
			if (cl.isMarked() == false && cl.getSize()>1) {
				return cl;
			}
		}
		if (this.allMarked()) {
			this.unmarkAll();
			return this.getUnmarkedNonUnaryClause();
		} 
		else {
			return new Clause(); // return empty clause
		}
	}
	
	
	
	public void concat(CNF cnf) {
		for (Clause cl : cnf) {
			this.add(cl);
		}
	}
	
	public void removeIfExists(Clause clause) {
		clauses.remove(clause);
	}
	
	public void remove(Clause clause) {
		boolean removed = clauses.remove(clause);
		assert removed : "Nothing to remove";
	}
	
	public boolean contains(Clause clause) {
		return clauses.contains(clause);
	}
	
	public ACQ_IConstraint[] getMonomialPositive() {
		assert(this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (Clause cl : clauses) {
			Unit unit = cl.get(0);
			if(!unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i =0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}
	
	public ACQ_IConstraint[] getMonomialNegative() {
		assert(this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (Clause cl : clauses) {
			Unit unit = cl.get(0);
			if(unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i =0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}
	
	public String toString() {
		String s = "";
		for(Clause cl : clauses) {
			String superscript = "";
			if (cl.isMarked()) {
				superscript = "\u207A";
			}
			if(s.length() == 0)
				s += "(" + cl.toString() + ")" + superscript;
			else
				s += " and (" + cl.toString() + ")" + superscript;
		}
		return s;
	}
	
	public void unitPropagate(Unit unit, Chrono chrono) {
		chrono.start("unit_propagate");
		
		if(!this.isMonomial()) {
			ArrayList<Unit> forcedlist = new ArrayList<Unit>();
			forcedlist.add(unit);
			
			for (int i = 0; i < forcedlist.size(); i++) {
				Unit forced = forcedlist.get(i); 
				boolean change = false;
				
				CNF toadd = new CNF();
				Iterator<Clause> iter = clauses.iterator();
				while(iter.hasNext()) {
					Clause cl = iter.next();
					if(cl.getSize() > 1) {
						Clause newcl = cl.clone();
						
						if(cl.contains(forced)) {
							iter.remove(); // remove cl
							newcl = null;
							break;
						}
						
						if (newcl.remove((Unit u) -> {
							if(forced.equalsConstraint(u) && forced.isNeg() != u.isNeg()) {
								return true;
							}
							else return false;
						}) == true) {
							change = true;
							if (newcl.getSize() == 1) {
								if (!newcl.get(0).isNeg()) chrono.stop("first_constr_learned");
								forcedlist.add(newcl.get(0));
							}
							
						}
						
						if (newcl != null & change) {
							iter.remove();
							newcl.unmark();
							toadd.add(newcl);
						}
					}
				}
				concat(toadd);
			}
			
			
		}
			
		chrono.stop("unit_propagate");
		
	}

	@Override
	public Iterator<Clause> iterator() {
		return clauses.iterator();
	}
	
	public boolean allMarked() {
		for (Clause cl : clauses) {
			if (cl.getSize() > 1 && !cl.isMarked()) {
				return false;
			}
		}
		return true;
	}
	
	public void unmarkAll() {
		for (Clause cl : clauses) {
			if (cl.getSize() > 1) {
				cl.unmark();
			}
		}
	}
	
	public ACQ_Query getInconsistency() {
		assert this.isMonomial();
		for (Clause cl : clauses) {
			if (cl.getOriginQuery() == null) continue; // May append in IrredundantQuery
			Unit unit = cl.get(0);
			if (!unit.isNeg()) {
				for (Clause cl2 : clauses) {
					Unit unit2 = cl2.get(0);
					if (unit2.isNeg() && 
							unit.getConstraint().equals(unit2.getConstraint())) {
						return cl.getOriginQuery();
					}
				}
			}
		}
		assert false;
		return null;
	}
	
}
