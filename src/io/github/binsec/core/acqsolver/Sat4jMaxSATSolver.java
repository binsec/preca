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

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.TimeoutException;

import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Clause;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.RevConstraintMapping;
import io.github.binsec.core.acqconstraint.Unit;

public class Sat4jMaxSATSolver extends SATSolver {

	
	RevConstraintMapping revmapping;
	int timeout;
	int nvars = 0;
	boolean timeoutReached = false;
	Runtime runtime;
	String torun;
	
	public Sat4jMaxSATSolver() {
		revmapping = new RevConstraintMapping();
		runtime = Runtime.getRuntime();
		torun = String.format("%s/resources/sat4j-maxsat.jar", System.getenv("PRECA_PATH"));
	}
	
	public String toCNF(Formula F) {
		
		int n = F.getClauses().size();
	   
	    StringBuilder clauses = new StringBuilder("p cnf " + nvars + " " + n + "\n");
	    for (CNF T : F.getCnfs()) {
			for (Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				
				
				for(Unit u : cl) {
					if(u.isNeg()) {
						Unit tmp = u.clone();
						tmp.unsetNeg();
						revmapping.add(tmp.getMiniSatVar(), tmp.clone());
						clauses.append(-u.getMiniSatVar()).append(" ");
					}
					else {
						revmapping.add(u.getMiniSatVar(), u.clone());
						clauses.append(u.getMiniSatVar()).append(" ");
					}
				}
				clauses.append("0\n");
			}
		}
	    
	    return clauses.toString();
	}
	public String toCNFSorted(Formula F) {
	    
	    List<Clause> allClauses = new ArrayList<>();
	    for (CNF T : F.getCnfs()) {
	        for (Clause cl : T) {
	            assert cl.getSize() > 0 : "empty clause";
	            allClauses.add(cl);
	        }
	    }
	    
	    allClauses.sort((cl1, cl2) -> {
	        int first1 = cl1.get(0).getMiniSatVar();
	        int first2 = cl2.get(0).getMiniSatVar();
	        return Integer.compare(first1, first2);
	    });
	    
	    int n = allClauses.size();
	    StringBuilder clauses = new StringBuilder("p cnf " + nvars + " " + n + "\n");
	    
	    for (Clause cl : allClauses) {
	        for (Unit u : cl) {
	            if (u.isNeg()) {
	                Unit tmp = u.clone();
	                tmp.unsetNeg();
	                revmapping.add(tmp.getMiniSatVar(), tmp.clone());
	                clauses.append(-u.getMiniSatVar()).append(" ");
	            } else {
	                revmapping.add(u.getMiniSatVar(), u.clone());
	                clauses.append(u.getMiniSatVar()).append(" ");
	            }
	        }
	        clauses.append("0\n");
	    }
	    
	    return clauses.toString();
	}
	
	
	@Override
	public SATModel solve(CNF T) {
		assert false;
		return null;
	}
	
	
	public SATModel solveConacq(Formula F) {
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
				ArrayList<Integer> model = convertConacq(problem.model());
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
	    SATModel res = null;
	    
	    String cnf = toCNFSorted(F);
	    // write cnf in the file
	    FileWriter myWriter;
		String filename = "/tmp/sat4jsat_" +  ProcessHandle.current().pid() +".cnf";
	    try {
	    	myWriter = new FileWriter(filename);
			myWriter.write(cnf);
			myWriter.close();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	   //System.err.println("CNF:\n" + cnf);
        
	    Process proc;
		boolean unsat = false;
				
		try {
			
			proc = runtime.exec(new String[] { "/usr/bin/java", "-jar",torun, filename});
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));


			// Read the output from the command
			String line = null;
			ArrayList<Integer> values = new ArrayList<>();
			//System.out.print(stdInput);
			while ((line = stdInput.readLine()) != null) {
				if (line.startsWith("c objective function")) {
					long objInt = Long.parseLong(line.split("=")[1]);
					if(objInt > 0) {
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
				res = new MiniSatModel(values, revmapping);
			}
			Files.deleteIfExists(Paths.get(filename));
			
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    

	    fireSolverEvent("END_satsolve", true, false);
	    return res;
	
	}
	
	protected ArrayList<Integer> convert(String resline) {
		// returns the list of all positive literal (corresponding to a real constraint)
		ArrayList<Integer> res = new ArrayList<>();
		
		if (resline.equals("v")) {
			return res;
		}
		
		for (String val : resline.substring(2).split(" ")) {
			if ((!val.startsWith("-")) && (!val.equals("0"))) {
				res.add(Integer.parseInt(val)); 
			}
		}
		return res;
	}
	
	protected ArrayList<Integer> convertConacq(int[] l) {
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
	

	public String toWCNF(Formula Fhard, Formula Fsoft, Formula Fsofter) {
		
		int n = Fsofter.getClauses().size();
	    int k = Fsoft.getClauses().size();
	    int h = Fhard.getClauses().size();
	    
	    
	    int nbClauses = h + k + n;
	    StringBuilder clauses = new StringBuilder("p wcnf " + nvars + " " + nbClauses + "\n");
	    for (CNF T : Fhard.getCnfs()) {
			for (Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				
				clauses.append(k*(n+1)+n+1);
				
				for(Unit u : cl) {
					if(u.isNeg()) {
						Unit tmp = u.clone();
						tmp.unsetNeg();
						revmapping.add(tmp.getMiniSatVar(), tmp.clone());
						clauses.append(" ").append(-u.getMiniSatVar());
					}
					else {
						revmapping.add(u.getMiniSatVar(), u.clone());
						clauses.append(" ").append(u.getMiniSatVar());
					}
				}
				clauses.append(" 0\n");
			}
		}
	    for(CNF T : Fsoft.getCnfs()) {
	        for (Clause cl : T) {
	            assert cl.getSize() ==1  : "has to be size 1";
	            Unit u = cl.get(0) ;
	            if(u.isNeg()) {
	            	Unit tmp = u.clone();
					tmp.unsetNeg();
					revmapping.add(tmp.getMiniSatVar(), tmp.clone());
	            	clauses.append(n + 1).append(" ").append(-u.getMiniSatVar()).append(" 0\n");
	            }
	            else {
	            	revmapping.add(u.getMiniSatVar(), u.clone());
	            	clauses.append(n + 1).append(" ").append(u.getMiniSatVar()).append(" 0\n");
	            }
	        }
	    }
	    
	    for (CNF T : Fsofter.getCnfs()) {
	    	for (Clause cl : T) {
	            assert cl.getSize() == 1 : "has to be size 1";
	            Unit u = cl.get(0) ;
	            if(u.isNeg()) {
	            	Unit tmp = u.clone();
					tmp.unsetNeg();
					revmapping.add(tmp.getMiniSatVar(), tmp.clone());
	            	clauses.append(1).append(" ").append(-u.getMiniSatVar()).append(" 0\n");
	            }
	            else {
	            	revmapping.add(u.getMiniSatVar(), u.clone());
	            	clauses.append(1).append(" ").append(u.getMiniSatVar()).append(" 0\n");
	            }
	        }
	    }
	    
	    return clauses.toString();
	}
	
public String toWCNF(Formula Fhard, Formula Fsoft) {
		
		int n = 0;//Fsofter.getClauses().size();
	    int k = Fsoft.getClauses().size();
	    int h = Fhard.getClauses().size();
	    
	    
	    int nbClauses = h + k + n;
	    StringBuilder clauses = new StringBuilder("p wcnf " + nvars + " " + nbClauses + "\n");
	    for (CNF T : Fhard.getCnfs()) {
			for (Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				
				clauses.append(k*(n+1)+n+1);
				
				for(Unit u : cl) {
					if(u.isNeg()) {
						Unit tmp = u.clone();
						tmp.unsetNeg();
						revmapping.add(tmp.getMiniSatVar(), tmp.clone());
						clauses.append(" ").append(-u.getMiniSatVar());
					}
					else {
						revmapping.add(u.getMiniSatVar(), u.clone());
						clauses.append(" ").append(u.getMiniSatVar());
					}
				}
				clauses.append(" 0\n");
			}
		}
	    for(CNF T : Fsoft.getCnfs()) {
	        for (Clause cl : T) {
	            assert cl.getSize() ==1  : "has to be size 1";
	            Unit u = cl.get(0) ;
	            if(u.isNeg()) {
	            	Unit tmp = u.clone();
					tmp.unsetNeg();
					revmapping.add(tmp.getMiniSatVar(), tmp.clone());
	            	clauses.append(n + 1).append(" ").append(-u.getMiniSatVar()).append(" 0\n");
	            }
	            else {
	            	revmapping.add(u.getMiniSatVar(), u.clone());
	            	clauses.append(n + 1).append(" ").append(u.getMiniSatVar()).append(" 0\n");
	            }
	        }
	    }
	    
	    
	    return clauses.toString();
	}
	@Override
	public SATModel maxsat(Formula Fhard, Formula Fsoft, Formula Fsofter) {
	    fireSolverEvent("BEG_maxsatsolve", false, true);
	    int n = Fsofter.getClauses().size();
	    int k = Fsoft.getClauses().size();
	    int h = Fhard.getClauses().size();
	    

	    
	    
	    SATModel res = null;
	    
	    String wcnf = toWCNF(Fhard, Fsoft, Fsofter);
	    // write wcnf in the file
	    FileWriter myWriter;
		String filename = "/tmp/sat4jmaxsat_" +  ProcessHandle.current().pid() +".wcnf";
	    try {
	    	myWriter = new FileWriter(filename);
			myWriter.write(wcnf);
			myWriter.close();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
        //System.err.println(wcnf);
        
	    Process proc;
		boolean unsat = false;
				
		try {
			
			proc = runtime.exec(new String[] { "/usr/bin/java", "-jar", torun, filename});
			
			BufferedReader stdInput = new BufferedReader(new 
			InputStreamReader(proc.getInputStream()));


			// Read the output from the command
			String line = null;
			ArrayList<Integer> values = new ArrayList<>();
			
			while ((line = stdInput.readLine()) != null) {
				if (line.startsWith("c objective function")) {
					long objInt = Long.parseLong(line.split("=")[1]);
					if(objInt > k*(n+1)+n) {
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
				res = new MiniSatModel(values, revmapping);
			}
			Files.deleteIfExists(Paths.get(filename));
			
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    

	    fireSolverEvent("END_maxsatsolve", true, false);
	    return res;
	}
	
	public SATModel maxsat(Formula Fhard, Formula Fsoft) {
	    fireSolverEvent("BEG_maxsatsolve", false, true);
	    int n = 0;//Fsofter.getClauses().size();
	    int k = Fsoft.getClauses().size();
	    int h = Fhard.getClauses().size();
	    

	    
	    
	    SATModel res = null;
	    
	    String wcnf = toWCNF(Fhard, Fsoft);
	    // write wcnf in the file
	    FileWriter myWriter;
		String filename = "/tmp/sat4jmaxsat_" +  ProcessHandle.current().pid() +".wcnf";
	    try {
	    	myWriter = new FileWriter(filename);
			myWriter.write(wcnf);
			myWriter.close();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
        //System.err.println(wcnf);
        
	    Process proc;
		boolean unsat = false;
				
		try {
			
			proc = runtime.exec(new String[] { "/usr/bin/java", "-jar", torun, filename});
			
			BufferedReader stdInput = new BufferedReader(new 
			InputStreamReader(proc.getInputStream()));


			// Read the output from the command
			String line = null;
			ArrayList<Integer> values = new ArrayList<>();
			
			while ((line = stdInput.readLine()) != null) {
				if (line.startsWith("c objective function")) {
					long objInt = Long.parseLong(line.split("=")[1]);
					if(objInt > k*(n+1)+n) {
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
				res = new MiniSatModel(values, revmapping);
			}
			Files.deleteIfExists(Paths.get(filename));
			
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    

	    fireSolverEvent("END_maxsatsolve", true, false);
	    return res;
	}
	
	

	@Override
	public SATModel solveMaxSat(Formula H, Formula Fsoft, ACQ_Network m, ConstraintMapping mapping) {
		assert false;
        return null;	    
	}

}
