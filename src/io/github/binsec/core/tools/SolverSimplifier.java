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

import io.github.binsec.core.acqconstraint.ACQ_ConjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_DisjunctionConstraint;
import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.learner.ACQ_Bias;

public class SolverSimplifier implements Simplifier {
	
	ACQ_ConstraintSolver csolv;
	ACQ_Bias known;
	
	public SolverSimplifier(ACQ_ConstraintSolver constraintsolver, ACQ_Bias known) {
		csolv = constraintsolver;
		this.known = known;
	}
	
	public ACQ_Network simplify(ACQ_Network net) {
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
	
}
