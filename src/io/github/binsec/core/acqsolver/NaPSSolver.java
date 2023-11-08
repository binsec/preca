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

package io.github.binsec.core.acqsolver;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Clause;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.RevConstraintMapping;
import io.github.binsec.core.acqconstraint.Unit;

public class NaPSSolver extends SATSolver {

	
	RevConstraintMapping revmapping;
	int timeout;
	int nvars = 0;
	boolean timeoutReached = false;
	Runtime runtime;
	String torun;
	
	public NaPSSolver() {
		revmapping = new RevConstraintMapping();
		runtime = Runtime.getRuntime();
		torun = String.format("%s/resources/naps-1.02b", System.getenv("PRECA_PATH"));
	}
	
	String toNaPS(Clause cl) {
		String res = "";
		for (Unit u : cl) {
			String name = u.toNaPS();
			res += "1 " + name + " ";
			
			if (!u.isNeg()) {
				revmapping.add(u.toMiniSat(), u.clone());
			}
			else {
				assert u.toMiniSat() < 0;
				Unit tmp = u.clone();
				tmp.unsetNeg();
				revmapping.add(-u.toMiniSat(), tmp);
				
			}
		}
		res += " > 0;\n";
		return res;
	}
	
	String toNaPS(Clause cl, int upper, int lower) {
		String supper = "";
		String slower = "";
		for (Unit u : cl) {
			String name = u.toNaPS();
			supper += "1 " + name + " ";
			slower += "1 " + name + " ";
			
			if (!u.isNeg()) {
				revmapping.add(u.toMiniSat(), u.clone());
			}
			else {
				assert u.toMiniSat() < 0;
				Unit tmp = u.clone();
				tmp.unsetNeg();
				revmapping.add(-u.toMiniSat(), tmp);
				
			}
			
		}
		supper += " <= " + upper + ";\n";
		slower += " >= " + lower + ";\n";
		return supper + slower;
	}
	
	String toNaPSMinimization(Set<Clause> minimizations) {
		String res = "min:";
		boolean shouldminimize = false; // if all weight equal 1 no minimization
		
		for (Clause cl : minimizations) {
			for (Unit u : cl) {
				int weight = -u.getConstraint().getWeight();
				if (weight != -1) {
					shouldminimize = true;
				}
				res += " " + weight + " " + u.toNaPS();
			}
		}
		return shouldminimize ? res + ";\n" : "";
	}
	
	@Override
	public SATModel solve(CNF T) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		String constraints = "";
		
		SATModel res = null;
			
		for (Clause cl : T) {
			assert cl.getSize() > 0 : "empty clause";
			constraints += toNaPS(cl);	
		}
		
		fireSolverEvent("BEG_TIMECOUNT", false, true);
		res = NaPSSolve(constraints);
		fireSolverEvent("END_TIMECOUNT", true, false);
			
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}

	@Override
	public SATModel solve(Formula F) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		String constraints = toNaPSMinimization(F.getMinimizations());
		
		SATModel res = null;
			
		
		for (CNF T : F.getCnfs()) {
			for (Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				constraints += toNaPS(cl);
			}
		}
		
		if (F.hasAtLeastAtMost()) {
			constraints += toNaPS(F.getAtLeastAtMost(), F.atMostUpper(), F.atLeastLower());
		}
		
		fireSolverEvent("BEG_TIMECOUNT", false, true);
		res = NaPSSolve(constraints);
		fireSolverEvent("END_TIMECOUNT", true, false);
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}
	
	protected NaPSModel NaPSSolve(String constraints) {
		
		NaPSModel res = null;
		
		// call NaPS
		
		FileWriter myWriter;
		String filename = "/tmp/naps_" +  ProcessHandle.current().pid() +".txt";
	    try {
	    	myWriter = new FileWriter(filename);
			myWriter.write(constraints);
			myWriter.close();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		
		Process proc;
		boolean unsat = false;
		
		try {
			
			proc = runtime.exec(new String[] { torun, filename});
			
			BufferedReader stdInput = new BufferedReader(new 
			InputStreamReader(proc.getInputStream()));


			// Read the output from the command
			String line = null;
			ArrayList<Integer> values = new ArrayList<>();
			
			while ((line = stdInput.readLine()) != null) {
				if (line.startsWith("s")) {
					if (line.startsWith("s UNSATISFIABLE")) {
						unsat = true;
						break;
					}
				}
				if (line.startsWith("v")) {
					values.addAll(convert(line));
				}
			}
			
			proc.waitFor();
			
			if (unsat) {
				res = null;
			}
			else {
				res = new NaPSModel(values, revmapping);
			}
			Files.deleteIfExists(Paths.get(filename));
			
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return res;
	}
	
	protected ArrayList<Integer> convert(String resline) {
		// returns the list of all positive literal (corresponding to a real constraint)
		ArrayList<Integer> res = new ArrayList<>();
		
		if (resline.equals("v")) {
			return res;
		}
		
		for (String val : resline.substring(2).split(" ")) {
			assert val.startsWith("-x") || val.startsWith("x");
			if (!val.startsWith("-")) {
				res.add(Integer.parseInt(val.substring(1))); // values are x1
			}
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
	
}
