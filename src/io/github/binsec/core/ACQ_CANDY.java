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

package io.github.binsec.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import constracq.core.acqconstraint.ACQ_ConjunctionConstraint;
import constracq.core.acqconstraint.ACQ_DisjunctionConstraint;
import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.Clause;
import constracq.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqconstraint.FalseConstraint;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.Unit;
import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.combinatorial.MARCO;
import io.github.binsec.core.combinatorial.DQueryGen;
import io.github.binsec.core.combinatorial.DQueryGen;
import io.github.binsec.core.combinatorial.MSSIter;
import constracq.core.learner.ACQ_Bias;
import constracq.core.learner.ACQ_Learner;
import constracq.core.learner.ACQ_Query;
import io.github.binsec.core.learner.Answer;
import constracq.tools.Chrono;
import io.github.binsec.core.tools.QueryPrinter;


public class ACQ_CANDY {

	protected ArrayList<ACQ_Query> queries = new ArrayList<>();
	protected ACQ_Bias atomic = null;
	protected ACQ_Bias knownconstraints = null;
	protected SATSolver satSolver;
	protected ACQ_ConstraintSolver solver;
	protected ACQ_Learner learner;
	protected ACQ_Network learned_dnf_MS = null;
	protected ACQ_Network learned_dnf_MG = null;
	protected ConstraintMapping mapping = null;
	protected int ndisj = 0;
	Chrono chrono;

	protected ArrayList<ACQ_Network> muses = null;
	protected Long learningtimeout;
	protected Long t0;
	protected ArrayList<ACQ_Network> strategy = null;
	protected ContradictionSet backgroundKnowledge = null;
	protected boolean verbose = false;
	protected boolean grow2 = true;
	public boolean timeouted = false;
	protected QueryPrinter qp = null;
	protected Answer mainClassif = null;
	protected int dist;
	protected ArrayList<String> learnedMSSes = new ArrayList<>();
	protected ArrayList<String> learnedNegMSSes = new ArrayList<>();
	protected Boolean switchToDQueryGen = false;
	protected ArrayList<String> learnedQueries = new ArrayList<>();


	public ACQ_CANDY(ACQ_Bias bias, ACQ_Bias known, ACQ_Learner learn, SATSolver sat, ACQ_ConstraintSolver solv) {
		learner = learn;
		mainClassif = Answer.YES;
		satSolver = sat;
		solver = solv;
		atomic = bias;
		knownconstraints = known;
		this.dist = -1;
		mapping = new ConstraintMapping();
		

		for (ACQ_IConstraint c : bias.getConstraints()) {
			String newvarname = c.getName() + c.getVariables();
			Unit unit = this.satSolver.addVar(c, newvarname);
			this.mapping.add(c, unit);

			ACQ_IConstraint neg = c.getNegation();
			assert bias.contains(neg) : "Bias should be complete";
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

		assert mapping.size() >= bias.getSize() : "mapping must contain more elements than bias";

	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setQueryPrinter(QueryPrinter qp) {
		this.qp = qp;
	}

	public void setGrow2(boolean b) {
		grow2 = b;
	}

	public void setLearningTimeout(Long tm) {
		this.learningtimeout = tm;
	}

	public void setStrat(ArrayList<ACQ_Network> strat) {
		this.strategy = strat;
	}

	public void setBackgroundKnowledge(ContradictionSet back) {
		this.backgroundKnowledge = back;
	}

	public ACQ_Network getLearnedDNFMS() {
		return this.learned_dnf_MS;
	}

	public ACQ_Network getLearnedDNFMG() {
		return this.learned_dnf_MG;
	}
	
	public ACQ_Network getNeighbor() {
		ConstraintFactory fact = this.atomic.getNetwork().getFactory();
		ACQ_Network neighbor = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		
		return neighbor;
	}
	public List<ACQ_Network> getNeighbors(ACQ_Network M){
		List<ACQ_Network> neighbors= new ArrayList<>();
		
		return neighbors;
	}

	public Boolean process(Chrono chronom) throws Exception {
		
		t0 = System.currentTimeMillis();
		chrono = chronom;
		chrono.start("pos_queries");
		chrono.start("neg_queries");

		//initialize MS, MG
		ConstraintFactory fact = this.atomic.getNetwork().getFactory();
		ACQ_Network resMG = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		ACQ_Network resMS = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		ACQ_Network empty = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		ConstraintSet conjMSS = fact.createSet();
		
		Deque<ACQ_Network> queueS = new LinkedList<>();

		MSSIter iter = null;
		this.timeouted = false;
		
		try {
			iter = new MARCO(atomic, empty, solver, satSolver, mapping, backgroundKnowledge, knownconstraints,learningtimeout, grow2, chrono);
			 
		}
		catch (TimeoutException e) {
			this.timeouted = true;
			return timeouted;
		}
				
		
		while (iter.hasNext()) {
			if (System.currentTimeMillis() - t0 > learningtimeout) {
				timeouted = true;
				break;
			}
			
			chrono.start("gen_query");
			ACQ_Query query;
			try {
				query = iter.next(); 
			} catch (TimeoutException e) {
				timeouted = true;
				break;
			}
			
			chrono.stop("gen_query");
			if (verbose)
				System.out.print(qp.toString(query));
			Answer answer;
			if (query.getClassification() == null) {
			    answer = learner.ask(query);
			} else {
			    answer = query.getClassification();
			}
			learnedQueries.add(query.toString());

			if (verbose)
				System.out.println("::" + answer);
			if (answer == Answer.YES) {
				ACQ_IConstraint toadd = addMSS(query, atomic);
				// a set that contains constraints (a constraint = a pos mss) (so a constraint is a cnf)
				conjMSS.add(toadd); 
				
				ACQ_Network M_marco=query.getM(atomic);
				queueS.addFirst(M_marco);
				learnedMSSes.add( toadd.toSmtlib());
				chrono.stop("pos_queries");
				chrono.start("pos_queries");
				
				//As soon as MARCO finds a positive query, I switch to DQueryGen.
				if(!switchToDQueryGen) {
					Formula formulaMARCO = iter.getFormula();
					ACQ_Query lastQuery = iter.getLastQuery();
					try {
						iter = new DQueryGen(queueS ,learner, formulaMARCO, Answer.YES ,atomic, empty, solver, satSolver, mapping, backgroundKnowledge, knownconstraints,
							learningtimeout, grow2, chrono);
					
					}
					catch (TimeoutException e) {
						this.timeouted = true;
						return timeouted;
					}
					// Learn the last generated query not to lose it
					if (verbose)
						System.out.print(qp.toString(lastQuery));
					Answer answer2;
					if (lastQuery.getClassification() == null) {
					    answer2 = learner.ask(lastQuery);
					} else {
					    answer2 = lastQuery.getClassification();
					}
					learnedQueries.add(lastQuery.toString());

					if (verbose)
						System.out.println("::" + answer2);
					if (answer2 == Answer.YES) {
						ACQ_IConstraint toadd2 = addMSS(lastQuery, atomic);
						conjMSS.add(toadd2); 
						queueS.addFirst(lastQuery.getM(atomic));
				        learnedMSSes.add( toadd2.toSmtlib());
						chrono.stop("pos_queries");
						chrono.start("pos_queries");
					} else if (answer2 != Answer.YES) {
						ACQ_IConstraint toadd2 = removeMSS(lastQuery, atomic);
						resMG.add(toadd2, true);
						queueS.addLast(lastQuery.getM(atomic));
						chrono.stop("neg_queries");
						chrono.start("neg_queries");
					}
					switchToDQueryGen = true;
				}
				
		        


				
			} else if (answer != Answer.YES) {
				ACQ_IConstraint toadd = removeMSS(query, atomic);
				resMG.add(toadd, true);
				queueS.addLast(query.getM(atomic));
				chrono.stop("neg_queries");
				chrono.start("neg_queries");
			}
		}
		
		//create false constraint to add it in MS (init MS)
		ACQ_IConstraint FalseConstraint = new FalseConstraint(); 
		conjMSS.add(FalseConstraint);
		
		// create a disj of pos MSSes and false constraint
		ACQ_DisjunctionConstraint disjMSS = new ACQ_DisjunctionConstraint(fact, conjMSS); 
		ACQ_IConstraint oneclause = disjMSS;
		resMS.add(oneclause, true);
		
		this.learned_dnf_MS = resMS;
		this.learned_dnf_MG = resMG;
		this.muses = iter.getMuses();
		return timeouted;

	}
	
	public ArrayList<String> getLearnedQueries(){
		return this.learnedQueries;
	}

	public ArrayList<String> getLearnedPosMSSes() {
	    return this.learnedMSSes;
	}
	
	public ArrayList<String> getLearnedNegMSSes() {
	    return this.learnedNegMSSes;
	}

	public int getLearnedMUSes() {
	    return this.muses.size();
	}
	
	protected ACQ_ConjunctionConstraint addMSS(ACQ_Query e, ACQ_Bias bias) {
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ConstraintSet set = fact.createSet();
		for (ACQ_IConstraint c : bias.getConstraints()) {
			if (c.check(e)) {
				set.add(c);
			}
		}
		return new ACQ_ConjunctionConstraint(fact, set);
	}

	protected ACQ_DisjunctionConstraint removeMSS(ACQ_Query e, ACQ_Bias bias) {
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ConstraintSet set = fact.createSet();
		for (ACQ_IConstraint c : bias.getConstraints()) {
			if (c.check(e)) {
				set.add(c.getNegation());
			}
		}
		return new ACQ_DisjunctionConstraint(fact, set);
	}
}
