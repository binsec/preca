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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.binsec.expe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.binsec.core.ACQ_Utils;
import io.github.binsec.core.DefaultExperience;
import io.github.binsec.core.acqconstraint.ACQ_ConjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_DisjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.AliasConstraint;
import io.github.binsec.core.acqconstraint.BinaryArithmetic;
import io.github.binsec.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.Contradiction;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqconstraint.DerefConstraint;
import io.github.binsec.core.acqconstraint.IntCellBinaryEqConstraint;
import io.github.binsec.core.acqconstraint.IntCellBinaryGTConstraint;
import io.github.binsec.core.acqconstraint.IntCellBinaryLTConstraint;
import io.github.binsec.core.acqconstraint.IntCellUnaryEqConstraint;
import io.github.binsec.core.acqconstraint.IntCellUnaryGTConstraint;
import io.github.binsec.core.acqconstraint.IntCellUnaryLTConstraint;
import io.github.binsec.core.acqconstraint.IntCellUnaryModConstraint;
import io.github.binsec.core.acqconstraint.Operator;
import io.github.binsec.core.acqconstraint.OverlapConstraint;
import io.github.binsec.core.acqconstraint.PtrCellBinaryGTConstraint;
import io.github.binsec.core.acqconstraint.PtrCellBinaryLTConstraint;
import io.github.binsec.core.acqconstraint.ScalarArithmetic;
import io.github.binsec.core.acqconstraint.StrlenEqConstraint;
import io.github.binsec.core.acqconstraint.ValidConstraint;
import io.github.binsec.core.acqconstraint.ConstraintFactory.ConstraintSet;
import io.github.binsec.core.acqsolver.ACQ_ChocoSolverCells;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.MiniSatSolver;
import io.github.binsec.core.acqsolver.NaPSSolver;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.acqvariable.CellType;
import io.github.binsec.core.combinatorial.AllPermutationIterator;
import io.github.binsec.core.combinatorial.CombinationIterator;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Learner;
import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.ACQ_Scope;
import io.github.binsec.core.learner.Answer;
import io.github.binsec.core.tools.Binsec;
import io.github.binsec.core.tools.CellQueryPrinter;
import io.github.binsec.core.tools.MUSSimplifier;
import io.github.binsec.core.tools.QueryPrinter;
import io.github.binsec.core.tools.Simplifier;
import io.github.binsec.core.tools.SolverSimplifier;

/**
 *
 * @author gregoire
 */
public class PreCA extends DefaultExperience {
	static boolean parallel = true;
	
	static int nbvars;
	static int nbacqvars = 0;
	
	static CellType[] types;
	static boolean[] globals;
	
	static ACQ_CellVariable[] cells;

	static String[] parsedconstraints;
	
	static ArrayList<Integer> disj = new ArrayList<>();
	static boolean autodisj = false;
	static boolean mss = false;
	
	static boolean active = true;
	static boolean strat = false;
	static boolean backknow = false;
	static boolean noprint = false;
	
	static String binaryconf = "";
	static Long learningtimeout = null;
	static Runtime runtime;
	static String torun;
	static boolean weighted = false;
	static boolean simplify = true;
	static ACQ_ConstraintSolver csolv;
	static ACQ_Bias known;
	
	/*
	 *  Variables to store if some kind of constraints are in the bias
	 *  Used to add relevant known constraints
	 */
	static boolean overlapcstr = false;
	static boolean derefcstr = false;
	
	static int[] constant_values = new int[]{0};
	
	static Binsec binsec;
	
	public PreCA() throws Exception {
		
		cells = new ACQ_CellVariable[nbvars];
		
		for (int q = 0; q < nbvars; q++) {
			cells[q] = new ACQ_CellVariable(types[q], globals[q]);
		}
		
		nbacqvars = getNbACQVars();
		
		runtime = Runtime.getRuntime();
		torun = String.format("%s/resources/exec_bin.py", System.getenv("PRECA_PATH"));
	}

	protected int getNbACQVars() {
		/*
		 * A cell is composed of at most 3 variables (one for the ref, one for the value and one for the size)
		 * If it is a pointer with ref, then there are 3 variables
		 * If it is a pointer with no ref, then there are 2 variables
		 * If it is an int with ref, then there are 2 variables
		 * If it is an int with no ref, then there is 1 variable 
		 */
		int res = 0;
		for (ACQ_CellVariable cell : cells) {
			if (cell.getRef() != null) res++;
			if (cell.getType() == CellType.PTR) res++;
			res++;
		}
		return res;
	}
	
	public ACQ_ConstraintSolver createSolver() {
		csolv = new ACQ_ChocoSolverCells(cells);
		return csolv;
	}
	
	protected void printbytes(byte[] bytes) {
		for (byte b: bytes) {
			System.out.println(b);
		}
	}
	
	protected byte[] reverse(byte[] bytes) {
		byte[] res = new byte[bytes.length];
		for (int i = 0; i<bytes.length; i++) {
			res[i] = bytes[bytes.length-1-i];
		}
		return res;
	}
	
	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public Answer ask(ACQ_Query e) {
				
				Integer s[] = new Integer[nbacqvars];
				
				for (int numvar : e.getScope()) {
					s[numvar] = e.getValue(numvar);

				}
				
				int res = binsec.call(s);
				
				if (res == 0) {
					e.classify(true);
					return Answer.YES;
				}
				else if (res == 1) {
					e.classify(false);
					return Answer.NO;
				}
				else if (res == 124) {
					//e.classify(null);
					assert false;
					return Answer.UKN;
				}
				else {
					assert false : "Error in evaluation of the expression";
					return Answer.NO; 
				}
			}
		};
	}

	protected String formatType(CellType t) {
		String type = "";
		switch (t) {
			case INT:
				type = "int";
				break;
			case UINT:
				type = "uint";
				break;
			case PTR:
				type = "void*";
				break;
			default:
				assert false: "Unknown type";	
		}
		return type;
	}
	
	protected String formatQuery(ACQ_Query query) {
		String res = "[";
		int valindex = 0;
		for (int cellindex = 0; cellindex < nbvars; cellindex ++) {
			ACQ_CellVariable var = cells[cellindex];
			
			if (var.isGlobal()) {
				res += "{ \"type\": \"void*\",  \"val\": \"@" + (var.getRef().rand()-1) + "\"}, ";
				valindex++;
			}
			
			
			
			String value = String.valueOf(query.getValue(valindex));
			if (var.getType() == CellType.PTR) {
				if (query.getValue(valindex) == 0) {
					value = "\"NULL\"";
				}
				else {
					value = "\"@" + (query.getValue(valindex) -1) + "\"";
				}
			}
			
			String type = formatType(var.getType());
			
			res += "{ \"type\": \"" + type + "\",  \"val\": " + value + "}, ";
			valindex++;
		}
		res += "]";
		return res;
	}
	
	public ACQ_Bias createBias() {
		
		BitSet bs = new BitSet();
		bs.set(0, nbacqvars);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		// build Constraints
		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();
		
		
		for (String sconstr : parsedconstraints) {
			
			String[] elems = sconstr.split("_");
			
			int constval;
			
			if (getNbVars(sconstr) == 1) {
				// Constraint of arity 1
				Integer[] cellids = getCellIds(sconstr, 1);
				Integer cellid = cellids[0];
				for (ACQ_CellVariable cell : cells) {
					
					if (cellid != null && cell.cellid != cellid) continue;
					
					switch(elems[0]) {
						case "Valid":
							if (cell.getType() != CellType.PTR) continue;
							ValidConstraint vc = weighted ? 
									new ValidConstraint(cell, 1000) : new ValidConstraint(cell);
							constraints.add(vc);
							constraints.add(weighted ? vc.getNegation(1) : vc.getNegation());
							break;
							
						case "StrlenEq":
							if (cell.getType() != CellType.PTR) continue;
							constval = Integer.parseInt(elems[2]) + 1; 
							
							StrlenEqConstraint sc = weighted ?
									new StrlenEqConstraint(cell, constval, 1) : new StrlenEqConstraint(cell, constval);
							constraints.add(sc);
							constraints.add(weighted ? sc.getNegation(1) : sc.getNegation());
							break;
							
						case "Deref":
							if (cell.getType() != CellType.PTR || !cell.isGlobal()) continue;
							derefcstr = true;
							
							DerefConstraint dc = new DerefConstraint(cell, cell);
							constraints.add(dc);
							constraints.add(dc.getNegation());
							break;
							
						case "Eq":
							if (cell.getType() != CellType.INT && cell.getType() != CellType.UINT) continue;
							constval = Integer.parseInt(elems[2]); 
							IntCellUnaryEqConstraint ic = weighted ?
									new IntCellUnaryEqConstraint(cell, constval, 500) : new IntCellUnaryEqConstraint(cell, constval);
							constraints.add(ic);
							constraints.add(weighted ? ic.getNegation(1) : ic.getNegation());
							break;
							
						case "LT":
							if (cell.getType() != CellType.INT && cell.getType() != CellType.UINT) continue;
							constval = Integer.parseInt(elems[2]);
							if (cell.getType() == CellType.UINT && constval == 0) continue;
							IntCellUnaryLTConstraint ic2 = weighted ? 
									new IntCellUnaryLTConstraint(cell, constval, 500) : new IntCellUnaryLTConstraint(cell, constval);
							constraints.add(ic2);
							constraints.add(weighted ? ic2.getNegation(1) : ic2.getNegation());
							break;
							
						case "GT":
							if (cell.getType() != CellType.INT && cell.getType() != CellType.UINT) continue;
							constval = Integer.parseInt(elems[2]);
							//if (cell.getType() == CellType.UINT && constval == 0) continue;
							IntCellUnaryGTConstraint ic3 = weighted ? 
									new IntCellUnaryGTConstraint(cell, constval, 1) : new IntCellUnaryGTConstraint(cell, constval);
							constraints.add(ic3);
							constraints.add(weighted ? ic3.getNegation(500) : ic3.getNegation());
							break;
						
						case "Mod":
							if (cell.getType() != CellType.INT && cell.getType() != CellType.UINT) continue;
							int mod = Integer.parseInt(elems[2]);
							int res = Integer.parseInt(elems[3]);
							IntCellUnaryModConstraint ic4 = weighted ?
									new IntCellUnaryModConstraint(cell, mod, res, 1): new IntCellUnaryModConstraint(cell, mod, res); 
							constraints.add(ic4);
							constraints.add(weighted ? ic4.getNegation(500) : ic4.getNegation());
							break;
						default:
							assert false: "Unknown constraint";
					}
				}
			}
			else if (getNbVars(sconstr) == 2) {
				if (cells.length < 2) continue;
				// Constraint of arity 2
				
				Integer[] cellids = getCellIds(sconstr, 2);
				Integer cellid1 = cellids[0];
				Integer cellid2 = cellids[1];
				
				CombinationIterator iterator = new CombinationIterator(cells.length, 2);
				while (iterator.hasNext()) {
					int[] vars = iterator.next();
					AllPermutationIterator pIterator = new AllPermutationIterator(2);
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();
						
						ACQ_CellVariable cell1 = cells[vars[pos[0]]];
						ACQ_CellVariable cell2 = cells[vars[pos[1]]];
						
						if (cellid1 != null && cell1.cellid != cellid1) continue;
						if (cellid2 != null && cell2.cellid != cellid2) continue;
						
						switch (elems[0]) {
						case "Alias":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if (cell1.getType() != CellType.PTR || cell2.getType() != CellType.PTR) continue;
							AliasConstraint ac = weighted ? 
									new AliasConstraint(cell1, cell2, 1) : new AliasConstraint(cell1, cell2);
							constraints.add(ac);
							constraints.add(weighted ? ac.getNegation(1000) : ac.getNegation());
							break;
						
						case "PtrLT":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if(cell1.getType() != CellType.PTR || cell2.getType() != CellType.PTR) continue;
							PtrCellBinaryLTConstraint pc1 = new PtrCellBinaryLTConstraint(cell1, cell2);
							constraints.add(pc1);
							constraints.add(pc1.getNegation());
							break;
							
						case "PtrGT":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if(cell1.getType() != CellType.PTR || cell2.getType() != CellType.PTR) continue;
							PtrCellBinaryGTConstraint pc2 = new PtrCellBinaryGTConstraint(cell1, cell2);
							constraints.add(pc2);
							constraints.add(pc2.getNegation());
							break;
							
						case "Deref":
							if (cell1.getType() != CellType.PTR || !cell2.isGlobal()) continue;
							derefcstr = true;
							DerefConstraint dc = new DerefConstraint(cell1, cell2);
							constraints.add(dc);
							constraints.add(dc.getNegation());
							break;
							
						case "Overlap":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if (cell1.getType() != CellType.PTR || cell2.getType() != CellType.PTR) continue;
							if (cell1.isGlobal() != cell2.isGlobal()) continue;
							overlapcstr = true;
							OverlapConstraint oc = weighted ?
									new OverlapConstraint(cell1, cell2, 1) : new OverlapConstraint(cell1, cell2);
							constraints.add(oc);
							constraints.add(weighted ? oc.getNegation(1000) : oc.getNegation());
							break;
							
						case "Eq":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if ((cell1.getType() != CellType.INT || cell2.getType() != CellType.INT) &&
									(cell1.getType() != CellType.UINT || cell2.getType() != CellType.UINT)) continue;
							IntCellBinaryEqConstraint ic = new IntCellBinaryEqConstraint(cell1, cell2);
							constraints.add(ic);
							constraints.add(ic.getNegation());
							break;
							
						case "LT":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if ((cell1.getType() != CellType.INT || cell2.getType() != CellType.INT) &&
									(cell1.getType() != CellType.UINT || cell2.getType() != CellType.UINT)) continue;
							IntCellBinaryLTConstraint ic2 = new IntCellBinaryLTConstraint(cell1, cell2);
							constraints.add(ic2);
							constraints.add(ic2.getNegation());
							break;
							
						case "GT":
							if (! (vars[pos[0]] < vars[pos[1]])) continue; 
							if ((cell1.getType() != CellType.INT || cell2.getType() != CellType.INT) &&
									(cell1.getType() != CellType.UINT || cell2.getType() != CellType.UINT)) continue;
							IntCellBinaryGTConstraint ic3 = new IntCellBinaryGTConstraint(cell1, cell2);
							constraints.add(ic3);
							constraints.add(ic3.getNegation());
							break;
							
						default:
							assert false: "Unknown constraint";
					
						}
					}
				}
			}
			else {
				assert false : "No constraints of arity 0 or > 2";
			}
		}
		
		if (disj.size() != 0) {
			ConstraintSet[] sets = new ConstraintSet[constraints.size()-2+1];
			for (int nb_disj : disj) {
				if (nb_disj > constraints.size()) continue;
				
				sets[nb_disj-2] = constraintFactory.createSet();
				CombinationIterator iterator = new CombinationIterator(constraints.size(), nb_disj);
				while (iterator.hasNext()) {
					int[] constrs = iterator.next();
					if(alldiff(constrs)) {
						ACQ_IConstraint[] iconstrs = new ACQ_IConstraint[constrs.length];
						for (int i = 0; i < constrs.length; i++) {
							iconstrs[i] = constraints.get_Constraint(constrs[i]);
						}
						
						if (notneg(iconstrs)) {
							ConstraintSet constraintSet = constraintFactory.createSet();
							
							int weight = 1;
							weight = iconstrs[0].getWeight();
							for (ACQ_IConstraint constr: iconstrs) {
								constraintSet.add(constr);
								weight = Math.max(weight, constr.getWeight());
							}
							
							ACQ_DisjunctionConstraint new_c = weighted ? 
									new ACQ_DisjunctionConstraint(constraintFactory, constraintSet, weight) : 
										new ACQ_DisjunctionConstraint(constraintFactory, constraintSet);
									
							sets[nb_disj-2].add(new_c);
							ACQ_IConstraint neg = weighted ? new_c.getNegation(1) : new_c.getNegation();
							sets[nb_disj-2].add(neg);
						}
					}
				}
			}
			
			for (ConstraintSet set : sets) {
				if (set != null) constraints.addAll(set);
			}
			
		}
		
		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		return new ACQ_Bias(network);
	}
	
	private int getNbVars(String sconstr) {
		int res = 0;
		String[] elems = sconstr.split("_");
		for (int i=1; i < elems.length; i++) {
			if (Character.isUpperCase(elems[i].charAt(0)) || elems[i].startsWith("v")) {
				res += 1;
			}
		}
		return res;
	}
	
	private Integer[] getCellIds(String sconstr, int nvars) {
		Integer[] res = new Integer[nvars];
		int index = 0;
		String[] elems = sconstr.split("_");
		for (int i=1; i<elems.length;i++) {
			if (Character.isUpperCase(elems[i].charAt(0))) {
				res[index] = null;
			}
			else if (elems[i].startsWith("v")){
				res[index] = Integer.parseInt(elems[i].substring(1));
			}
			index++;
		}
		return res;
	}
	
	protected boolean alldiff(int[] list) {
		for (int i = 0; i < list.length - 1; i++) {
			for (int j = i+1; j < list.length; j++) {
				if (list[i] == list[j]) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected boolean notneg(ACQ_IConstraint[] list) {
		for (int i = 0; i < list.length - 1; i++) {
			for (int j = i+1; j < list.length; j++) {
				if (list[i].equals(list[j].getNegation())) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public ACQ_Bias getKnownConstraints() {
		BitSet bs = new BitSet();
		bs.set(0, nbacqvars);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		// build Constraints
		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();
		
		
		for (ACQ_CellVariable cell : cells) {
			if (cell.getType() == CellType.PTR) {
				ValidConstraint val = new ValidConstraint(cell);
				StrlenEqConstraint eq = new StrlenEqConstraint(cell, 1);
				//UnaryArithmetic eq = new UnaryArithmetic("EqualX", cell.getSize().id, Operator.EQ, 1);
				
				ACQ_DisjunctionConstraint disj = new ACQ_DisjunctionConstraint(constraintFactory, val, eq);
				constraints.add(disj);
			}
		}
		
		if(nbvars >= 2) {
			CombinationIterator iterator2 = new CombinationIterator(nbvars, 2);
			while (iterator2.hasNext()) {
				int[] vars = iterator2.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(2);
				while (pIterator.hasNext()) {
					int[] pos = pIterator.next();
	
					if (vars[pos[0]] < vars[pos[1]]) // NL: commutative relations
					{
						ACQ_CellVariable cell1 = cells[vars[pos[0]]];
						ACQ_CellVariable cell2 = cells[vars[pos[1]]];
						
						if (cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR &&
								cell1.isGlobal() == cell2.isGlobal()) {
							OverlapConstraint over = new OverlapConstraint(cell1, cell2);
							
							BinaryArithmetic lower1 = new BinaryArithmetic("LessEqualXY", 
									cell1.getValue().id, Operator.LE, cell2.getValue().id, "GreaterXY");
							BinaryArithmetic lower2 = new BinaryArithmetic("LessXY", 
									cell2.getValue().id, Operator.LT, cell1.getValue().id, "GreaterEqualXY");
							
							ACQ_ConjunctionConstraint conj1 = new ACQ_ConjunctionConstraint(constraintFactory, over, lower1);
							ACQ_ConjunctionConstraint conj2 = new ACQ_ConjunctionConstraint(constraintFactory, over, lower2);
							
							ScalarArithmetic scal1 = new ScalarArithmetic("Dist", 
									new int[] {cell1.getValue().id, cell1.getSize().id, cell2.getValue().id, cell2.getSize().id}, 
									new int[] {1000, 1, -1000, -1}, 
									Operator.EQ, 0);
							
							ScalarArithmetic scal2 = new ScalarArithmetic("Dist", 
									new int[] {cell2.getValue().id, cell2.getSize().id, cell1.getValue().id, cell1.getSize().id}, 
									new int[] {1000, 1, -1000, -1}, 
									Operator.EQ, 0);
							
							ACQ_DisjunctionConstraint disj1 = new ACQ_DisjunctionConstraint(constraintFactory,
									conj1.getNegation(), scal1);
	
							ACQ_DisjunctionConstraint disj2 = new ACQ_DisjunctionConstraint(constraintFactory,
									conj2.getNegation(), scal2);
							
							constraints.add(disj1);
							constraints.add(disj2);
							
						}
						
						if (cell1.isGlobal() && cell2.isGlobal()) {
							/*
							 * Two global variables must have different ref addresses 
							 */
							
							BinaryArithmetic diff_ref = new BinaryArithmetic("DiffXY", 
									cell1.getRef().id, Operator.NQ, cell2.getRef().id, "EqualXY");
							
							constraints.add(diff_ref);
							
						}
						
					}
				}
			}
		}
		
		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		known = new ACQ_Bias(network);
		return known;
	}
	
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping) {
		ConstraintFactory constraintfactory = bias.getNetwork().getFactory();
		
		ContradictionSet res = new ContradictionSet(constraintfactory, bias.getVars(), mapping); // Set mapping (3 arg)
		
		for (ACQ_IConstraint cst : this.getKnownConstraints().getConstraints()) {
			ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
			net.add(cst, true);
			res.addFact(new Contradiction(net));
		}
		
		if (!backknow)	return res;
		for (ACQ_IConstraint constr: bias.getConstraints()) {
			if (constr.getName().contains("_or_") || 
				(!constr.getName().contains("_and_") && !constr.getName().contains("Not"))) { // we don't want to add redundancy

				ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
				net.add(constr, true);
				net.add(constr.getNegation(), true);
				res.add(new Contradiction(net));
			}
			
			if (disj.contains(2)) {
				/*
				 * remove c1 /\ (~c1 /\ X)
				 * and remove c1 /\ c2 /\ (~c1 /\ ~c2)
				 * and TODO remove a(c1) /\ ~a(c1 \/ X) <=> false 
				 */
				for (ACQ_IConstraint constr2: bias.getConstraints()) {
					if (!(constr instanceof ACQ_DisjunctionConstraint) && !(constr instanceof ACQ_ConjunctionConstraint) &&
						!(constr2 instanceof ACQ_DisjunctionConstraint) && !(constr2 instanceof ACQ_ConjunctionConstraint) &&
						(!constr.equals(constr2)) && (!constr.equals(constr2.getNegation()))) {
						ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
						net.add(constr, true);
						net.add(constr2, true);
						net.add(new ACQ_DisjunctionConstraint(constraintfactory, constr.getNegation(), constr2.getNegation()), true);
						res.add(new Contradiction(net));
					}
				}
			}
		}
		
		if(nbvars >= 2) {
			CombinationIterator iterator2 = new CombinationIterator(nbvars, 2);
			while (iterator2.hasNext()) {
				int[] vars = iterator2.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(2);
				while (pIterator.hasNext()) {
					int[] pos = pIterator.next();
	
					if (vars[pos[0]] < vars[pos[1]]) // NL: commutative relations
					{
						ACQ_CellVariable cell1 = cells[vars[pos[0]]];
						ACQ_CellVariable cell2 = cells[vars[pos[1]]];
						ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
						
						ACQ_IConstraint c1 = null;
						ACQ_IConstraint c2 = null;
						ACQ_IConstraint c3 = null;
						
						if (cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR) {
							c1 = new ValidConstraint(cell1, true); // not valid
							//c2 = new AliasConstraint(cell1, cell2, true); // not alias
							c2 = new AliasConstraint(cell1, cell2); // alias
							c3 = new ValidConstraint(cell2); // valid
							
							if (!bias.contains(c1) || !bias.contains(c2) || !bias.contains(c3)) continue;
							
							net.add(c1, true);
							net.add(c2, true);
							net.add(c3, true);
							res.add(new Contradiction(net));
							
						}
						else if (cell1.getType() == CellType.INT && cell2.getType() == CellType.INT) {
							c1 = new IntCellUnaryEqConstraint(cell1, 0); // = 0
							c2 = new IntCellBinaryEqConstraint(cell1, cell2); // cell1 = cell2
							c3 = new IntCellUnaryEqConstraint(cell2, 0, true); // != 0
							
							if (!bias.contains(c1) || !bias.contains(c2) || !bias.contains(c3)) continue;
							
							net.add(c1, true);
							net.add(c2, true);
							net.add(c3, true);
							res.add(new Contradiction(net));
						}
						
						
					}
				}
			}
			
			if (nbvars >= 3) {
				CombinationIterator iterator3 = new CombinationIterator(nbvars, 3);
				while (iterator3.hasNext()) {
					int[] vars = iterator3.next();
					AllPermutationIterator pIterator = new AllPermutationIterator(3);
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();
		
						if (vars[pos[0]] < vars[pos[1]] && vars[pos[1]] < vars[pos[2]]) // NL: commutative relations
						{
							ACQ_CellVariable cell1 = cells[vars[pos[0]]];
							ACQ_CellVariable cell2 = cells[vars[pos[1]]];
							ACQ_CellVariable cell3 = cells[vars[pos[2]]];
							
							ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
							
							if (cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR 
									&& cell3.getType() == CellType.PTR) {
								ACQ_IConstraint c1 = new AliasConstraint(cell1, cell2); // cell1 = cell2
								ACQ_IConstraint c2 = new AliasConstraint(cell1, cell3); // cell1 = cell3
								ACQ_IConstraint c3 = new AliasConstraint(cell2, cell3, true); // cell2 != cell3
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
								
								net = new ACQ_Network(constraintfactory, bias.getVars());
								c1 = new AliasConstraint(cell1, cell2); // cell1 = cell2
								c2 = new AliasConstraint(cell2, cell3); // cell2 = cell3
								c3 = new AliasConstraint(cell1, cell3, true); // cell1 != cell3
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
								
								net = new ACQ_Network(constraintfactory, bias.getVars());
								c1 = new AliasConstraint(cell1, cell3); // cell1 = cell3
								c2 = new AliasConstraint(cell2, cell3); // cell2 = cell3
								c3 = new AliasConstraint(cell1, cell2, true); // cell1 != cell2
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
							}
							else if (cell1.getType() == CellType.INT && cell2.getType() == CellType.INT 
									&& cell3.getType() == CellType.INT) {
								ACQ_IConstraint c1 = new IntCellBinaryEqConstraint(cell1, cell2); // cell1 = cell2
								ACQ_IConstraint c2 = new IntCellBinaryEqConstraint(cell1, cell3); // cell1 = cell3
								ACQ_IConstraint c3 = new IntCellBinaryEqConstraint(cell2, cell3, true); // cell2 != cell3
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
								
								net = new ACQ_Network(constraintfactory, bias.getVars());
								c1 = new IntCellBinaryEqConstraint(cell1, cell2); // cell1 = cell2
								c2 = new IntCellBinaryEqConstraint(cell2, cell3); // cell2 = cell3
								c3 = new IntCellBinaryEqConstraint(cell1, cell3, true); // cell1 != cell3
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
								
								net = new ACQ_Network(constraintfactory, bias.getVars());
								c1 = new IntCellBinaryEqConstraint(cell1, cell3); // cell1 = cell3
								c2 = new IntCellBinaryEqConstraint(cell2, cell3); // cell2 = cell3
								c3 = new IntCellBinaryEqConstraint(cell1, cell2, true); // cell1 != cell2
								
								if (bias.contains(c1) & bias.contains(c2) && bias.contains(c3)) {
									net.add(c1, true);
									net.add(c2, true);
									net.add(c3, true);
									res.add(new Contradiction(net));
								}
							}
						}
					}
				}
			}
		}
		
		if(nbvars >= 2) {
			CombinationIterator iterator4 = new CombinationIterator(nbvars, 2);
			while (iterator4.hasNext()) {
				int[] vars = iterator4.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(2);
				while (pIterator.hasNext()) {
					int[] pos = pIterator.next();
	
					if (vars[pos[0]] < vars[pos[1]]) // NL: commutative relations
					{
						ACQ_CellVariable cell1 = cells[vars[pos[0]]];
						ACQ_CellVariable cell2 = cells[vars[pos[1]]];
						
						
						if (cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR) {
							ACQ_Network net = new ACQ_Network(constraintfactory, bias.getVars());
							
							ACQ_IConstraint c1 = new AliasConstraint(cell1, cell2);
							ACQ_IConstraint c2 = new ValidConstraint(cell1, true);
							
							if (bias.contains(c1) && bias.contains(c2)) {
								net.add(c1, true);
								net.add(c2, true);
								res.add(new Contradiction(net));
							}
							
							net = new ACQ_Network(constraintfactory, bias.getVars());
							c1 = new AliasConstraint(cell1, cell2);
							c2 = new ValidConstraint(cell2, true);
							
							if (bias.contains(c1) && bias.contains(c2)) {
								net.add(c1, true);
								net.add(c2, true);
								res.add(new Contradiction(net));
							}
							
							net = new ACQ_Network(constraintfactory, bias.getVars());
							c1 = new AliasConstraint(cell1, cell2, true);
							c2 = new ValidConstraint(cell1, true);
							
							if (bias.contains(c1) && bias.contains(c2)) {
								net.add(c1, true);
								net.add(c2, true);
								res.add(new Contradiction(net));
							}
							
							net = new ACQ_Network(constraintfactory, bias.getVars());
							c1 = new AliasConstraint(cell1, cell2, true);
							c2 = new ValidConstraint(cell2, true);
							
							if (bias.contains(c1) && bias.contains(c2)) {
								net.add(c1, true);
								net.add(c2, true);
								res.add(new Contradiction(net));
							}
						}
					}
				}
			}
		}
		
		return res;
	}

	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias){
		if (!strat) return null;
		
		ArrayList<ACQ_Network> res = new ArrayList<>();
		
		ACQ_Network net = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
		
		// First query (no NULL, no alias, no deref)
		for (ACQ_CellVariable cell : cells ) {
			if (cell.getType() == CellType.PTR) {
				net.add(new ValidConstraint(cell), true);
				if (cell.isGlobal()) {
					net.add(new DerefConstraint(cell, cell, true), true);
				}
			}
			else if (cell.getType() == CellType.INT) {
				net.add(new IntCellUnaryGTConstraint(cell, 0), true); // integer > 0
			}
			else if (cell.getType() == CellType.UINT) {
				net.add(new IntCellUnaryEqConstraint(cell, 0, true), true); // integer != 0
			}
		}
		
		if (cells.length >= 2) {
			// No aliasing between pointers variables
			// and no deref
			// No equality between integer values
			CombinationIterator iterator = new CombinationIterator(cells.length, 2);
			while (iterator.hasNext()) {
				int[] vars = iterator.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(2);
				while (pIterator.hasNext()) {
					int[] pos = pIterator.next();
					ACQ_CellVariable cell1 = cells[vars[pos[0]]];
					ACQ_CellVariable cell2 = cells[vars[pos[1]]];
					
					if (cell1.getType() == CellType.PTR && cell2.isGlobal()) {
						net.add(new DerefConstraint(cell1, cell2, true), true); // not deref
					}
					
					if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR) {
						net.add(new AliasConstraint(cell1, cell2, true), true); // not alias
					}
					
					if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.INT && cell2.getType() == CellType.INT) {
						net.add(new IntCellBinaryEqConstraint(cell1, cell2, true), true); // int1 != int2
					}
					if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.UINT && cell2.getType() == CellType.UINT) {
						net.add(new IntCellBinaryEqConstraint(cell1, cell2, true), true); // int1 != int2
					}
				}
				
			}
			res.add(net);
		}
		
		/*
		 * Force next queries' variables to be non NULL and only one aliasing
		 */
		
		if (cells.length >= 2) {
			
			// At most one aliasing
			CombinationIterator iterator = new CombinationIterator(cells.length, 2);
			while (iterator.hasNext()) {
				// Choose one couple of pointers that will alias
				int[] vars = iterator.next();
				ACQ_CellVariable cell1 = cells[vars[0]];
				ACQ_CellVariable cell2 = cells[vars[1]];
				
				// Check that vars are not the same and cells types
				if (vars[0] == vars[1] || cell1.getType() != CellType.PTR || cell2.getType() != CellType.PTR) {
					continue;
				}
				
				net = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
				
				// no NULL pointer, no auto deref and no integer equal to 0
				for (ACQ_CellVariable cell : cells ) {
					if (cell.getType() == CellType.PTR) {
						net.add(new ValidConstraint(cell), true);
						if (cell.isGlobal()) {
							net.add(new DerefConstraint(cell, cell, true), true);
						}
					}
					else if (cell.getType() == CellType.INT) {
						net.add(new IntCellUnaryGTConstraint(cell, 0), true); // integer > 0
					}
					else if (cell.getType() == CellType.UINT) {
						net.add(new IntCellUnaryEqConstraint(cell, 0, true), true); // integer != 0
					}
				}
				
				CombinationIterator iterator2 = new CombinationIterator(cells.length, 2);
				while (iterator2.hasNext()) {
					int[] vars2 = iterator2.next();
					AllPermutationIterator pIterator = new AllPermutationIterator(2);
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();
						ACQ_CellVariable cell3 = cells[vars2[pos[0]]];
						ACQ_CellVariable cell4 = cells[vars2[pos[1]]];
					
						if (cell1.equals(cell3) && cell2.equals(cell4)) {
							// Type already checked previously
							net.add(new AliasConstraint(cell3, cell4), true); // alias
						} else if(vars2[pos[0]] < vars2[pos[1]] && 
									cell3.getType() == CellType.PTR && cell4.getType() == CellType.PTR) {
								net.add(new AliasConstraint(cell3, cell4, true), true); // not alias
						}
						
						if (cell3.getType() == CellType.PTR && cell4.isGlobal()) {
							net.add(new DerefConstraint(cell3, cell4, true), true); // not deref
						}
						
						if(vars2[pos[0]] < vars2[pos[1]] && cell3.getType() == CellType.INT && cell4.getType() == CellType.INT) {
							net.add(new IntCellBinaryEqConstraint(cell3, cell4, true), true); // int1 != int2
						}
						if(vars2[pos[0]] < vars2[pos[1]] && cell3.getType() == CellType.UINT && cell4.getType() == CellType.UINT) {
							net.add(new IntCellBinaryEqConstraint(cell3, cell4, true), true); // int1 != int2
						}
					}
					
				}
				res.add(net);
			}
		}
		
		/*
		 * Force next queries' variables to one NULL and no aliasing
		 * TODO
		 */
		for (ACQ_CellVariable cell : cells ) {
			// choose a pointer to be NULL
			
			if (cell.getType() != CellType.PTR) {
				continue;
			}
			
			net = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
			
			// First query (no NULL, no alias, no deref)
			for (ACQ_CellVariable cell2 : cells ) {
				
				if (cell.equals(cell2)) {
					// If not valid(cell) no need to add deref(cell, cell) constraint
					net.add(new ValidConstraint(cell, true), true); // not valid
				}
				else if (cell2.getType() == CellType.PTR) {
					net.add(new ValidConstraint(cell2), true);
					if (cell2.isGlobal()) {
						net.add(new DerefConstraint(cell2, cell2, true), true);
					}
				}
				else if (cell2.getType() == CellType.INT) {
					net.add(new IntCellUnaryGTConstraint(cell2, 0), true); // integer > 0
				}
				else if (cell2.getType() == CellType.UINT) {
					net.add(new IntCellUnaryEqConstraint(cell2, 0, true), true); // integer != 0
				}
			}
			
			if (cells.length >= 2) {
				// No aliasing between pointers variables
				// and no deref
				// No equality between integer values
				CombinationIterator iterator = new CombinationIterator(cells.length, 2);
				while (iterator.hasNext()) {
					int[] vars = iterator.next();
					AllPermutationIterator pIterator = new AllPermutationIterator(2);
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();
						ACQ_CellVariable cell1 = cells[vars[pos[0]]];
						ACQ_CellVariable cell2 = cells[vars[pos[1]]];
					
						if (cell1.getType() == CellType.PTR && cell2.isGlobal()) {
							net.add(new DerefConstraint(cell1, cell2, true), true); // not deref
						}
						
						if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.PTR && cell2.getType() == CellType.PTR) {
							net.add(new AliasConstraint(cell1, cell2, true), true); // not alias
						}
						
						if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.INT && cell2.getType() == CellType.INT) {
							net.add(new IntCellBinaryEqConstraint(cell1, cell2, true), true); // int1 != int2
						}

						if(vars[pos[0]] < vars[pos[1]] && cell1.getType() == CellType.UINT && cell2.getType() == CellType.UINT) {
							net.add(new IntCellBinaryEqConstraint(cell1, cell2, true), true); // int1 != int2
						}
					}
					
				}
				res.add(net);
			}
		}
		
		return res;
	}
	
	public static void process(String args[]) {
		PreCA expe;
		try {
			expe = new PreCA();
			expe.setParams(60000, // timeout
				 !noprint,
				 true
			);
			if (!mss) ACQ_Utils.executeConacqExperience(expe, active);
			else ACQ_Utils.executeDCAExperience(expe);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	public static void main(String[] args) {
		Options options = new Options();

		OptionGroup group1 = new OptionGroup();
        Option file_opt = new Option("file", true, "configuration file");
        file_opt.setRequired(true);
        group1.addOption(file_opt);
        options.addOptionGroup(group1);
        
        OptionGroup group2 = new OptionGroup();
        Option help_opt = new Option("help", false, "help");
        help_opt.setRequired(false);
        group2.addOption(help_opt);
        
        Option confhelp_opt = new Option("helpconf", false, "help for config files");
        confhelp_opt.setRequired(false);
        group2.addOption(confhelp_opt);
        
        options.addOptionGroup(group2);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
        	print_header();
            System.out.println(e.getMessage());
            formatter.printHelp("preca", options);
            System.exit(1);
        }
        
        boolean helpMode = cmd.hasOption("help") || args.length == 0;
        if (helpMode) {
        	print_header();
			formatter.printHelp("preca", options, true);
			System.exit(0);
        }
        
        boolean confhelp = cmd.hasOption("helpconf");
        if (confhelp) {
        	print_header();
        	print_confhelp();
        	System.exit(0);
        }
        
        String filename = cmd.getOptionValue("file");
        if (filename == null) {
        	print_header();
        	formatter.printHelp("preca", options, true);
			System.exit(1);
        }
        
        parse_infile(filename);
		process(args);
	}
	
	private static void print_header() {
		String header = "\r\n"
				+ "$$$$$$$\\                       $$$$$$\\   $$$$$$\\  \r\n"
				+ "$$  __$$\\                     $$  __$$\\ $$  __$$\\ \r\n"
				+ "$$ |  $$ | $$$$$$\\   $$$$$$\\  $$ /  \\__|$$ /  $$ |\r\n"
				+ "$$$$$$$  |$$  __$$\\ $$  __$$\\ $$ |      $$$$$$$$ |\r\n"
				+ "$$  ____/ $$ |  \\__|$$$$$$$$ |$$ |      $$  __$$ |\r\n"
				+ "$$ |      $$ |      $$   ____|$$ |  $$\\ $$ |  $$ |\r\n"
				+ "$$ |      $$ |      \\$$$$$$$\\ \\$$$$$$  |$$ |  $$ |\r\n"
				+ "\\__|      \\__|       \\_______| \\______/ \\__|  \\__|\r\n"
				+ "                                                  \r\n";
		
		System.out.println(header);
	}
	
	private static void print_confhelp() {
		String help = "Format of configuration file: \n"
					+ "\r\n"
					+ "# Variables Setup\r\n"
					+ "nargs: <int>                     number of inputs of the function\r\n"
					+ "types: type1, ..., typeN         type of each input (INT, UINT, PTR)\r\n"
					+ "globals: scope1, ..., scopeN     scope of each input (true means global, false local)\r\n"
					+ "addr_globals: addr1, ..., addrM  address where are stored in the binary each global variable\r\n"		
					+ "\r\n"
					+ "# Emulator Options\r\n"
					+ "bin: <path>                      path to the binary\r\n"
					+ "binsec: <path>                   path to the binsec initialization file\r\n"
					+ "\r\n"
					+ "# Expected Result\r\n"
					+ "precond: <predicate>             expected result (optional, used in PreCA evaluation)\r\n"
					+ "\r\n"
					+ "# Acquisition Options\r\n"
					+ "active: <bool>                   run acquisition in active mode (default, true)\r\n"
					+ "disj: <mode>                     mode to handle disjunction among auto, mss, <int> and {<int>, ..., <int>}\r\n"
					+ "strat: <bool>                    use preprocessing (default, true)\r\n"
					+ "back: <bool>                     use background knowledge (default, true)\r\n"
					+ "verbose: <bool>                  set verbose mode (default, true)\r\n"
					+ "json: <bool>                     format results in json (default, false)\r\n"
					+ "timeout: <time-seconds>          acquisition time budget in seconds\r\n"
					+ "emultimeout: <time-seconds>      emulation time budget in seconds\r\n"
					+ "action_emultimeout: <bool>       anwser to return on emulation timeout (default: false)\r\n"
					+ "simplify: <bool>                 simplify the result precondition\r\n"
					+ "\r\n"
					+ "# Language Options\r\n"
					+ "bias: constr1, ..., constrN      set of constraints to consider (if not setted add all constraints)\r\n"
					+ "                                 remark: automatically adds the negation of each constraint";
		System.out.println(help);
	}
	
	private static void parse_infile(String filename) {
		binaryconf = new File(filename).getAbsolutePath();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
            	if (line.startsWith("active")) {
            		active = Boolean.parseBoolean(line.split(":")[1].strip());
            	} 
            	else if (line.startsWith("nargs")) {
            		nbvars = Integer.parseInt(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("types")) {
            		String[] stypes = line.split(":")[1].split(",");
            		CellType[] typelist = new CellType[stypes.length];
            		for (int i = 0; i < stypes.length; i++) {
            			String t = stypes[i].strip();
            			if (t.contains("PTR")) typelist[i] = CellType.PTR;
            			else if (t.contains("UINT")) typelist[i] = CellType.UINT;
            			else if (t.contains("INT")) typelist[i] = CellType.INT;
            			else assert false : "unknown type";
            		}
            		types = typelist;
            	}
            	else if (line.startsWith("globals")) {
            		String[] sglobals = line.split(":")[1].split(",");
            		boolean[] globalslist = new boolean[sglobals.length];
            		for (int i = 0; i < sglobals.length; i++) {
            			globalslist[i] = Boolean.parseBoolean(sglobals[i].strip());
            		}
            		globals = globalslist;
            	}
            	else if (line.startsWith("disj")) {
            		String val = line.split(":")[1].strip();
            		if (val.equals("auto")) autodisj = true;
            		else if (val.startsWith("mss")) mss = true;
            		else if (val.startsWith("{")) {
            			val = val.replace("{", "").replace("}", "");
            			for (String s : val.split(",")) {
            				disj.add(Integer.parseInt(s.strip()));
            			}
            		}
            		else {
            			for (int i = 2; i <= Integer.parseInt(val); i++) {
            				disj.add(i);
            			}
            		}
            	}
            	else if (line.startsWith("strat")) {
            		strat = Boolean.parseBoolean(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("back")) {
            		backknow = Boolean.parseBoolean(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("verbose")) {
            		noprint = !Boolean.parseBoolean(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("json")) {
            		json = Boolean.parseBoolean(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("timeout")) {
            		learningtimeout = 1000*Long.parseLong(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("simplify")) {
            		simplify = Boolean.parseBoolean(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("bias")) {
            		parsedconstraints = line.split(":")[1].split(",");
            		for (int i=0; i < parsedconstraints.length; i++) {
            			parsedconstraints[i] = parsedconstraints[i].strip();
            		}
            	}
            }
            assert active || !mss;
            assert active || !strat;

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
	 
	 	if(autodisj) {
	 		int maxdisj = 1;
	 		for (CellType type : types) {
	 			if (type == CellType.INT || type == CellType.UINT) {
	 				maxdisj += 1;
	 			}
	 		}
	 		
	 		maxdisj = Integer.max(maxdisj, 2);
	 		for (int i = 2; i <= maxdisj; i++) {
				disj.add(i);
			}
	 	}
	 	
	 	binsec = new Binsec(filename, types, globals);
	}

	@Override
	public SATSolver createSATSolver() {
		return new MiniSatSolver();
		//return new NaPSSolver();
	}
	
	@Override
	public Long getLearningTimeout() {
		return learningtimeout;
	}

	@Override
	public ACQ_CellVariable[] getCells() {
		// TODO Auto-generated method stub
		return cells;
	}
	
	@Override
	public boolean isWeighted() {
		return weighted;
	}

	
	public QueryPrinter getQueryPrinter() {
		return new CellQueryPrinter(types, globals);
	}
	
	
	@Override
	public ACQ_Network simplify(ACQ_Network net) {
		if (!simplify) return net;
		Simplifier simpl = new SolverSimplifier(csolv, known);
		return simpl.simplify(net);
	}

	@Override
	public ACQ_Network simplify(ACQ_Network net, ArrayList<ACQ_Network> muses) {
		if (!simplify) return net;
		Simplifier simpl = new MUSSimplifier(muses, csolv, known);
		return simpl.simplify(net);
	}
}
