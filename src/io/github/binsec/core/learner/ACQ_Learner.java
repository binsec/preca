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

package io.github.binsec.core.learner;

import java.util.ArrayList;

/**
 * 
 * Class used to determine if whether or not a query is a valid solution or not
 *
 */
public class ACQ_Learner implements ILearner {
	/**
	 * Ask this learner if the tuple represented by the query e is a solution or not
	 * 
	 * @param e Example to classify as positive or negative
	 * @return true if the query e is positive
	 */

	public ArrayList<ACQ_Query> memory = new ArrayList<>();

	boolean memory_enabled = true;

	public boolean isMemory_enabled() {
		return memory_enabled;
	}

	public void setMemory_enabled(boolean memory_enabled) {
		this.memory_enabled = memory_enabled;
	}

	public Answer ask(ACQ_Query e) {
		// TODO Auto-generated method stub
		return Answer.NO;
	}

	/**
	 * 
	 * @param example
	 */
	public synchronized Answer ask_query(ACQ_Query example) {

		Answer asked_query = Answer.NO;
		if (memory_enabled && !memory.isEmpty())
			for (ACQ_Query tmp : memory)
				if ((example.extend(tmp) && tmp.isNegative()) || tmp.extend(example) && tmp.isPositive()) {
					example.classify_as(tmp);
					asked_query=Answer.YES;
					break;
				}
		if (!example.isClassified()) {
			ask(example);
			if (memory_enabled)
				add_memory(example);
		}
		return asked_query;

	}


	private synchronized void add_memory(ACQ_Query example) {
		memory.add(example);
	}

	public boolean equal(ACQ_Query a, ACQ_Query b) {
		for (int i : b.getScope()) {

			if (a.getValue(i) == b.getValue(i)) {
				return false;

			}
		}
		return true;
	}

	@Override
	public void non_asked_query(ACQ_Query query) {
		// TODO Auto-generated method stub
		
	}

}