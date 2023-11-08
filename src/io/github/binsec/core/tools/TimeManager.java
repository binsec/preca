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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeManager {

	private List<Double> list = new ArrayList<Double>();
	
	protected String name = null;
	
	protected TimeUnit unit = TimeUnit.MS;
	
	public TimeManager() {
	}
	
	public TimeManager(String name) {
		this.name = name;
	}
	
	public void add(double i) {
		list.add(i);
	}

	public void display() {
		for(double i : list) {
			System.out.println(i);
		}
	}
	
	public int nbInstance() {
		return list.size();
	}
	
	public double getMax() {
		return list.size()==0 ? 0 : Collections.max(list);
	}
	
	public double getMin() {
		return list.size()==0 ? 0 : Collections.min(list);
	}
	
	public double getTotal() {
		double time = 0;
		for(double f : list) {
			time += f;
		}
		return time;
	}
	
	public double getAverage() {
		return getTotal()/nbInstance();
	}
	
	public double getMedian() {
		return list.size()==0 ? 0 : list.get((int)Math.floor((nbInstance()-1)/2));
	}
	
	public double getSD(){
		double sd = 0;
		for(double f : list) {
			sd += Math.pow(Math.abs(f - getAverage()), 2);
		}
		
		return Math.sqrt(sd/nbInstance());
	}
	
	public String getResults() {
		DecimalFormat df = new DecimalFormat("0.000E0");
		return "------ " + this.name + " Solving times ------" +
				"\nTotal time : " + df.format(getTotal()) + unit +
				"\nMax time : " + df.format(getMax()) + unit +
				"\nMin time : " +  df.format(getMin()) + unit +
				"\nAverage time : " + df.format(getAverage()) + unit +
				"\nMedian : " + df.format(getMedian()) + unit +
				"\nStandard deviation : " + df.format(getSD()) + unit +
				"\nInstances : " + nbInstance();
	}
	
	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

}
