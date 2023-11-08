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
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.tools.QueryPrinter;

public abstract class DefaultExperience implements IExperience {
	
	protected static boolean json = false;

	private long timeout= 5000;
	private boolean verbose;

	public void setParams(long timeout, boolean verbose,boolean log_queries) {
		this.timeout = timeout;
		this.verbose= verbose;

	}

	public boolean isVerbose() {
		return verbose;
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public Long getTimeout() {
		return timeout;
	}
	
	public Long getLearningTimeout() {
		return null;
	}
	
	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias) {
		return null;
	}
	
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping) {
		return null;
	}
	
	public boolean getJson() {
		return json;
	}

	public boolean isWeighted() {
		return false;
	}
	
	public ACQ_Network simplify(ACQ_Network net, ArrayList<ACQ_Network> muses) {
		return net;
	}
	
	public ACQ_Network simplify(ACQ_Network net) {
		return net;
	}

	/*
	 * Return true if use the extension of grow in MSS enumeration
	 */
	public boolean getGrow2() {
		return true;
	}
	
	public QueryPrinter getQueryPrinter() {
		return new QueryPrinter();
	}
	
	
}
