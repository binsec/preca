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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class records series of times collected during a process
 *
 * @author agutierr
 */

public class Chrono {
	private String name;
	private boolean nano;
	protected HashMap<String,Long> current_chronos=new HashMap<String,Long>();
	protected HashMap<String,ArrayList<Long>> results=new HashMap<String,ArrayList<Long>>();
	public Chrono(String name,boolean nano)
	{
		this.name=name;
		this.nano=nano;
	}

	public Chrono(String name) {
		this(name, false);
	}

	public String getName() {
		return name;
	}

	public void start(String serieName) {
		current_chronos.put(serieName, nano ? System.nanoTime() : System.currentTimeMillis());
	}

	synchronized public void stop(String serieName) {
		Long currentTime = current_chronos.get(serieName);
		if (currentTime != null) {
			ArrayList<Long> result = results.get(serieName);
			if (result == null) {
				result = new ArrayList<Long>();
				results.put(serieName, result);
			}
			result.add((nano ? System.nanoTime() : System.currentTimeMillis()) - currentTime);
			current_chronos.remove(serieName);
		}
	}
	
	public double toUnit(double v, TimeUnit unit) {
		double res = v;
		if (nano) {
			switch (unit) {
			case S: // seconds
				return res / 1E9;
			case MS: // miliseconds
				return res / 1E6;
			case NS: // nanoseconds
				return res;
			default : assert false : "Unkown unit";
			}
		} 
		else {
			// milisecond
			
			switch (unit) {
			case S: // seconds
				return res / 1000.0;
			case MS: // miliseconds
				return res;
			case NS: // nanoseconds
				return res * 1E6;
			default : assert false : "Unkown unit";
			}
		}
		return res;
	}
	
	synchronized public Double getResult(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null)
					l += mesure;
		} else {
			System.err.println(serieName);
			return null;
		}
		return toUnit(l, unit);
	}
	
	synchronized public Double getMean(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null)
					l += mesure;
		} else {
			System.err.println(serieName);
			return null;
		}
		return toUnit(l, unit) / nbInstances(serieName);
	}
	
	synchronized public Double getMin(String serieName, TimeUnit unit) {
		double min = -1;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null && (min < 0  || min > mesure))
					min = mesure;
		} else {
			System.err.println(serieName);
			return null;
		}
		return toUnit(min, unit);
	}
	
	synchronized public Double getMax(String serieName, TimeUnit unit) {
		double max = -1;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null && (max < 0  || max < mesure))
					max = mesure;
		} else {
			System.err.println(serieName);
			return null;
		}
		return toUnit(max, unit);
	}
	
	synchronized public double[] getAll(String serieName, TimeUnit unit) {
		if (results.get(serieName) != null) {
			ArrayList<Long> r = results.get(serieName);
			double[] res = new double[r.size()];
			for (int i = 0; i < r.size(); i++) {
				res[i] = toUnit(r.get(i), unit);
			}
			return res;
		}
		else {
			System.err.println(serieName);
			return null;	
		}
	}

	synchronized public Integer nbInstances(String serieName) {
		if (results.get(serieName) != null) {
			return results.get(serieName).size();
		} else {
			System.err.println(serieName);
			return null;
		}
	}
	
	synchronized public Double getLast(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {

			if (results.get(serieName).get(results.get(serieName).size()-1) != null)
				l += results.get(serieName).get(results.get(serieName).size()-1);
		} else {
			System.err.println(serieName);
			return null;
		}
		return toUnit(l, unit);
	}

	/*synchronized public ArrayList<Long> getResultArray(String serieName) {
		return results.get(serieName);
	}*/

	synchronized public double getResult(TimeUnit unit) {
		double l = 0;
		for (ArrayList<Long> mesures : results.values())
			for (Long mesure : mesures)
				l += mesure;
		return toUnit(l, unit);
	}

	synchronized public int getSerieCount() {
		return results.size();
	}

	synchronized public String[] getSerieNames() {
		return results.keySet().toArray(new String[results.keySet().size()]);
	}
}
