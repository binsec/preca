/******************************************************************************/
/*  This file is part of PRECA.                                               */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.    */
/*                                                                            */
/*  Copyright (C) 2019-2026                                                   */
/*  CEA (Commissariat à l'énergie atomique et aux énergies                    */
/*       alternatives)                                                        */
/*                                                                            */
/*  you can redistribute it and/or modify it under the terms of the GNU       */
/*  Lesser General Public License as published by the Free Software           */
/*  Foundation, version 2.1.                                                  */
/*                                                                            */
/*  It is distributed in the hope that it will be useful,                     */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of            */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             */
/*  GNU Lesser General Public License for more details.                       */
/*                                                                            */
/*  See the GNU Lesser General Public License version 2.1                     */
/*  for more details (enclosed in the file licenses/LGPLv2.1).                */
/*                                                                            */
/******************************************************************************/

package io.github.binsec.core.acqsolver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.maxsat.WeightedMaxSatDecorator;


import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Clause;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.RevConstraintMapping;
import io.github.binsec.core.acqconstraint.Unit;

public class MiniSatSolver extends SATSolver {

	RevConstraintMapping revmapping;
	int timeout;
	int nvars = 0;
	boolean timeoutReached = false;
	
	public MiniSatSolver() {
		revmapping = new RevConstraintMapping();
	}
	
	protected VecInt toMiniSatClause(Clause cl) {
		int[] minisatcl = new int[cl.getSize()];
		for (int i = 0; i < cl.getSize(); i++) {
			Unit unit = cl.get(i);
			minisatcl[i] = unit.toMiniSat();
			if (!unit.isNeg()) {
				revmapping.add(minisatcl[i], unit.clone());
			}
			else {
				assert minisatcl[i] < 0;
				Unit tmp = unit.clone();
				tmp.unsetNeg();
				revmapping.add(-minisatcl[i], tmp);
				
			}
		}
		return new VecInt(minisatcl);
	}
	
	@Override
	public SATModel solve(CNF T) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		ISolver solv = SolverFactory.newDefault();
		solv.setTimeout(this.timeout);
		
		solv.newVar(this.nvars);
		
		SATModel res = null;
		
		try {
			
			for (Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				solv.addClause(toMiniSatClause(cl));	
			}
			
			IProblem problem = solv;
			
			fireSolverEvent("BEG_TIMECOUNT", false, true);
			boolean sat = problem.isSatisfiable();
			fireSolverEvent("END_TIMECOUNT", true, false);
			
			if (sat) {
				ArrayList<Integer> model = convert(problem.model());
				res = new MiniSatModel(model, revmapping);
			}
		} 
		catch (ContradictionException e) {/* the constraint network is not satisfiable */} 
		catch (TimeoutException e) {
			this.timeoutReached = true;
		}
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}

	@Override
	public SATModel solve(Formula F) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		IPBSolver solv = SolverFactory.newDefault();
		solv.setTimeout(this.timeout);
		
		SATModel res = null;
		
		try {
			for (CNF T : F.getCnfs()) {
				for (Clause cl : T) {		
					solv.addClause(toMiniSatClause(cl));
				}
			}
			if (F.hasAtLeastAtMost()) {
				VecInt minisatclause = toMiniSatClause(F.getAtLeastAtMost());
				solv.addAtLeast(minisatclause, F.atLeastLower());
				solv.addAtMost(minisatclause, F.atMostUpper());
			}
				
			IProblem problem = solv;
			fireSolverEvent("BEG_TIMECOUNT", false, true);
			boolean sat = problem.isSatisfiable();
			fireSolverEvent("END_TIMECOUNT", true, false);
			if (sat) {	
				ArrayList<Integer> model = convert(problem.model());
				res = new MiniSatModel(model, revmapping);
			}
		} 
		catch (ContradictionException e) {/* the constraint network is not satisfiable */} 
		catch (TimeoutException e) {
			this.timeoutReached = true;
		}
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}
	
	@Override
	public SATModel maxsat(Formula Fhard, Formula Fsoft, Formula Fsofter) {
	    assert false;
	    return null;
	}
	
	protected ArrayList<Integer> convert(int[] l) {
		// returns the list of all positive literal (corresponding to a real constraint)
		ArrayList<Integer> res = new ArrayList<>();
		for (int i : l) {
			if (i > 0)	res.add(i);
		}
		return res;
	}

	@Override
	public void setVars() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setLimit(Long timeout) {
		this.timeout = timeout.intValue();
		
	}

	@Override
	public Unit addVar(ACQ_IConstraint constr, String name) {
		this.nvars++;
		Unit unit = new Unit(constr, nvars, false);
		return unit;
	}

	@Override
	public Boolean isTimeoutReached() {
		return this.timeoutReached;
	}
	
	@Override
	public void reset() {
		revmapping = new RevConstraintMapping();
		nvars = 0;
		timeoutReached = false;
	}

	//test for maxsat using satsolver 
	@Override
	public SATModel solveMaxSat(Formula H, Formula S,ACQ_Network m,ConstraintMapping mapping) {

	    fireSolverEvent("BEG_maxsatsolve", false, true);

	    SATModel res = null;
	    Formula F = new Formula();
	    for (CNF T : H.getCnfs()) {
            F.addCnf(T);
        }
	    Clause cl = new Clause();
	    for(ACQ_IConstraint c : m.getConstraints()) {
        	Unit intC = mapping.get(c);
        	cl.add(intC);        	
        }
	    
        for(int i=m.size()-1; i>=0 ;i--) {
        	F.setAtLeastAtMost(cl, i, m.size());
        	SATModel model = solve(F);
        	if(model!= null) {
        		res = model;
        		break;
        	}
        }
        fireSolverEvent("END_maxsatsolve", true, false);
        return res;	    
	}

	@Override
	public SATModel solveConacq(Formula F) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SATModel maxsat(Formula Fhard, Formula Fsoft) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
