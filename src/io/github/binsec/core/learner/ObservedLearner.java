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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This class is a wrapper to observe a given learner with a PropertyChangeListener
 * @author agutierr
 */
public class ObservedLearner extends ACQ_Learner {
	private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	protected ILearner learner;

	public ObservedLearner(ILearner learner) {
		this.learner = learner;
	}
	@Override
	public Answer ask(ACQ_Query e) {
		Answer ret = learner.ask(e);
		pcs.firePropertyChange("ASK", ret, e);
		return ret;
	}
	
	public Answer ask(ACQ_Query e, boolean fromMSS) {
		Answer ret = learner.ask(e);
		pcs.firePropertyChange(fromMSS ? "ASK_MSS" : "ASK", ret, e);
		return ret;
	}
	
	@Override
	public Answer ask_query(ACQ_Query query) {
		if (learner instanceof ACQ_Learner) {
			pcs.firePropertyChange("ASK", null, query);
			return learner.ask_query(query);
			
		}
		return Answer.NO;
		}
	@Override
		public void non_asked_query(ACQ_Query query) {
			if (learner instanceof ACQ_Learner) {
				learner.non_asked_query(query);
				pcs.firePropertyChange("NON_ASKED_QUERY", null, query);

			}
		

	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

}
