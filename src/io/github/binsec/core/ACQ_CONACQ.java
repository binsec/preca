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

package io.github.binsec.core;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import io.github.binsec.core.acqconstraint.ACQ_ConjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_DisjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Clause;
import io.github.binsec.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.Contradiction;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.Unit;
import io.github.binsec.core.acqconstraint.ConstraintFactory.ConstraintSet;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.ACQ_IDomain;
import io.github.binsec.core.acqsolver.SATModel;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Learner;
import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.Answer;
import io.github.binsec.core.learner.AnswerTimeoutException;
import io.github.binsec.core.tools.Chrono;
import io.github.binsec.core.tools.QueryPrinter;

public class ACQ_CONACQ {
	protected ACQ_Bias bias;
	protected ACQ_Bias bias_minus;
	
	protected ACQ_Learner learner;
	protected ACQ_ConstraintSolver constrSolver;
	protected ACQ_IDomain domain;
	protected CNF T;
	protected boolean verbose = false;
	protected ConstraintMapping mapping;
	protected SATSolver satSolver;
	protected ConstraintFactory constraintFactory;
	protected ACQ_Network learned_network;
	protected ACQ_Bias knownconstraints = null;
	protected ArrayList<ACQ_Network> strategy = null;
	protected ContradictionSet backgroundKnowledge = null;
	protected Chrono chrono;
	protected Long learningtimeout;
	protected Long t0;
	public boolean timeouted = false;
	protected ArrayList<ACQ_Query> asked = new ArrayList<>();
	protected ArrayList<String> asked_debug = new ArrayList<>();
	protected ArrayList<ACQ_Query> preprocanswered = new ArrayList<>();
	protected ACQ_CellVariable[] vars;
	protected boolean active = true;
	protected boolean weighted = false;
	protected ACQ_Query collapsingQuery;
	protected QueryPrinter qp;

	public ACQ_CONACQ(ACQ_Learner learner, ACQ_Bias bias, SATSolver sat, ACQ_ConstraintSolver solv) {
		this.bias = bias;
		this.constraintFactory = bias.network.getFactory();
		this.learner = learner;
		this.satSolver = sat;
		this.constrSolver = solv;
		this.domain = solv.getDomain();
		this.mapping = new ConstraintMapping();
		
		for (ACQ_IConstraint c : bias.getConstraints()) {
			String newvarname = c.getName() + c.getVariables();
			Unit unit = this.satSolver.addVar(c, newvarname);
			this.mapping.add(c, unit);
			
			ACQ_IConstraint neg = c.getNegation();
			if (!bias.contains(neg)) {
				newvarname = neg.getName() + neg.getVariables();
				unit = this.satSolver.addVar(neg, newvarname);
				this.mapping.add(neg, unit);
			}
		}
		assert mapping.size() >= bias.getSize(): "mapping must contain more elements than bias";
		filter_conjunctions();
		this.bias_minus = bias.copy();
		
	}
	
	public ACQ_CONACQ(ACQ_Learner learner, ACQ_Bias bias, ACQ_Bias known, SATSolver sat, ACQ_ConstraintSolver solv) {
		this.bias = bias;
		this.knownconstraints = known;
		this.constraintFactory = bias.network.getFactory();
		this.learner = learner;
		this.satSolver = sat;
		this.constrSolver = solv;
		this.domain = solv.getDomain();
		this.mapping = new ConstraintMapping();
		
		for (ACQ_IConstraint c : bias.getConstraints()) {
			String newvarname = c.getName() + c.getVariables();
			Unit unit = this.satSolver.addVar(c, newvarname);
			this.mapping.add(c, unit);
			
			ACQ_IConstraint neg = c.getNegation();
			if (!bias.contains(neg)) {
				newvarname = neg.getName() + neg.getVariables();
				unit = this.satSolver.addVar(neg, newvarname);
				this.mapping.add(neg, unit);
			}
		}
		
		for (ACQ_IConstraint c : knownconstraints.getConstraints()) {
			if (!bias.contains(c)) {
				String newvarname = c.getName() + c.getVariables();
				Unit unit = this.satSolver.addVar(c, newvarname);
				this.mapping.add(c, unit);
			}
			
			
			ACQ_IConstraint neg = c.getNegation();
			if (!bias.contains(neg)) {
				String newvarname = neg.getName() + neg.getVariables();
				Unit unit = this.satSolver.addVar(neg, newvarname);
				this.mapping.add(neg, unit);
			}
		}
		
		assert mapping.size() >= bias.getSize(): "mapping must contain more elements than bias";
		filter_conjunctions();
		this.bias_minus = bias.copy();
		
	}
	
	public void setQueryPrinter(QueryPrinter qp) {
		this.qp = qp;
	}
	
	public void setWeighted(boolean b) {
		weighted = b;
	}

	protected void filter_conjunctions() {
		for (Unit unit : mapping.values()) {
			ACQ_IConstraint c = unit.getConstraint();
			if (c instanceof ACQ_ConjunctionConstraint) {
				bias.reduce(c);
			}
		}
	}
	
	public void setCellVariables(ACQ_CellVariable[] list) {
		vars = list;
	}
	
	public void setPreprocAnswered(ArrayList<ACQ_Query> list) {
		this.preprocanswered = list;
	}
	
	public void setPassive() {
		this.active = false;
	}
	
	public void setLearningTimeout(Long tm) {
		this.learningtimeout = tm;
	}
	
	public void setStrat(ArrayList<ACQ_Network> strat) {
		this.strategy = strat;
	}
	
	public void setChrono(Chrono chrono) {
		this.chrono = chrono;
	}
	
	public void setBackgroundKnowledge(ContradictionSet back) {
		this.backgroundKnowledge = back;
	}
	
	public ContradictionSet getBackgroundKnowledge() {
		return this.backgroundKnowledge;
	}
	
	public ArrayList<ACQ_Query> getAskedQueries() {
		return this.asked;
	}
	
	public ArrayList<ACQ_Query> getAskedQueriesPos() {
		ArrayList<ACQ_Query> pos = new ArrayList<>();
		for (ACQ_Query e : asked) {
			if (e.isPositive()) {
				pos.add(e);
			}
		}
		return pos;
	}
	
	public ArrayList<ACQ_Query> getAskedQueriesNeg() {
		ArrayList<ACQ_Query> neg = new ArrayList<>();
		for (ACQ_Query e : asked) {
			if (e.isNegative()) {
				neg.add(e);
			}
		}
		return neg;
	}
	
	public ACQ_Bias getBias() {
		return bias;
	}

	public ACQ_Network getLearnedNetwork() {
		return learned_network;
	}
	
	public ACQ_Query getCollapsingQuery() {
		if (collapsingQuery == null) collapsingQuery = T.getInconsistency();
		assert collapsingQuery.isNegative();
		return collapsingQuery;
	}
	
	public void setVerbose(boolean verbose) {

		this.verbose = verbose;
	}
	
	protected boolean istimeouted() throws TimeoutException {
		if (this.learningtimeout != null && 
				(this.learningtimeout <= (System.currentTimeMillis() - this.t0))) {
			throw new TimeoutException();
		}
		
		return false;
	}
	
	protected boolean istimeouted_nothrow() {
		try {
			istimeouted();
			return false;
		}
		catch (TimeoutException e) {
			return true;
		}
	}
	
	public ACQ_Query query_gen(CNF T, ContradictionSet N) throws Exception {
		ACQ_Query q = new ACQ_Query();
		Clause alpha = new Clause();
		int epsilon = 0;
		int t = 0;
		boolean skip_buildformula = false;
		Formula form = null;
		Boolean splittable = null;
		while (!istimeouted() && q.isEmpty() && !T.isMonomial()) {
			if (!skip_buildformula) {
				Clause newalpha;
				
				if (alpha.isEmpty() && !(newalpha = T.getUnmarkedNonUnaryClause()).isEmpty()) {
					alpha = newalpha;
					epsilon = 0;
					t = 1; // Optimal in expectation
					//t = Math.max(alpha.getSize() -1, 1); // Optimistic
				}
				splittable = (!alpha.isEmpty() && ((t+epsilon < alpha.getSize()) || (t - epsilon > 0)));
				chrono.start("build_formula");
				//form = BuildFormula(splittable, T, N, filteralpha(alpha), t, epsilon);
				form = BuildFormula(splittable, T, N, alpha, t, epsilon);
				chrono.stop("build_formula");
				assert(form != null);
				form.addCnf(N.toCNF());
			}
			skip_buildformula = false;
			//System.out.println("Start SAT solver");
			SATModel model = satSolver.solve(form);
			//System.out.println("Stop SAT solver");
			if (satSolver.isTimeoutReached()) {
				assert(q.isEmpty());
				return q; // Collapse
			}
			
			if (model == null) {
				assert !alpha.isEmpty() : "invariant: alpha should not be empty";
				
				if (splittable) {
					epsilon += 1;
				} else {
					T.remove(alpha);
					for (Unit unit : alpha) {
						assert !unit.isNeg() : "literals in alpha should not be negated";
						chrono.stop("first_constr_learned");
						Clause fromalpha = new Clause(unit);
						fromalpha.setOriginQuery(alpha.getOriginQuery());
						T.add(fromalpha);
						T.unitPropagate(unit, chrono);
						bias_minus.reduce(unit.getConstraint().getNegation());
					}
					alpha = new Clause(); //Empty clause 
					assert alpha.isEmpty() : "The empty clause should be empty";
				}
			}
			else {
				ACQ_Network network = toNetwork(model);
				//System.out.println("Start CP Solve");
				q = constrSolver.solveQ(network);
				//System.out.println("Stop CP Solve");
				
				if (constrSolver.isTimeoutReached()) {
					assert q.isEmpty() : "Timeout reached but q is not empty";
					return q;
				}
				
				if (q.isEmpty()) {
					//System.out.println("Start QuickXplain: " + network.toString());
					network = filternet(network);
					Contradiction unsatCore = quickExplain(new ACQ_Network(constraintFactory, bias.network.getVariables()), network);
					//System.out.println("Stop QuickXplain");
					//System.out.println(unsatCore.toNetwork());
					assert (unsatCore != null && !unsatCore.isEmpty());
					N.add(unsatCore);
					/*if (!alpha.isEmpty()) {
						// Here splittalbe, alpha (!= empty) and T does not change (only N change) 
						// so BuildFormula will return the same formula 
						skip_buildformula = true; // Here splittalbe, alpha (!= empty) and T does not change (only N change) so BuildFormula 
					}*/
				}
				else {
					if (!splittable && !alpha.isEmpty())
						alpha.mark();
				}
				
			}
			
		}
		if (q.isEmpty())
			q = irredundantQuery(T);
		return q;
	}
	
	protected ACQ_Network filternet(ACQ_Network net) {
		ACQ_Network res = new ACQ_Network(constraintFactory, bias.getVars());
		for (ACQ_IConstraint constr : net) {
			boolean toadd = true;
			
			if (constr instanceof ACQ_DisjunctionConstraint) {
				ACQ_DisjunctionConstraint disj  = (ACQ_DisjunctionConstraint) constr;
				for (ACQ_IConstraint added : res) {
					if (disj.contains(added)) {
						toadd = false;
						break;
					}
				}
			}
			
			if (constr instanceof ACQ_ConjunctionConstraint) {
				ACQ_ConjunctionConstraint conj = (ACQ_ConjunctionConstraint) constr;
				boolean included = true;
				for (ACQ_IConstraint subconj : conj.getConstraints()) {
					if (!res.contains(subconj)) {
						included = false;
						break;
					}
				}
				
				toadd = !included;
			}
			
			if (toadd) {
				res.add(constr, true);
			}
		}
		return res;
	}
	
	protected Formula BuildFormula(Boolean splittable, CNF T, ContradictionSet N, Clause alpha, int t, int epsilon) {
		Formula res =  new Formula();
		if (!alpha.isEmpty()) {
			res.addCnf(T);
			// No need to remove unary negative as it is never added to T
			for(ACQ_IConstraint c : bias_minus.getConstraints()) {	
				// No need to check if T contains unary negative as it is never added to T 
				boolean cont = alpha.contains(c);
				if (splittable && !cont && !alpha.contains(c.getNegation())) {
					res.addClause(new Clause(mapping.get(c)));
				}
				if (cont) {
					Clause newcl = new Clause();
					newcl.add(mapping.get(c));
					newcl.add(mapping.get(c.getNegation()));
					res.addClause(newcl);
					if (weighted)
						res.addMinimization(newcl);
				}
			}
			
			int lower, upper;
			if (splittable && !weighted) {
				lower = Math.max(alpha.getSize() - t - epsilon, 1);
				upper = Math.min(alpha.getSize() - t + epsilon, alpha.getSize() -1);
			}
			else {
				lower = 1;
				upper = alpha.getSize() - 1;
			}
			
			res.setAtLeastAtMost(alpha, lower, upper); // atLeast and atMost are left symbolic in order to let the solver encode it at will
		}
		else {
			CNF F = T.clone();
			Clause toadd = new Clause();
			// All constraints not in bias_minus are set to false
			for (ACQ_IConstraint constr : bias_minus.getConstraints()) {
				if (isUnset(constr, T, N)) {
					//constr is unset
					Unit toremove = mapping.get(constr.getNegation()).clone();
					toremove.setNeg();
					
					F.removeIfExists(new Clause(toremove)); // TODO check if can be removed
					//F.remove(new Clause(toremove));
					
					toadd.add(mapping.get(constr.getNegation()));	
				}
			}
			
			assert !toadd.isEmpty() : "toadd should not be empty";
			F.add(toadd);
			res.addCnf(F);
		}
		return res;
	}
	
	protected boolean isUnset(ACQ_IConstraint constr, CNF T, ContradictionSet N) {
		Unit unit = mapping.get(constr);
		Unit neg = unit.clone();
		neg.setNeg();
		
		CNF tmp1 = T.clone();
		tmp1.concat(N.toCNF());
		CNF tmp2 = tmp1.clone();
		
		tmp1.add(new Clause(unit));
		tmp2.add(new Clause(neg));
		
		return satSolver.solve(tmp1) != null && satSolver.solve(tmp2) != null;
	}
	
	protected ACQ_Query irredundantQuery(CNF T) {
		assert(T.isMonomial());
		ACQ_Network learned = new ACQ_Network(constraintFactory, bias.getVars(), constraintFactory.createSet(T.getMonomialPositive()));
		learned.addAll(knownconstraints.getNetwork(), true);
		long startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 100) { // Until 0.1 second timeout
			ACQ_Query q = constrSolver.solveQ(learned);
			
			ConstraintSet kappa = bias_minus.getKappa(q);
			for(Clause clause : T) { // TODO check if we cannot remove the loop
				Unit unit = clause.get(0);
				if (unit.isNeg())
					kappa.remove(unit.getConstraint());
			}
			
			if(kappa.size() > 0) {
				return q;
			}
		}
		
		for(ACQ_IConstraint c : bias_minus.getConstraints()) {
			if (!learned.contains(c)) {
				learned.add(c.getNegation(), true);
				ACQ_Query q = constrSolver.solveQ(learned);
				learned.remove(c.getNegation());
				if(!q.isEmpty())
					return q;
				else {
					T.add(new Clause(mapping.get(c)));
					// Here I do not add a originQuery as another clause is redundant 
				}
			}
		}
		return new ACQ_Query();
	}
	
	protected Contradiction quickExplain(ACQ_Network b, ACQ_Network network) {
		chrono.start("quick_explain");
		Contradiction result;
		if (network.size() == 0) {
			result = new Contradiction(new ACQ_Network());
		}
		else {
			ACQ_Network res = quick(b, b, network);
			assert !isConsistent(res) : "quickExplain must returned inconsistent networks";
			result = new Contradiction(res);
		}
		chrono.stop("quick_explain");
		return result;
	}
	
	protected ACQ_Network quick(ACQ_Network b, ACQ_Network delta, ACQ_Network c) {
		//System.out.println("In");
		if(delta.size() != 0 && !isConsistent(b)) {
			return new ACQ_Network(c.getFactory(), c.getFactory().createSet());
		}
		if(c.size() == 1) 
			return c;
		
		ACQ_Network c1 = new ACQ_Network(constraintFactory, bias.network.getVariables()); 
		ACQ_Network c2 = new ACQ_Network(constraintFactory, bias.network.getVariables());
		
		int i=0;
		for (ACQ_IConstraint constr : c.getConstraints()) {
			if(i < c.size()/2) {
				c1.add(constr, true);
			}
			else {
				c2.add(constr, true);
			}
			i+=1;
		}
		
		ACQ_Network b_union_c1 = new ACQ_Network(constraintFactory, b, bias.network.getVariables());
		b_union_c1.addAll(c1, true);
		ACQ_Network delta2 = quick(b_union_c1, c1, c2);
		
		ACQ_Network b_union_delta2 = new ACQ_Network(constraintFactory, b, bias.network.getVariables());
		b_union_delta2.addAll(delta2, true);
		ACQ_Network delta1 = quick(b_union_delta2, delta2, c1);
		
		delta1.addAll(delta2, true);
		return delta1;
	}

	protected Boolean isConsistent(ACQ_Network network) {
		return constrSolver.solve(network);
	}
	
	protected ACQ_Query preproc_query_gen() {
		if (this.strategy != null && this.strategy.size() > 0) {
			ACQ_Network s = this.strategy.get(0);
			this.strategy.remove(0);
			s.addAll(knownconstraints.getNetwork(), true);
			return constrSolver.solveQ(s);
		}
		else {
			return new ACQ_Query();
		}
	}
	
	protected boolean preprocess(CNF T, ContradictionSet N) throws Exception {
		boolean collapse = false;
		boolean stop = false;
		ArrayList<ACQ_Query> neg = new ArrayList<>();
		
		/*
		 * Passive learning
		 */
		for (ACQ_Query membership_query : preprocanswered) {
			if (istimeouted_nothrow()) {
				this.timeouted = true;
				break;
			}
			if (verbose) System.out.println("[INFO] replay query " + qp.toString(membership_query) + "::" + membership_query.isPositive());
			ConstraintSet kappa = bias_minus.getKappa(membership_query);
			//assert kappa.size() > 0;
			if (kappa.size() == 0 && membership_query.isNegative()) {
				collapse = true;
				collapsingQuery = membership_query;
				return collapse;
			}
			if(kappa.size() > 0) {
				
				boolean answer = membership_query.isPositive();
				asked.add(membership_query);
				
				if (answer) {
					for (ACQ_IConstraint c : kappa) {
						Unit unit = mapping.get(c).clone();
						unit.setNeg();
						T.unitPropagate(unit, chrono);
					}
					bias_minus.reduce(kappa);
				}
				else {
					// make sure to not add two times the same clause
					boolean toadd = true;
					for (ACQ_Query old : neg) {
						ConstraintSet oldkappa = bias_minus.getKappa(old);
						if (oldkappa.equals(kappa)) {
							toadd = false;
							break;
						}
					}
					
					if (toadd) {
						if (kappa.size() == 1) {
							chrono.stop("first_constr_learned");
							ACQ_IConstraint c = kappa.get_Constraint(0);
							Unit unit = mapping.get(c).clone();
							T.unitPropagate(unit, chrono);
							Clause cl = new Clause(unit);
							cl.setOriginQuery(membership_query);
							T.addChecked(cl);
							
							// Remove negation
							bias_minus.reduce(c.getNegation());
							unit = mapping.get(c.getNegation()).clone();
							unit.setNeg();
							T.unitPropagate(unit, chrono);
						}
						else {
							Clause disj = new Clause();
							for (ACQ_IConstraint c: kappa) {
								Unit unit = mapping.get(c).clone();
								disj.add(unit);
							}
							disj.setOriginQuery(membership_query);
							T.addChecked(disj);
						}
						neg.add(membership_query);
					}
				}
			}
		}
		
		while (!stop) {
			if (istimeouted_nothrow()) {
				this.timeouted = true;
				break;
			}
			if (bias_minus.getConstraints().isEmpty())
				break;
			
			ACQ_Query membership_query = preproc_query_gen();
			assert membership_query != null : "membership query can't be null";
			
			
			if(constrSolver.isTimeoutReached() || satSolver.isTimeoutReached()) {
				collapse = true;
				break;
			}
		
			if (membership_query.isEmpty()) {
				stop = true;
			}
			else {
				if (verbose) System.out.print("[PREPROC] " + qp.toString(membership_query));
				Answer answer = learner.ask(membership_query);
				if (answer == Answer.UKN) {
					throw new AnswerTimeoutException("Answer timeouted");
				}
				ConstraintSet kappa = bias_minus.getKappa(membership_query);
				if (verbose) System.out.println("::" + membership_query.isPositive());
				asked.add(membership_query);
				//assert kappa.size() > 0;
				
				if (kappa.size() == 0 && membership_query.isNegative()) {
					collapse = true;
					collapsingQuery = membership_query;
					return collapse;
				}
				
				if(kappa.size() > 0) {
					if (answer == Answer.YES) {
						for (ACQ_IConstraint c : kappa) {
							Unit unit = mapping.get(c).clone();
							unit.setNeg();
							T.unitPropagate(unit, chrono);
						}
						bias_minus.reduce(kappa);
					}
					else {
						// make sure to not add two times the same clause
						boolean toadd = true;
						for (ACQ_Query old : neg) {
							ConstraintSet oldkappa = bias_minus.getKappa(old);
							if (oldkappa.equals(kappa)) {
								toadd = false;
								break;
							}
						}
						
						if (toadd) {
							if (kappa.size() == 1) {
								chrono.stop("first_constr_learned");
								ACQ_IConstraint c = kappa.get_Constraint(0);
								Unit unit = mapping.get(c).clone();
								T.unitPropagate(unit, chrono);
								bias_minus.reduce(c.getNegation());
								Clause disj = new Clause();
								disj.add(unit);
								disj.setOriginQuery(membership_query);
								T.addChecked(disj);
								
								// Remove negation
								bias_minus.reduce(c.getNegation());
								unit = mapping.get(c.getNegation()).clone();
								unit.setNeg();
								T.unitPropagate(unit, chrono);
							}
							else {
								Clause disj = new Clause();
								for (ACQ_IConstraint c: kappa) {
									Unit unit = mapping.get(c).clone();
									disj.add(unit);
								}
								disj.setOriginQuery(membership_query);
								T.addChecked(disj);
							}
							neg.add(membership_query);
						}
					}
				}
			}
		}
		
		return collapse;
	}
	
	public boolean process() throws Exception {
		this.t0 = System.currentTimeMillis();
		
		boolean convergence = false;
		boolean collapse = false;
		
		T = new CNF();
		ContradictionSet N;
		if (this.backgroundKnowledge == null) {
			N = new ContradictionSet(constraintFactory, bias.network.getVariables(), mapping);
		} else {
			N = this.backgroundKnowledge;
		}
		
		// assert(learned_network.size()==0);
		chrono.start("total_acq_time");
		chrono.start("first_constr_learned");
		collapse = preprocess(T, N);
		
		while (active && !(collapse || convergence)) {
			if (istimeouted_nothrow()) {
				this.timeouted = true;
				break;
			}
			
			if (bias_minus.getConstraints().isEmpty())
				break;
			
			ACQ_Query membership_query;
			try {
				chrono.start("gen_query");
				membership_query = query_gen(T, N);
				chrono.stop("gen_query");
			}
			catch (TimeoutException e) {
				this.timeouted = true;
				chrono.stop("gen_query");
				break;
			}
			assert membership_query != null : "membership query can't be null";
			
			
			if(constrSolver.isTimeoutReached() || satSolver.isTimeoutReached()) {
				collapse = true;
				break;
			}
		
			if (membership_query.isEmpty()) {
				convergence = true;
			}
			else {
				if (verbose) System.out.print(qp.toString(membership_query));
				Answer answer = learner.ask(membership_query);
				if (answer == Answer.UKN) {
					throw new AnswerTimeoutException("Answer timeouted");
				}
				ConstraintSet kappa = bias_minus.getKappa(membership_query);
				assert kappa.size() > 0;
				if (verbose) System.out.println("::" + membership_query.isPositive());
				asked.add(membership_query);
				assert !asked_debug.contains(membership_query.toString());
				asked_debug.add(membership_query.toString());
				
				if(answer == Answer.YES) {
					for (ACQ_IConstraint c : kappa) {
						Unit unit = mapping.get(c).clone();
						unit.setNeg();
						T.unitPropagate(unit, chrono);
					}
					bias_minus.reduce(kappa);
				}
				else {
					if (kappa.size() == 1) {
						chrono.stop("first_constr_learned");
						ACQ_IConstraint c = kappa.get_Constraint(0);
						Unit unit = mapping.get(c).clone();
						T.unitPropagate(unit, chrono);
						Clause unary = new Clause(unit);
						unary.setOriginQuery(membership_query);
						T.add(unary);
						
						// Remove negation
						bias_minus.reduce(c.getNegation());
						unit = mapping.get(c.getNegation()).clone();
						unit.setNeg();
						T.unitPropagate(unit, chrono);
						
					}
					else {
						Clause disj = new Clause();
						for (ACQ_IConstraint c: kappa) {
							Unit unit = mapping.get(c).clone();
							disj.add(unit);
						}
						disj.setOriginQuery(membership_query);
						T.add(disj);
						//T.unitPropagate(chrono);
					}
				}
			}
		}
		chrono.stop("total_acq_time");
		if (this.timeouted) {
			if (verbose) System.out.println("[WARNING] Timeouted");
			/*if (verbose) System.out.print("[INFO] Extract network from T: ");
			learned_network = new ACQ_Network(constraintFactory, bias.getVars());
			for (ACQ_IConstraint constr: bias_minus.getConstraints()) {
				if (!isUnset(constr, T, N)) {
					learned_network.add(constr, true);
				}
			}
			if (verbose) System.out.println("Done");*/
		}
		else if (!collapse) {
			if (verbose) System.out.print("[INFO] Extract network from T: ");
			CNF F = T.clone();
			for (ACQ_IConstraint c : bias.getConstraints()) {
				if (bias_minus.contains(c)) continue;
				Unit u = mapping.get(c).clone();
				u.setNeg();
				F.add(new Clause(u));
			}
			
			T = F;
			
			SATModel model = satSolver.solve(F);
			if (verbose) System.out.println("Done");
			learned_network = model != null ? toNetwork(model) : null;
		}

		return !collapse;
	}
	
	protected ACQ_Network toNetwork(SATModel model) throws Exception {
		chrono.start("to_network");
		assert(model != null);
		ACQ_Network network = new ACQ_Network(constraintFactory, bias.getVars());
		
		for (ACQ_IConstraint constr : model.getPositive()) {
			assert constr != null;
			network.add(constr, true);
		}
		
		chrono.stop("to_network");
		return network;
	}
	
	protected String formatQuery(ACQ_Query query) {
		String res = "[";
		for (int index = 0; index < vars.length; index ++) {
			ACQ_CellVariable var = vars[index];
			String value = String.valueOf(query.getValue(index));
			
			String type = "";
			switch (var.getType()) {
				case INT:
					type = "int";
					break;
				case UINT:
					type = "uint";
					break;
				case PTR:
					type = "void*";
					if (query.getValue(index) == 0) {
						value = "\"NULL\"";
					}
					else {
						value = "\"@" + (query.getValue(index) -1) + "\"";
					}
					break;
				default:
					assert false: "Unknown type";	
			}
			
			res += "{ \"type\": \"" + type + "\",  \"val\": " + value + "}, ";
		}
		res += "]";
		return res;
	}
	
}
