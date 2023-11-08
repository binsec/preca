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

import io.github.binsec.core.learner.ACQ_Query;

public class StatManager {
	
	
	private int nb_var;
	
	// Stats counters
	private String msg;
	private int nb_partial_query;
	private int nb_complete_query;
	private int nb_positive_query;
	private int nb_negative_query;
	private int query_size;
	private int non_asked_query;
	private int visited_scopes;
	
	public StatManager(int nb) {
		nb_partial_query = 0;
		nb_complete_query = 0;
		nb_positive_query = 0;
		nb_negative_query = 0;
		query_size = 0;
		non_asked_query=0;
		nb_var = nb;
		visited_scopes=0;
	}
	
	public void setMsg(String message) {
		msg = message;
	}
	
	public void update(ACQ_Query e) {
		if(e.isPositive()) {
			nb_positive_query++;
			if(e.getScope().size() == nb_var)
				nb_complete_query++;
			else 
				nb_partial_query++;
		}else {
			nb_negative_query++;
			if(e.getScope().size() == nb_var)
				nb_complete_query++;
			else
				nb_partial_query++;
		}
		query_size += e.getScope().size();
	}
	
	public void update_non_asked_query(ACQ_Query e) {
		
			non_asked_query++;
			
	}
	
	public void update_visited_scopes() {
		visited_scopes++;
		
}
	
	public int getNbPartialQuery() {
		return nb_partial_query;
	}
	
	public int getNbCompleteQuery() {
		return nb_complete_query;
	}
	
	public int getNbPositiveQuery() {
		return nb_positive_query;
	}
	
	public int getNbNegativeQuery() {
		return nb_negative_query;
	}
	
	public float getQuerySize() {
		return (float)query_size/(nb_partial_query+nb_complete_query);
	}
	
	
	public String toString() {
		String res = "----- " + msg + " -----";
		res += "\nTotal queries : " + (nb_complete_query + nb_partial_query);
		res += "\nPositive queries : " + nb_positive_query;
		res += "\nNegative queries : " + nb_negative_query;
		return res;
	}

	public int getNon_asked_query() {
		return non_asked_query;
	}

	public void setNon_asked_query(int non_asked_query) {
		this.non_asked_query = non_asked_query;
	}

	public int getVisited_scopes() {
		return visited_scopes;
	}

	public void setVisited_scopes(int visited_scopes) {
		this.visited_scopes = visited_scopes;
	}
	
	
}
