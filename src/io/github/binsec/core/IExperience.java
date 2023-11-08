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

import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ConstraintMapping;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Learner;

public interface IExperience {

	public ACQ_Bias createBias();
	
	public ACQ_Learner createLearner();

	public ACQ_ConstraintSolver createSolver();

	public Long getTimeout();
	
	public Long getLearningTimeout();
	
	public boolean isVerbose();

	public SATSolver createSATSolver();
	
	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias);
	
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping);
	
	public boolean getJson();
	
	public ACQ_CellVariable[] getCells();
	
	public default ACQ_Bias getKnownConstraints() {
		return null;
	}
}
