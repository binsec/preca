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

import java.util.HashSet;
import java.util.Set;

public class Formula {
	
	protected Set<CNF> cnfs;
	protected Set<Clause> minimizations;
	protected Clause atLeastAtMost;
	protected int atLeastLower;
	protected int atMostUpper;
	
	
	public Formula() {
		cnfs = new HashSet<CNF>();
		minimizations = new HashSet<Clause>();
		atLeastAtMost = null;
	}
	
	public void addCnf(CNF cnf) {
		cnfs.add(cnf);
	}
	
	public void addClause(Clause cl) {
		CNF toadd = new CNF();
		toadd.add(cl);
		cnfs.add(toadd);
	}
	
	public void setAtLeastAtMost(Clause cl, int lower, int upper) {
		atLeastAtMost = cl;
		atLeastLower = lower;
		atMostUpper = upper;
	}
	
	public boolean hasAtLeastAtMost() {
		return atLeastAtMost != null;
	}
	
	public Clause getAtLeastAtMost() {
		return atLeastAtMost;
	}
	
	public int atLeastLower() {
		return atLeastLower;
	}
	
	public int atMostUpper() {
		return atMostUpper;
	}
	
	public String toString() {
		String s = "";
		for (CNF cnf : cnfs) {
			if (s.length() == 0)
				s += "[" + cnf.toString() + "]";
			else
				s += "\nand [" + cnf.toString() + "]";
		}
		
		if(hasAtLeastAtMost()) {
			if (s.length() == 0) {
				s += "atLeast("+ atLeastAtMost +", " + atLeastLower + ")";
				s += "\natMost("+ atLeastAtMost +", " + atMostUpper + ")";
			}
			else {
				s += "\natLeast("+ atLeastAtMost +", " + atLeastLower + ")";
				s += "\natMost("+ atLeastAtMost +", " + atMostUpper + ")";
			}
		}
		return s;
		
	}
	
	public Set<CNF> getCnfs(){
		return cnfs;
	}
	
	public void addMinimization(Clause cl) {
		minimizations.add(cl);
	}
	
	public Set<Clause> getMinimizations() {
		return minimizations;
	}
}
