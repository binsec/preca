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
import java.util.concurrent.TimeoutException;

import constracq.core.acqconstraint.ACQ_DisjunctionConstraint;
import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import constracq.core.acqconstraint.ConstraintFactory;
import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.acqsolver.ACQ_ConstraintSolver;
import constracq.core.learner.ACQ_Bias;
import constracq.core.learner.ACQ_Learner;
import constracq.core.learner.ACQ_Query;
import constracq.tools.Chrono;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqconstraint.Unit;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.combinatorial.MARCO;
import io.github.binsec.core.combinatorial.MSSIter;
import io.github.binsec.core.learner.Answer;
import io.github.binsec.core.tools.QueryPrinter;

public class ACQ_DCA {

	protected ArrayList<ACQ_Query> queries = new ArrayList<>();
	protected ACQ_Bias atomic = null; 
	protected ACQ_Bias knownconstraints = null; 
	protected SATSolver satSolver;
	protected ACQ_ConstraintSolver solver;
	protected ACQ_Learner learner;
	protected ACQ_Network learned_dnf = null;
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
	protected ArrayList<String> distances = new ArrayList<>();
	protected ArrayList<String> learnedQueries = new ArrayList<>();
	protected ArrayList<String> learnedNegMSSes = new ArrayList<>();
	
	public ACQ_DCA(ACQ_Bias bias, ACQ_Bias known, ACQ_Learner learn, SATSolver sat, ACQ_ConstraintSolver solv) {
		learner = learn;
		satSolver = sat;
		solver = solv;
		atomic = bias;
		knownconstraints = known;
		
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
		
		assert mapping.size() >= bias.getSize(): "mapping must contain more elements than bias";
		
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
	
	public ACQ_Network getLearnedDNF() {
		return this.learned_dnf;
	}
	
	public ConstraintSet getMSS(ACQ_Query e, ACQ_Bias bias){
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ConstraintSet MSS = fact.createSet();
		for (ACQ_IConstraint c : bias.getConstraints()) {
			if (c.check(e)) {
				MSS.add(c);
			}
		}
		return MSS;
	}
	
	public ArrayList<String> getDistances(){
		return this.distances;
	}
	
	public Boolean process(Chrono chronom) throws Exception {
		t0 = System.currentTimeMillis();
		chrono = chronom;
		chrono.start("pos_queries");
		chrono.start("neg_queries");
		//chrono.start("pos_queries_gen");
		
		ConstraintFactory fact = this.atomic.getNetwork().getFactory();
		ACQ_Network res = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		ACQ_Network empty = new ACQ_Network(fact, this.atomic.getNetwork().getVariables());
		
		MSSIter iter = new MARCO(atomic, empty, solver, satSolver, mapping, 
				backgroundKnowledge, knownconstraints, learningtimeout, grow2, chrono);
		
		ACQ_Network newMSS = null;
		ACQ_Network prevMarcoMSS = null;
		while (iter.hasNext()) {
			if (System.currentTimeMillis() - t0 > learningtimeout) {
				timeouted = true;
				return false;
			}
			chrono.start("gen_query");
			ACQ_Query query;
			try {
				query = iter.next();
			}
			catch (TimeoutException e) {
				timeouted = true;
				return false;
			}
			chrono.stop("gen_query");
			
			if (verbose) {
				System.out.print(qp.toString(query));
			}
			learnedQueries.add(query.toString());
			Answer answer = learner.ask(query);
			if (verbose) System.out.println("::" + answer);
			queries.add(query);
			
			newMSS = query.getM(atomic);
			if(prevMarcoMSS != null) {
				//calculer la distance 
				int d = distance(newMSS,prevMarcoMSS);
				if(answer == Answer.YES ) {
					distances.add("+ " + d);
				}else distances.add("- " + d);
			}else {
				prevMarcoMSS = newMSS;  
			}
			
			if (answer != Answer.YES) {
				ACQ_IConstraint toadd = removeMSS(query, atomic);
				res.add(toadd, true);
				chrono.stop("neg_queries");
				chrono.start("neg_queries");
				ACQ_Network NegMSS = query.getM(atomic);
				learnedNegMSSes.add(NegMSS.toSmtlib());
			}else if (answer == Answer.YES)  {
				
				/*for (ACQ_IConstraint c : atomic.getConstraints()) {
					if (c.check(query)) {
						System.out.println(c);
					}
				}*/
				//chrono.stop("pos_queries_gen");
				chrono.stop("pos_queries");
				chrono.start("pos_queries");
			}
			prevMarcoMSS = newMSS;
		}
		this.timeouted = false;
		this.learned_dnf = res;
		this.muses = iter.getMuses();
		return false;
		
	}
	
	public int distance(ACQ_Network MSS1, ACQ_Network MSS2){
		int d =0;
		for(ACQ_IConstraint c : MSS1.getConstraints()){
			if(!MSS2.contains(c)) d+=1;
		}
		return d;
	}
	public ArrayList<String> getLearnedQueries(){
		return this.learnedQueries;
	}

	public ArrayList<ACQ_Query> getQueries(){
		return queries;
	}

	public Integer getNumberQueries(Answer classification){
		int countQueries =0;
		for(ACQ_Query query : queries){
			if(query.getClassification()==classification) countQueries++;
		}
		return countQueries;
	}
	public ArrayList<String> getLearnedNegMSSes() {
	    return this.learnedNegMSSes;
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


