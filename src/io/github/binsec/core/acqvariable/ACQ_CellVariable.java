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

package io.github.binsec.core.acqvariable;

import java.util.Collections;
import java.util.HashSet;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class ACQ_CellVariable {
	static int nextvarid = 0; // next id for new variables
	static int nextrefaddress = 1; // This is also the number of global variables + 1
	static int numberptrcells = 0;
	static int numberintcells = 0; // invariant nextrefaddress <= numberptrcells + numberintcells
	static int numberrefs = 0;
	static int nextptrcellid = 0;
	
	public int cellid; // the cell id (independently from global or not) printed in smtlib
	int ptrcellid; // the cell id when taking into account only pointer cells  (invariant: globalcellid <= cellid)
	boolean global;
	ACQ_RefVariable ref = null;
	ACQ_ValueVariable vv;
	ACQ_ValueVariable size = null;
	
	static HashSet<Integer> addintconstants = new HashSet<>(); 
	static HashSet<Integer> adduintconstants = new HashSet<>();
	static boolean bigString = false;
	
	public ACQ_CellVariable(CellType vtype, boolean global) {
		this.cellid = getNbCells();
		
		this.global = global;
		
		if (global) {
			// If not global (i.e. parameter), then we have no constraint over the reference to the parameter
			this.ref = new ACQ_RefVariable(nextvarid, nextrefaddress);
			nextrefaddress++;
			nextvarid++;
			numberrefs++;
			
		}
		
		
		this.vv = new ACQ_ValueVariable(nextvarid, vtype);
		nextvarid++;
		
		// Depending on the type of the cell, increase the appropriate upper bound
		if (vtype == CellType.PTR) {
			// Add variable to represent the size of the pointed data structure
			this.size = new ACQ_ValueVariable(nextvarid, CellType.UINT);
			nextvarid++;
			
			ptrcellid = nextptrcellid;
			nextptrcellid++;
			numberptrcells++;
		}
		else if (vtype == CellType.INT || vtype == CellType.UINT) 
			numberintcells++;
		else 
			assert false: "Unknown type";
		
	}
	
	public int getNbCells() {
		return numberptrcells + numberintcells;
	}
	
	public boolean isGlobal() {
		assert this.global == (this.ref != null); 
		return this.global;
	}
	
	public ACQ_RefVariable getRef() {
		return this.ref;
	}
	
	public ACQ_ValueVariable getValue() {
		return this.vv;
	}
	
	public ACQ_ValueVariable getSize() {
		return this.size;
	}
	
	public IntVar getChocoRef(Model model) {
		return this.ref.getChocoVar(model, 1, numberptrcells+numberrefs + 5);
	}
	
	public IntVar getChocoValue(Model model) {
		if (getType() == CellType.PTR) {
			// We want that all cells can be valid, without dereferencing each other nor dereferencing
			// itself and aliasing with no other value
			//return this.vv.getChocoVar(model, 0, nextrefaddress + ptrcellid);
			return this.vv.getChocoVar(model, 0, numberptrcells+numberrefs + 5);
		}
		else if (getType() == CellType.INT) {
			int min = addintconstants.size() == 0 ? 0 : Collections.min(addintconstants);
			int max = addintconstants.size() == 0 ? 0 : Collections.max(addintconstants);
			int lower = Math.min(min, -(numberintcells +1));
			int upper = Math.max(max, numberintcells +1);
			return this.vv.getChocoVar(model, lower, upper);
			
		}
		else if (getType() == CellType.UINT) {
			int max = adduintconstants.size() == 0 ? 0 : Collections.max(adduintconstants);
			int upper = Math.max(max, numberintcells +1);
			return this.vv.getChocoVar(model, 0, upper);
		}
		else {
			System.err.println("Unknown type");
			System.exit(1);
			return null;
		}
	}
	
	public void addValue(int i) {
		if (getType() == CellType.INT) {
			ACQ_CellVariable.addintconstants.add(i);
		}
		else if (getType() == CellType.UINT) {
			if (i >= 0)	ACQ_CellVariable.adduintconstants.add(i);
		}
		else assert false;
	}
	
	public IntVar getChocoSize(Model model) {
		int max = bigString ? 1000*(numberptrcells+numberrefs) : 1;
		return this.size.getChocoVar(model, 1, max);
		//return this.size.getChocoVar(model, 1, 5);
	}
	
	public void setBigString() {
		ACQ_CellVariable.bigString = true;
	}
	
	public CellType getType() {
		return this.vv.getType();
	}
	
	public Integer[] rand() {
		Integer res[] = new Integer[3];
		if (this.ref == null) {
			res[0] = null;
		}
		else {
			res[0] = this.ref.rand();
 		}
		int lower;
		int upper;
		if (getType() == CellType.PTR) {
			// We want that all cells can be valid, without dereferencing each other nor dereferencing
			// itself and aliasing with no other value
			lower =  0;
			upper = nextrefaddress + ptrcellid;
		}
		else if (getType() == CellType.INT) {
			lower = -(numberintcells +1);
			upper = numberintcells +1;
		}
		else if (getType() == CellType.UINT) {
			lower = 0;
			upper = numberintcells +1;
		}
		else {
			System.err.println("Unknown type");
			System.exit(1);
			return null;
		}
		
		res[1] = this.vv.rand(lower, upper);
		
		// add size (if ptr = NULL, then size equals 1) 
		if (getType() != CellType.PTR)
			res[2] = null;
		else {
			if (res[1] == 0) {
				res[2] = 1;
			}
			else {
				res[2] = this.size.rand(1, upper);
			}
		}
		return res;
	}
	
	public String toString() {
		String ref = (this.ref != null) ? "&var" + this.cellid : "_";
		return "(" + ref + " -> " + "var" + this.cellid + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
		result = prime * result + ((vv == null) ? 0 : vv.hashCode());
		
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
		ACQ_CellVariable other = (ACQ_CellVariable) obj;
		
		if (this.isGlobal() != other.isGlobal()) {
			return false;
		}
		
		if (this.isGlobal()) {
			if (!this.ref.equals(other.ref)) {
				return false;
			}
		}
		
		if (!this.vv.equals(other.vv)) {
			return false;
		}

		return true;
	}
}
