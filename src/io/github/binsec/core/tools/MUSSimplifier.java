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

package io.github.binsec.core.tools;

import java.util.ArrayList;
import java.util.Comparator;

import io.github.binsec.core.acqconstraint.ACQ_ConjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_DisjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintFactory.ConstraintSet;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.combinatorial.CombinationIterator;
import io.github.binsec.core.learner.ACQ_Bias;

public class MUSSimplifier implements Simplifier {

	ArrayList<ACQ_Network> muses;
	ACQ_ConstraintSolver csolv;
	ACQ_Bias known;
	
	public MUSSimplifier(ArrayList<ACQ_Network> muses) {
		muses.sort(new Comparator<ACQ_Network>() {
		    @Override
		    public int compare(ACQ_Network n1, ACQ_Network n2) {
		        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
		        int diff = n1.size() - n2.size();
		        if (diff > 0) return 1;
		        else if (diff == 0) return 0;
		        else return -1;
		    }});
		this.muses = muses;
	}
	
	public MUSSimplifier(ArrayList<ACQ_Network> muses, ACQ_ConstraintSolver constraintsolver, ACQ_Bias known) {
		muses.sort(new Comparator<ACQ_Network>() {
		    @Override
		    public int compare(ACQ_Network n1, ACQ_Network n2) {
		        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
		        int diff = n1.size() - n2.size();
		        if (diff > 0) return 1;
		        else if (diff == 0) return 0;
		        else return -1;
		    }});
		this.muses = muses;
		this.csolv = constraintsolver;
		this.known = known; 
	}
	
	protected boolean isNext(ACQ_DisjunctionConstraint d1, ACQ_DisjunctionConstraint d2) {
		ConstraintSet set1 = d1.constraintFactory.createSet(d1.constraintSet);
		ConstraintSet set2 = d2.constraintFactory.createSet(d2.constraintSet);
		if (set1.size() == set2.size()) {
			ConstraintSet set12 = d1.constraintFactory.createSet(set1);
			ConstraintSet set22 = d2.constraintFactory.createSet(set2);
			set12.removeAll(set2);
			set22.removeAll(set1);
			if (set12.size() == 1 && set22.size() == 1) {
				return true;
				/*if (set22.contains(set12.get_Constraint(0).getNegation())) {
					return true;
				}*/
			}
		}
		return false;
	}
	
	protected ACQ_IConstraint simplifyClause(ACQ_DisjunctionConstraint disj, ArrayList<ACQ_Network> muses) {
		ConstraintFactory constraintFactory = disj.constraintFactory;
		ConstraintSet set = constraintFactory.createSet();
		set.addAll(disj.constraintSet);
		
		ArrayList<ACQ_IConstraint> toremove = new ArrayList<>();
		
		for (ACQ_IConstraint c1 : disj.constraintSet) {
			
			ConstraintSet newset = constraintFactory.createSet();
			newset.add(c1);
			for (ACQ_IConstraint c2 : disj.constraintSet) {
				if (c1.equals(c2) || toremove.contains(c2)) continue;
				newset.add(c2.getNegation());
			}
			
			for (ACQ_Network mus: muses) {
				if (newset.contains(mus.getConstraints())) {
					toremove.add(c1);
					break;
				}
			}
		}
		
		for (ACQ_IConstraint c: toremove) {
			if (set.contains(c)) set.remove(c);
		}
		
		toremove = new ArrayList<>();
		ArrayList<ACQ_IConstraint> toadd = new ArrayList<>();
		
		for (int i = 2; i <= disj.constraintSet.size(); i++) {
			CombinationIterator iterator = new CombinationIterator(disj.constraintSet.size(), i);
			while (iterator.hasNext()) {
				int[] constrs = iterator.next();
				ConstraintSet subset = constraintFactory.createSet();
				for (int cid : constrs) {
					subset.add(disj.constraintSet.get_Constraint(cid));
				}
				
				ACQ_IConstraint rename = getRenameOr(subset, muses);
				if (rename != null) {
					toadd.add(rename);
					for (ACQ_IConstraint c : subset) {
						toremove.add(c);
					}
				}
			}
		}
		
		for (ACQ_IConstraint c: toremove) {
			if (set.contains(c)) set.remove(c);
		}
		
		for (ACQ_IConstraint c: toadd) {
			if (!set.contains(c)) set.add(c);
		}
		
		if (set.size() > 1) {
			return new ACQ_DisjunctionConstraint(constraintFactory, set);
		}
		else {
			return set.get_Constraint(0);
		}
	}
	
	protected ACQ_IConstraint getRenameOr(ConstraintSet set, ArrayList<ACQ_Network> muses) {
		/*
		 * Return c <=> c1 \/ c2 \/ ... \/ cn where ci \in set
		 * Or null
		 */
		
		for (ACQ_Network mus : muses) {
			if (mus.size() == set.size()+1) {
				
				boolean contained = true;
				for (ACQ_IConstraint c : set) {
					if (!mus.contains(c.getNegation())) {
						contained = false;
						break;
					}
				}
				
				if (contained) {
					ACQ_Network tmp = new ACQ_Network(mus.getFactory(), mus.getConstraints());
					
					for (ACQ_IConstraint c: set) {
						tmp.remove(c.getNegation());
					}
					assert tmp.size() == 1;
					ACQ_IConstraint rename = tmp.getConstraints().get_Constraint(0);
					
					boolean allchecked = true;
					
					for (ACQ_IConstraint c : set) {
						ACQ_Network tocheck = new ACQ_Network(mus.getFactory());
						tocheck.add(c, true);
						tocheck.add(rename.getNegation(), true);
						if (!muses.contains(tocheck)) {
							allchecked = false;
							break;
						}
					}
					
					if (allchecked) {
						return rename;
					}
				}	
			}
		}
		return null;
	}
	
	protected ACQ_IConstraint getRenameAnd(ACQ_IConstraint c1, ACQ_IConstraint c2, ArrayList<ACQ_Network> muses) {
		/*
		 * Return c <=> c1 /\ c2
		 * Or null
		 */
		
		for (ACQ_Network mus : muses) {
			if (mus.size() == 3 && mus.contains(c1) && mus.contains(c2)) {
				ACQ_Network tmp = new ACQ_Network(mus.getFactory(), mus.getConstraints());
				tmp.remove(c1);
				tmp.remove(c2);
				assert tmp.size() == 1;
				ACQ_IConstraint negrename = tmp.getConstraints().get_Constraint(0);
				ACQ_IConstraint rename = negrename.getNegation();
				
				ACQ_Network tocheck1 = new ACQ_Network(mus.getFactory());
				tocheck1.add(c1.getNegation(), true);
				tocheck1.add(rename, true);
				
				ACQ_Network tocheck2 = new ACQ_Network(mus.getFactory());
				tocheck2.add(c2.getNegation(), true);
				tocheck2.add(rename, true);
				
				if (muses.contains(tocheck1) && muses.contains(tocheck2)) {
					return rename;
				}
				
			}
		}
		return null;
	}
	
	protected ACQ_IConstraint negCommon(ACQ_DisjunctionConstraint d1, ACQ_DisjunctionConstraint d2) {
		for (ACQ_IConstraint c1 : d1.constraintSet) {
			for (ACQ_IConstraint c2 : d2.constraintSet) {
				if (c1.equals(c2.getNegation())) {
					return c1;
				}
			}
		}
		return null;
	}
	
	protected ACQ_Network simplifyNet(ACQ_Network net, ArrayList<ACQ_Network> muses) {
		ACQ_Network res = new ACQ_Network(net.constraintFactory, net.getVariables());
		res.addAll(net, true);
		boolean simplified = false;
		
		for (ACQ_IConstraint c1: net) {
			simplified = false;
			
			for (ACQ_IConstraint c2: net) {
				if (c1.equals(c2)) continue;
				
				if (!(c1 instanceof ACQ_DisjunctionConstraint)) {
					// c1 is an atomic constraint
					
					if (c2 instanceof ACQ_DisjunctionConstraint && 
							((ACQ_DisjunctionConstraint)c2).constraintSet.contains(c1.getNegation())) {
						ACQ_DisjunctionConstraint d2 = (ACQ_DisjunctionConstraint)c2; 
						ConstraintSet newset = net.getFactory().createSet(d2.constraintSet);
						newset.remove(c1.getNegation());
						
						ACQ_IConstraint newcst;
						
						if (newset.size() > 1) {
							newcst = new ACQ_DisjunctionConstraint(net.getFactory(), newset);
						}
						else {
							newcst = newset.get_Constraint(0);
						}
						
						res.add(newcst, true);
						res.remove(c2);
						simplified = true;
					}
					else {
						/*
						 * If c1 => c2 then remove c2
						 * NB: generalization of a /\ (a \/ X) -> a /\ X
						 */
						ConstraintSet isfalse = net.constraintFactory.createSet();
						if (c2 instanceof ACQ_DisjunctionConstraint) {
							ACQ_DisjunctionConstraint d2 = (ACQ_DisjunctionConstraint)c2; 
							
							for (ACQ_IConstraint cd2 : d2.constraintSet) {
								isfalse.add(cd2.getNegation());
							}
						}
						else isfalse.add(c2.getNegation());
						
						isfalse.add(c1);
						
						for (ACQ_Network mus : muses) {
							if (isfalse.contains(mus.getConstraints())) {
								res.remove(c2);
								simplified = true;
								break;
							}
						}
					}
				}
				else if (c1 instanceof ACQ_DisjunctionConstraint && c2 instanceof ACQ_DisjunctionConstraint) {
					ACQ_DisjunctionConstraint d1 = (ACQ_DisjunctionConstraint) c1;
					ACQ_DisjunctionConstraint d2 = (ACQ_DisjunctionConstraint) c2;
					
					ACQ_IConstraint rename = null;
					
					if (isNext(d1, d2)) {
						/*
						 * d1 = x1 \/ ... \/ xn \/ a
						 * d2 = x1 \/ ... \/ xn \/ b
						 * 
						 */
						ConstraintSet setd1 = d1.constraintFactory.createSet(d1.constraintSet);
						ConstraintSet setd2 = d2.constraintFactory.createSet(d2.constraintSet);
						setd1.removeAll(d2.constraintSet);
						setd2.removeAll(d1.constraintSet);
						
						assert setd1.size() == 1;
						assert setd2.size() == 1;
						
						ACQ_IConstraint cst_d1 = setd1.get_Constraint(0);
						ACQ_IConstraint cst_d2 = setd2.get_Constraint(0);
						
						if (cst_d1.equals(cst_d2.getNegation())) {
							ConstraintSet newset = net.getFactory().createSet(d1.constraintSet);
							newset.retainAll(d2.constraintSet);
							
							ACQ_IConstraint newcst;
							
							if (newset.size() > 1) {
								newcst = new ACQ_DisjunctionConstraint(net.getFactory(), newset);
							}
							else {
								newcst = newset.get_Constraint(0);
							}
							
							simplified = true;
							res.add(newcst, true);
							res.remove(c1);
							res.remove(c2);
							break;
						}
						else if ((rename = getRenameAnd(cst_d1, cst_d2, muses)) != null) {
							/*
							 * If there is c <=> a & b
							 * Then replaces d1 and d2 by x1 \/ ... \/ xn \/ c
							 * 
							 */
							
							ConstraintSet newset = net.getFactory().createSet(d1.constraintSet);
							newset.retainAll(d2.constraintSet);
							newset.add(rename);
							
							ACQ_IConstraint newcst;
							
							if (newset.size() > 1) {
								newcst = new ACQ_DisjunctionConstraint(net.getFactory(), newset);
							}
							else {
								newcst = newset.get_Constraint(0);
							}
							
							simplified = true;
							res.add(newcst, true);
							res.remove(c1);
							res.remove(c2);
							break;
						}	
					}
				}
			}
			if (simplified) {
				break;
			}
			else {
				res.add(c1, true);
			}
		}
		if (simplified) return simplifyNet(res, muses);
		else return res;
	}
	
	protected boolean isfalse(ConstraintSet set, ArrayList<ACQ_Network> muses) {
		for (ACQ_Network mus : muses) {
			if (set.contains(mus.getConstraints())) {
				return true;
			}
		}
		return false;
	}
	
	/*protected ACQ_IConstraint deduce(ACQ_IConstraint atom, ACQ_Network net, ArrayList<ACQ_Network> muses) {
		ACQ_Network res = new ACQ_Network(net.constraintFactory, net.getVariables());
		res.addAll(net, true);
		
		int ntrue = 0;
		for (ACQ_IConstraint c : net) {
			if (c instanceof ACQ_DisjunctionConstraint) {
				ACQ_DisjunctionConstraint d = (ACQ_DisjunctionConstraint) c;
				if (d.contains(atom)) {
					ntrue += 1;
					continue;
				}
				for (ACQ_IConstraint a2 : d.constraintSet) {
					ConstraintSet and_atom_a2 = net.getFactory().createSet();
					and_atom_a2.add(atom);
					and_atom_a2.add(a2);
					if (isfalse(and_atom_a2, muses)) {
						res.remove(c);
						ConstraintSet newset = net.getFactory().createSet(d.constraintSet);
						newset.remove(a2);
						
						ACQ_IConstraint add
						if (newset.size() == 1) {
							
						}
						break;
					}
				}
			}
			else if (c.equals(atom)) {
				ntrue += 1;
			}
			
		}
		return null;
	}
	
	protected ACQ_Network shaving(ACQ_Network net, ArrayList<ACQ_Network> muses) {
		
		for (ACQ_IConstraint c : net) {
			if (!(c instanceof ACQ_DisjunctionConstraint)) continue;
			ACQ_DisjunctionConstraint d = (ACQ_DisjunctionConstraint) c;
			for (ACQ_IConstraint atom : d.constraintSet) {
				ACQ_IConstraint deduced = deduce(atom, net, muses);
			}
		}
		return net;
	}*/
	
	public ACQ_Network simplifyWithSolver(ACQ_Network net) {
		if (csolv == null || known == null) return net;
		
		for (ACQ_IConstraint c : net) {
			if (!(c instanceof ACQ_DisjunctionConstraint)) continue;
			ACQ_DisjunctionConstraint d = (ACQ_DisjunctionConstraint) c;
			for (ACQ_IConstraint atom : d.constraintSet) {
				// Check if atom is useless
				ACQ_Network tosolve = new ACQ_Network(net.constraintFactory, net.getVariables());
				tosolve.addAll(net, true);
				tosolve.remove(c);
				
				ConstraintSet newdisj = net.getFactory().createSet(d.constraintSet);
				newdisj.remove(atom);
				
				if (newdisj.size() == 1) {
					tosolve.add(newdisj.get_Constraint(0), true);
				}
				else {
					tosolve.add(new ACQ_DisjunctionConstraint(net.getFactory(), newdisj) , true);
				}
				
//				tosolve.add(atom, true);
//				for (ACQ_IConstraint a2 : d.constraintSet) {
//					if (a2.equals(atom)) continue;
//					tosolve.add(a2.getNegation(), true);
//				}
//				tosolve.addAll(known.getNetwork(), true);
				
				if (csolv.equiv(net, tosolve, known.getNetwork())) {
				//if (!csolv.solve(tosolve)) {
					ACQ_Network res = new ACQ_Network(net.constraintFactory, net.getVariables());
					res.addAll(net, true);
					res.remove(c);

//					ConstraintSet newdisj = net.getFactory().createSet(d.constraintSet);
//					newdisj.remove(atom);
					
					if (newdisj.size() == 1) {
						res.add(newdisj.get_Constraint(0), true);
					}
					else {
						res.add(new ACQ_DisjunctionConstraint(net.getFactory(), newdisj) , true);
					}
					
					return res;
				}
				
				
			}
		}
		return net;
	}
	
	public ACQ_Network rmRedundantConstraint(ACQ_Network net) {
		if (csolv == null || known == null) return net;
		
		for (int i = net.size()-1; i >= 0; i--) {
			ACQ_IConstraint constr = net.getConstraints().get_Constraint(i);
			if (constr instanceof ACQ_DisjunctionConstraint) {
				for (int j = 0; j < i; j++) {
					if (((ACQ_DisjunctionConstraint)constr).contains(net.getConstraints().get_Constraint(j))) {
						net.remove(constr);
						break;
					}
				}
			}
			else if (constr instanceof ACQ_ConjunctionConstraint) {
				net.remove(constr); // We don't need ConjunctionConstraints as we can express it as conjunction of Constraints
			}
		}
		
		
		for (int i = net.size()-1; i >= 0; i--) {
			ACQ_IConstraint toremove = net.getConstraints().get_Constraint(i);
			ACQ_Network new_net = new ACQ_Network(net.constraintFactory, net, net.getVariables());
			new_net.remove(toremove);
			
			if (csolv.equiv(net, new_net, known.getNetwork())) {
				net.remove(toremove);
			}
		}
		
		return net;
	}
	
	public ACQ_Network simplify(ACQ_Network net) {
		ACQ_Network res; 
		
		while (true) {
			res = new ACQ_Network(net.getFactory(), net.getVariables());
			
			for (ACQ_IConstraint c : net) {
				if (c instanceof ACQ_DisjunctionConstraint) {
					ACQ_DisjunctionConstraint disj = (ACQ_DisjunctionConstraint) c;
					res.add(simplifyClause(disj, muses), true);		
				}
				else {
					res.add(c, true);
				}
			}
		
			res = simplifyNet(res, muses);
			
			if (res.equals(net) && 
					(res = simplifyWithSolver(res)).equals(net) 
					&& 
					(res = rmRedundantConstraint(res)).equals(net)
					){
			//if (res.equals(net)) {
				return res;
			}
			net = res;
		}
	}
	
	/*
	public ACQ_Network simplify(ACQ_Network net, ArrayList<ACQ_Network> muses) {
		ACQ_Network res; 
		
		muses.sort(new Comparator<ACQ_Network>() {
		    @Override
		    public int compare(ACQ_Network n1, ACQ_Network n2) {
		        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
		        int diff = n1.size() - n2.size();
		        if (diff > 0) return 1;
		        else if (diff == 0) return 0;
		        else return -1;
		    }});
		
		
		int nExtend = 100;
		
		
		ArrayList<ACQ_Network> candidate_results = new ArrayList<>();
		
		while (nExtend > 0) {
			res = new ACQ_Network(net.getFactory(), net.getVariables());
			
			for (ACQ_IConstraint c : net) {
				if (c instanceof ACQ_DisjunctionConstraint) {
					ACQ_DisjunctionConstraint disj = (ACQ_DisjunctionConstraint) c;
					res.add(simplifyClause(disj, muses), true);		
				}
				else {
					res.add(c, true);
				}
			}
		
			res = simplifyNet(res, muses);
			
			if (res.equals(net)) {
				candidate_results.add(res);
				res = extend_clauses(res, muses);
				nExtend--;
			}
			net = res;
		}
		
		ACQ_Network result = null;
		
		for (ACQ_Network cand : candidate_results) {
			if (result == null) { 
				result = cand;
			}
			else {
				if (result.size() > cand.size()) {
					result = cand;
				}
			}
		}
		return result;
	}*/
	
}
