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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import io.github.binsec.core.acqconstraint.ACQ_IConstraint;
import io.github.binsec.core.acqconstraint.CNF;
import io.github.binsec.core.acqconstraint.Formula;
import io.github.binsec.core.acqconstraint.Unit;

abstract public class SATSolver {
	
	private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	public abstract SATModel solve(CNF T);
	
	public abstract SATModel solve(Formula F);
	
	public abstract void setVars();
	
	public abstract void setLimit(Long timeout);
	
	public abstract Unit addVar(ACQ_IConstraint constr, String name);
	
	public abstract Boolean isTimeoutReached();
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}
	
	public void fireSolverEvent(String name,Object oldValue,Object newValue){
		pcs.firePropertyChange(name, oldValue, newValue);
	}

	public abstract void reset();
}
