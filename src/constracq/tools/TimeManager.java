package constracq.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.binsec.core.tools.TimeUnit;

public class TimeManager {

	private List<Double> list = new ArrayList<Double>();
	
	protected String name = null;
	
	protected TimeUnit unit = TimeUnit.MS;
	
	public TimeManager() {
	}
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

}
