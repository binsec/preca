package constracq.tools;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.binsec.core.tools.TimeUnit;

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
	public void start_at_t(String serieName, Long t) {
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
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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

	
	synchronized public Double getStdDev(String serieName, TimeUnit unit){
		Double mu = getMean(serieName, unit);
		Double res = 0.0;
		Double dev = 0.0;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName)){
				dev += java.lang.Math.pow(mesure - mu,2);
			}
			res = java.lang.Math.sqrt(toUnit(dev, unit)/ nbInstances(serieName));
				
		} else {
			System.err.println(serieName);
			return null;
		}
		return res;
	}

	synchronized public ArrayList<Long> getExactTime(String serieName, TimeUnit unit ){
		ArrayList<Long> originList = results.get(serieName);
		ArrayList<Long> cumulList =new ArrayList<>();
		Long cumul =0L;
		if (originList != null) {
			for(int i=0; i<originList.size(); i++){
				cumul += originList.get(i);
				cumulList.add(cumul);
			}
		}		
		return cumulList;

	}
	/*synchronized public double Average(ArrayList<Long> l, TimeUnit unit) {  //mean for arraylist
        if (l == null || l.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Long number : l) {
            sum += number;
        }
        return toUnit(sum,unit) / l.size();
    }
	synchronized public Double getStdDev(String serieName, TimeUnit unit){
		ArrayList<Long> cumulList = getExactTime(serieName, unit);  // récuperer les temps exacts de chaque Yes 
		// calcul le mean de cumulList
		double mu = Average(cumulList, unit);

		Double res = 0.0;
		Double dev = 0.0;
		if (cumulList != null) {
			for (Long mesure : cumulList){
				dev += java.lang.Math.pow(mesure - mu,2);
			}
			res = java.lang.Math.sqrt(toUnit(dev, unit)/ nbInstances(serieName));
				
		} else {
			System.err.println(serieName);
			return null;
		}
		return res;
	}*/


	synchronized public Double getMed(String serieName, TimeUnit unit){
		if (nbInstances(serieName) == null || nbInstances(serieName) == 0) {
			return 0.0;
		}
		ArrayList<Long> cumulList = getExactTime(serieName, unit);  // récuperer les temps exacts de chaque Yes 

		Long res = cumulList.get((int)java.lang.Math.ceil(nbInstances(serieName)/2.0)-1); 
		return toUnit(res, unit);
	}
		
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
	
	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
	synchronized public double[] getAllExact(String serieName, TimeUnit unit) {
		ArrayList<Long> cumulList = getExactTime(serieName, unit);
		if (cumulList != null) {
			double[] res = new double[cumulList.size()];
			for (int i = 0; i < cumulList.size(); i++) {
				res[i] = toUnit(cumulList.get(i), unit);
			}
			return res;
		}
		else {
			System.err.println(serieName);
			return null;	
		}
	}

	/*
	 * by Gregoire MENGUY (CEA LIST)
	 */
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
