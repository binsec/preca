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

package io.github.binsec.core.combinatorial;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Clause;
import constracq.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.StrlenEqConstraint;
import io.github.binsec.core.acqconstraint.Unit;
import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.SATModel;
import io.github.binsec.core.acqsolver.SATSolver;
import constracq.core.learner.ACQ_Bias;
import constracq.core.learner.ACQ_Learner;
import constracq.core.learner.ACQ_Query;
import io.github.binsec.core.learner.Answer;
import constracq.tools.Chrono;

public class DQueryGen extends MSSIter {
	
	protected ACQ_Network basenet;
	protected ACQ_Query next;
	protected ACQ_Network notSeen;
	protected ACQ_Bias bias;
	protected ACQ_Bias known; // known relations
	protected ConstraintMapping mapping;
	protected int distancemin;
	protected ACQ_ConstraintSolver solver;
	protected SATSolver satsolver;
	protected Deque<ACQ_Network> queueS = new LinkedList<>();
	protected Set<ACQ_Network> visited;
	protected ArrayList<ACQ_Network> muses = new ArrayList<>();
	protected Answer mainclassif;
	protected Formula formula= new Formula();
	protected ACQ_Network MSS;
	protected ACQ_Learner learner;
	protected Long timeout;
	protected Long t0;
	protected boolean grow2 = true;
	protected Chrono chrono;
	protected Set<ACQ_Network> positiveSeeds = new HashSet<>();

	
	public DQueryGen(Deque<ACQ_Network> queueS, ACQ_Learner learn, Answer mainclassif,ACQ_Bias bias, ACQ_Network net, 
			ACQ_ConstraintSolver solver, SATSolver satsolver, 
			ConstraintMapping mapping, ContradictionSet backgroundKnowledge, ACQ_Bias known, 
			Long timeout, boolean grow2, Chrono chrono) throws TimeoutException {
		this.MSS= queueS.poll();
		this.distancemin =0;
		this.mainclassif=mainclassif;
		this.learner=learn;
		this.timeout = timeout;
		t0 = System.currentTimeMillis();
		this.solver = solver;
		this.satsolver = satsolver;
		this.chrono = chrono;
		this.grow2 = grow2;
		this.basenet = net;
		notSeen = new ACQ_Network(net.getFactory(), bias.getVars());
		this.known = known;
		
		this.bias = filterbias(bias, basenet);
		this.mapping = mapping;
		this.formula = new Formula();
		this.formula.addClause(blockDown(MSS));
		if (backgroundKnowledge != null) this.formula.addCnf(backgroundKnowledge.toCNF());

		//addNegMUSes(bias);
		
		if (solver.solve(concat(known.getNetwork(), basenet))) {
			next();
		}
		
	}
	
	public DQueryGen(Deque<ACQ_Network> queueS, ACQ_Learner learn, Formula form, Answer mainclassif,ACQ_Bias bias, ACQ_Network net, 
			ACQ_ConstraintSolver solver, SATSolver satsolver, 
			ConstraintMapping mapping, ContradictionSet backgroundKnowledge, ACQ_Bias known, 
			Long timeout, boolean grow2, Chrono chrono) throws TimeoutException {
		this.formula=form;
		this.MSS= queueS.poll();
		this.distancemin =0;
		this.mainclassif=mainclassif;
		this.learner=learn;
		this.timeout = timeout;
		t0 = System.currentTimeMillis();
		this.solver = solver;
		this.satsolver = satsolver;
		this.chrono = chrono;
		this.grow2 = grow2;
		this.basenet = net;
		notSeen = new ACQ_Network(net.getFactory(), bias.getVars());
		this.known = known;
		
		this.bias = filterbias(bias, basenet);
		this.mapping = mapping;

		//addNegMUSes(bias);
		
		if (solver.solve(concat(known.getNetwork(), basenet))) {
			next();
		}
		
	}
	
	public void addNegMUSes(ACQ_Bias b) {
		ConstraintFactory fact = bias.getNetwork().getFactory();
		Set<ACQ_IConstraint> seen = new HashSet<>();
		for(ACQ_IConstraint c : this.bias.getConstraints()) {
			ACQ_Network initMUSes = new ACQ_Network(fact, bias.getVars() );
			if(!seen.contains(c) && !seen.contains(c.getNegation())) {
				initMUSes.add(c, grow2);
				initMUSes.add(c.getNegation(), grow2);
				if(!formula.getClauses().contains(initMUSes)) {
					this.muses.add(initMUSes);
					this.formula.addClause(blockUp(initMUSes));
					seen.add(c);
				}				
			}			
		}
	}
	
	public void setMUS(ContradictionSet muses) {
		formula.addCnf(muses.toCNF());
	}
	
	private ACQ_Bias filterbias(ACQ_Bias bias, ACQ_Network net) throws TimeoutException {
		ACQ_Bias res = bias.copy();
		for (ACQ_IConstraint c : bias.getNetwork()) {
			istimeouted(); // throws an exception if timeout reached
			if (net.contains(c)) {
				res.reduce(c);
			}
		}
		return res;
	}
	
	protected boolean istimeouted() throws TimeoutException {
		if (this.timeout != null && 
				(this.timeout <= (System.currentTimeMillis() - t0))) {
			throw new TimeoutException();
		}
		
		return false;
	}
	
	
	private ACQ_Query solve(ACQ_Network net) {
		return solver.solveQ(concat(basenet, concat(net, known.getNetwork())));
	}
	
	private boolean satisfiable(ACQ_Network net) {
		return !solver.solveQ(concat(basenet, concat(net, known.getNetwork()))).isEmpty();
	}
	
	protected ACQ_Network concat(ACQ_Network net1, ACQ_Network net2) {
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ACQ_Network res = new ACQ_Network(fact, bias.getVars());
		res.addAll(net1, true);
		res.addAll(net2, true);
		return res;
	}
	
	public boolean hasNext() {
		return next != null;
	}
	
	public Formula toFormula(ACQ_Network M) {
		Formula res = new Formula();
		for(ACQ_IConstraint c : M.getConstraints()) {
			Clause clause = new Clause();
			Unit clause_unit = mapping.get(c);
			clause.add(clause_unit);
			res.addClause(clause);
		}
		return res;
	}
	public Formula toFormula(ConstraintSet M) {
		Formula res = new Formula();
		for(ACQ_IConstraint c : M) {
			Clause clause = new Clause();
			Unit clause_unit = mapping.get(c);
			clause.add(clause_unit);
			res.addClause(clause);
		}
		return res;
	}
	
	public int distance(ACQ_Network MSS1, ACQ_Network MSS2){
		int d =0;
		for(ACQ_IConstraint c : MSS1.getConstraints()){
			if(!MSS2.contains(c)) d+=1;
		}
		return d;
	}
	
	public ACQ_Network remKnown(ACQ_Bias knowns, ACQ_Network m) {
		for(ACQ_IConstraint c : knowns.network) {
			m.remove(c);
		}
		return m;
	}
	
	public ACQ_Network remNotInBias(ACQ_Network m, ACQ_Bias b) {
		ACQ_Network res = new ACQ_Network(m.getFactory());
		for (ACQ_IConstraint c : m) {
			if (b.contains(c)) {
				res.add(c, true);
			}
		}
		return res;
		
	}
	public ACQ_Network notInMSS(ACQ_Network MSS) {
		ACQ_Network MSSnegC = new ACQ_Network(MSS.getFactory());
		for (ACQ_IConstraint c : bias.getConstraints()) {
			if(!MSS.contains(c) && !known.contains(c)) MSSnegC.add(c, true);
		}
		for (ACQ_IConstraint c : known.getConstraints()) {
			if(!MSS.contains(c) && !known.contains(c)) MSSnegC.add(c, true);
		}
		return MSSnegC;
	}
	
	public ACQ_Query next() throws TimeoutException {
		int n=0;
		chrono.start("enum_mss");
		ACQ_Query res = next; 
		while (true) {
			istimeouted();
			ACQ_Network MSSneg = notInMSS(MSS);
			SATModel model = satsolver.maxsat(formula,toFormula(MSS), toFormula(MSSneg));
			if (satsolver.isTimeoutReached()) {
				assert false : "Sat solver timeouted";
				chrono.stop("enum_mss");
				return null;
			}
			if (satsolver.isTimeoutReached()) {
				assert false: "Sat solver timeouted";
				chrono.stop("enum_mss");
				return null;
			}
			if (model != null ) {
				ACQ_Network seed = toNetwork(model);
				seed = remNotInBias(seed, bias);
				ACQ_Query query = solve(seed);
						
				if (!query.isEmpty()) {
					assert seed.size() == MSS.size() : "seed is not an MSS";
					formula.addClause(blockDown(seed));
					
					
					Answer answer = learner.ask(query);
					next = query;
					
					if(!queueS.contains(seed) ) {
						if(answer== Answer.NO) {
							queueS.addFirst(seed);
							positiveSeeds.add(seed);
						}else {
							queueS.addLast(seed); 
						}
					}
					
					
					int d = distance(MSS,seed);
					
					if (distancemin==0) { 
		                distancemin=d;
		            }
					if(d>distancemin ) {
						if(!queueS.isEmpty()) {
							MSS=queueS.poll();
							distancemin=0;
							break;
						}else {
							next = null;
							break;
						}
					}
					
					break;		
				}
				else {
					chrono.start("number_of_muses");
					ACQ_Network mus = shrink(seed, bias);
					muses.add(mus);
					formula.addClause(blockUp(mus));
					chrono.stop("number_of_muses");						
				}
			}
			else {
				next=null;
				break;
			}
		}
		chrono.stop("enum_mss");
		return res;
	}
	
	public Deque<ACQ_Network> getQueueS() {
        return queueS;
    }
	
	protected ACQ_Network grow(ACQ_Network seed, ACQ_Bias bias) throws TimeoutException {
		ACQ_Bias diff = bias.copy();
		diff.reduce(seed.getConstraints());
		
		for (ACQ_IConstraint c : diff.getNetwork()) {
			istimeouted();
			ConstraintSet cset = seed.getFactory().createSet(seed.getConstraints());
			cset.add(c);
			ACQ_Network union = new ACQ_Network(seed.getFactory(), cset);
			if (satisfiable(union)) {
				seed.add(c, true);
			}
		}
		
		if (seed.isEmpty()) {
			return null;
		}
		else {
			return seed;
		}
	}
	
	protected ACQ_Network grow(ACQ_Query query, ACQ_Network seed, ACQ_Bias bias) throws TimeoutException {
		ACQ_Bias diff = bias.copy();
		assert seed.check(query);
		
		diff.reduce(seed.getConstraints());
		
		for (ACQ_IConstraint c : diff.getNetwork()) {
			istimeouted();
			
			if (c.check(query)) {
				seed.add(c, true);
			}
		}
		
		if (seed.isEmpty()) {
			return null;
		}
		else {
			return seed;
		}
	}
	
	protected ACQ_Network shrink(ACQ_Network seed, ACQ_Bias C) throws TimeoutException {
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ACQ_Network res = new ACQ_Network(fact, bias.getVars());
		res.addAll(seed, true);
		
		for (ACQ_IConstraint constr : seed) {
			istimeouted();
			ACQ_Network seedminus = new ACQ_Network(fact, bias.getVars());
			seedminus.addAll(res, true);
			seedminus.remove(constr);
			
			if (!satisfiable(seedminus)) {
				res.remove(constr);
			}
			
		}
		
		return res;
	}
	
	private Clause blockDown(ACQ_Network mss) {
		Clause res = new Clause();
		for (ACQ_IConstraint constr : bias.getNetwork()) {
			if (!mss.contains(constr)) {
				res.add(mapping.get(constr));
			}
		}
		return res;
	}
	
	private Clause blockUp(ACQ_Network mss) {
		Clause res = new Clause();
		for (ACQ_IConstraint constr : mss) {
			Unit u = mapping.get(constr).clone();
			u.setNeg();
			res.add(u);
		}
		return res;
	}
	
	protected ACQ_Network toNetwork(SATModel model) {
		//chrono.start("to_network");
		assert(model != null);
		ACQ_Network network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
		
		for (ACQ_IConstraint constr : model.getPositive()) {
			assert constr != null;
			network.add(constr, true);
		}
		
		//chrono.stop("to_network");
		return network;
	}
	
	public ArrayList<ACQ_Network> getMuses() {
		return muses;
	}
}
