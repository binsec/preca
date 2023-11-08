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

package io.github.binsec.core.acqconstraint;

import io.github.binsec.core.learner.ACQ_Scope;

public class Contradiction {
	
	ACQ_Network net;
	ConstraintFactory factory;
	ACQ_Scope scope;
	
	public Contradiction(ConstraintFactory factory, ACQ_Scope scope) {
		this.net = new ACQ_Network(factory, scope);
		this.factory = factory;
		this.scope = scope;
	}
	
	public Contradiction(ACQ_Network net) {
		this.net = net;
		this.factory = net.getFactory();
		this.scope = net.getVariables();
	}
	
	public ACQ_Network toNetwork() {
		return net;
	}
	
	public ACQ_DisjunctionConstraint toDisjunction() {
		ACQ_IConstraint[] l = net.getArrayConstraints();
		assert l.length > 1 : "A constraint is by itself contradictory";
		ACQ_DisjunctionConstraint res = new ACQ_DisjunctionConstraint(factory, l[0].getNegation(), l[1].getNegation());
		for(int i = 2; i<l.length; i++) {
			res = new ACQ_DisjunctionConstraint(factory, res, l[i]);
		}
		return res;
		
	}
	
//	public void add(Unit unit) {
//		assert(unit.isNeg());
//	}
	
	public Clause toClause(ConstraintMapping mapping) {
		Clause result = new Clause();
		for(ACQ_IConstraint c : net.getConstraints()) {
			Unit unit = mapping.get(c).clone();
			unit.setNeg();
			result.add(unit);
		}
		return result;
	}
	
	public Clause toFact(ConstraintMapping mapping) {
		Clause result = new Clause();
		for(ACQ_IConstraint c : net.getConstraints()) {
			Unit unit = mapping.get(c).clone();
			result.add(unit);
		}
		return result;
	}
	
	public Boolean isEmpty() {
		return net.isEmpty();
	}
}
