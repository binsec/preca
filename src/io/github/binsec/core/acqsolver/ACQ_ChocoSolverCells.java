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

package io.github.binsec.core.acqsolver;

import java.util.ArrayList;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.acqvariable.CellType;
import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.ACQ_Scope;

public class ACQ_ChocoSolverCells extends ACQ_ConstraintSolver {

	ACQ_CellVariable[] cells;
	
	public ACQ_ChocoSolverCells(ACQ_CellVariable[] cells) {
		this.cells = cells;
	}
	
	@Override
	public boolean solve(ACQ_Network learned_network) {
		fireSolverEvent("BEG_solve_network", false, true);
		Model model = new Model("solve");
		
		IntVar[] chocoVars = getChocoVars(model);
		
		for (ACQ_IConstraint constr : learned_network.getConstraints()) {
			for (Constraint choco : constr.getChocoConstraints(model, chocoVars)) {
				choco.post();
			}
		}
		
		Solver solver = model.getSolver();
		//solver.setSearch(new DomOverWDeg(chocoVars, 0, new IntDomainRandom(0)));
		
		if (timeout)
			solver.limitTime(this.getLimit());
		
		boolean b = solver.solve();
		fireSolverEvent("TIMECOUNT_N", null, Float.valueOf(solver.getTimeCount()));
		fireSolverEvent("END_solve_network", true, false);
		return b;
	}

	@Override
	public ACQ_Query solveQ(ACQ_Network learned_network) {
		fireSolverEvent("BEG_solveQ", false, true);
		Model model = new Model("solve");
		
		IntVar[] chocoVars = getChocoVars(model);
		
		ACQ_Query query = new ACQ_Query();
		
		for (ACQ_IConstraint constr : learned_network.getConstraints()) {
			for (Constraint choco : constr.getChocoConstraints(model, chocoVars)) {
				choco.post();
			}
		}
		
		Solver solver = model.getSolver();
		//solver.setSearch(new DomOverWDeg(chocoVars, 0, new IntDomainRandom(0)));
		
		if (timeout)
			solver.limitTime(this.getLimit());
		
		solver.solve();
		
		fireSolverEvent("TIMECOUNT_Q", null, Float.valueOf(solver.getTimeCount()));
		
		if (solver.getSolutionCount() != 0) {
			int[] tuple = new int[chocoVars.length];
			for (int i = 0; i < chocoVars.length; i++) {
				tuple[i] = chocoVars[i].getValue();
			}
			query = new ACQ_Query(learned_network.getVariables(), tuple);

		}
		fireSolverEvent("END_solveQ", true, false);
		return query;
	}

	private IntVar[] getChocoVars(Model model) {
		ArrayList<IntVar> res = new ArrayList<IntVar>();
		for (int i = 0; i < cells.length; i++) {
			ACQ_CellVariable cell = cells[i];
			if (cell.isGlobal()) {
				res.add(cell.getChocoRef(model));
			}
			res.add(cell.getChocoValue(model));
			if (cell.getType() == CellType.PTR) {
				res.add(cell.getChocoSize(model));
			}
			
		}
		return res.toArray(new IntVar[res.size()]);
		
	}

	@Override
	public ACQ_Query peeling_process(ACQ_Network network_A, ACQ_Network network_B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVars(ACQ_Scope vars) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ACQ_Query max_AnotB(ACQ_Network network1, ACQ_Network network2, ACQ_Heuristic heuristic) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ArrayList<ACQ_Query> allSolutions(ACQ_Network learned_network) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setTimeoutReached(boolean timeoutReached) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isTimeoutReached() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ACQ_Network get2remove() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset2remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ACQ_IDomain getDomain() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean equiv(ACQ_Network net1, ACQ_Network net2, ACQ_Network known) {
		// Check that net1 and net2 are equivalent modulo known context
		
		// check ~net1 /\ net2
		ACQ_Network new1 = new ACQ_Network(net2.constraintFactory, net2, net2.getVariables());
		new1.add(net1.getNegation(), true);
		new1.addAll(known, true);
		if (!solveQ(new1).isEmpty()) return false;
		
		// check net1 /\ ~net2
		ACQ_Network new2 = new ACQ_Network(net1.constraintFactory, net1, net1.getVariables());
		new2.add(net2.getNegation(), true);
		new2.addAll(known, true);
		if (!solveQ(new2).isEmpty()) return false;
		
		return true;
	}

}
