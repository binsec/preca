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

package io.github.binsec.core.combinatorial;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqconstraint.ConstraintFactory.ConstraintSet;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Query;

public class DAA extends MSSIter {
	protected ArrayList<ACQ_Network> mcses = new ArrayList<>();
	protected ArrayList<ACQ_Network> muses = new ArrayList<>();
	protected ACQ_Network seed;
	protected ACQ_Network basenet;
	protected boolean haveSeed;
	protected ACQ_Bias bias;
	protected ACQ_Network next;
	protected ACQ_ConstraintSolver solver;
	
	protected Long timeout;
	protected Long t0;
	
	public DAA(ACQ_Bias bias, ACQ_Network net, ACQ_Bias known, ACQ_ConstraintSolver solver, Long timeout) throws TimeoutException {
		this.timeout = timeout;
		t0 = System.currentTimeMillis();
		this.solver = solver;
		
		haveSeed = true;
		this.bias = filterbias(bias, net);
		basenet = net;
		basenet.addAll(known.getNetwork(), true);
		
		seed = new ACQ_Network(this.bias.getNetwork().getFactory());
		next = grow(seed, this.bias);
		
	}
	
	public ACQ_Bias filterbias(ACQ_Bias bias, ACQ_Network net) throws TimeoutException {
		ACQ_Bias res = bias.copy();
		for (ACQ_IConstraint c : bias.getNetwork()) {
			istimeouted(); // throws an exection if timeout reached
			if (net.contains(c)) {
				res.reduce(c);
			}
			else {
				ACQ_Network all = new ACQ_Network(net.getFactory(), net, net.getVariables());
				all.addAll(net, true);
				all.add(c, true);
				if (!solver.solve(all)) {
					res.reduce(c);
				}
				else {
					all = new ACQ_Network(net.getFactory(), net, net.getVariables());
					all.addAll(net, true);
					all.add(c.getNegation(), true);
					if (!solver.solve(all)) {
						res.reduce(c);
					}
				}
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
	
	public boolean hasNext() {
		return next != null;
	}
	
	protected boolean isMSS(ACQ_Network net, ACQ_Bias bias) {
		for (ACQ_IConstraint c : bias.getNetwork()) {
			if (net.contains(c)) continue;
			ConstraintFactory fact = net.getFactory();
			ACQ_Network all = new ACQ_Network(fact, net, net.getVariables());
			all.add(c, true);
			if (solver.solve(all)) {
				return false;
			}
		}
		return true;
	}
	
	public ACQ_Query next() throws TimeoutException {
		ACQ_Network mss = getComplete(nextMSS());
		assert isMSS(mss, bias);
		return solver.solveQ(mss);
	}
	
	public ACQ_Network nextMSS() throws TimeoutException {
		
		ACQ_Bias diff = bias.copy();
		diff.reduce(next.getConstraints());
		mcses.add(diff.getNetwork());
		haveSeed = false;
		
		ArrayList<ACQ_Network> toiter = hittingSets(dup(mcses));
		toiter.removeAll(muses);
		for (ACQ_Network candidate : toiter) {
			istimeouted();
			boolean sat = satisfiable(candidate);
			if (sat) {
				seed = candidate;
				haveSeed = true;
				break;
			}
			else {
				muses.add(candidate);
			}
		}
		
		ACQ_Network res = next;
		next = haveSeed? grow(seed, bias) : null;
		return res;
		
	}
	
	public ACQ_Network getComplete(ACQ_Network value) {
		ConstraintFactory fact = value.getFactory();
		ACQ_Network all = new ACQ_Network(fact, value, value.getVariables());
		all.addAll(this.basenet, true);
		return all;
	}
	
	public boolean satisfiable(ACQ_Network net) {
		return solver.solve(getComplete(net));
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
	
	protected <T> ArrayList<T> dup(ArrayList<T> l) {
		ArrayList<T> res = new ArrayList<>();
		for (T e : l) {
			res.add(e);
		}
		return res;
	}
	
	protected ArrayList<ACQ_Network> hittingSets(ArrayList<ACQ_Network> sets) throws TimeoutException {
		assert sets.size() > 0;
		ACQ_Network elem = sets.remove(0);
		ArrayList<ACQ_Network> elemset = new ArrayList<>();
		
		for (ACQ_IConstraint c : elem) {
			istimeouted();
			ConstraintSet cset = elem.getFactory().createSet(c);
			ACQ_Network newnet = new ACQ_Network(elem.getFactory(), elem.getVariables(), cset);
			elemset.add(newnet);
		}
		
		if (sets.size() == 0) {	
			return elemset;
		}
		else {
			return minimum(cross(hittingSets(sets), elemset));
		}
	}
	
	protected ArrayList<ACQ_Network> minimum(ArrayList<ACQ_Network> g) throws TimeoutException {
		ArrayList<ACQ_Network> res = new ArrayList<ACQ_Network>();
		for (ACQ_Network s : g) {
			boolean toadd = true;
			for (ACQ_Network t : g) {
				istimeouted();
				if (issubset(t, s) && !t.equals(s)) {
					toadd = false;
					break;
				}
			}
			if (toadd) {
				res.add(s);
			}
		}
		return res;
	}
	
	protected boolean issubset(ACQ_Network l1, ACQ_Network l2) throws TimeoutException {
		for (ACQ_IConstraint a : l1) {
			boolean in = false;
			for (ACQ_IConstraint b : l2) {
				istimeouted();
				if (a.equals(b)) {
					in = true;
					break;
				}
			}
			if (!in) return false;
		}
		return true;
	}
	
	protected ArrayList<ACQ_Network> cross(ArrayList<ACQ_Network> a, ArrayList<ACQ_Network> b) throws TimeoutException {
		ArrayList<ACQ_Network> res = new ArrayList<ACQ_Network>();
		for (ACQ_Network ai : a) {
			for (ACQ_Network bj : b) {
				istimeouted();
				ConstraintSet cset = ai.getFactory().createSet(ai.getConstraints());
				cset.addAll(bj.getConstraints());
				ACQ_Network newnet = new ACQ_Network(ai.getFactory(), cset);
				res.add(newnet);
			}
		}
		return res;
	}
}	
