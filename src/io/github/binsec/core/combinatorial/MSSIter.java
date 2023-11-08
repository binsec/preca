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

import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.tools.Chrono;

public abstract class MSSIter {
	
	protected Chrono chrono = null;
	
	abstract public boolean hasNext();
	
	abstract public ACQ_Query next() throws TimeoutException ;
	
	public void setMUS(ContradictionSet muses) {
		// by default do nothing
	}
	
	public void setChrono(Chrono chrono) {
		this.chrono = chrono;	
	}

	public ArrayList<ACQ_Network> getMuses() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
