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
import java.util.Arrays;
import java.util.HashMap;

import io.github.binsec.core.DefaultExperience;
import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ConstraintFactory;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Learner;
import io.github.binsec.core.learner.ACQ_Query;

public class Collective_Stats {
	HashMap<Integer, Chrono> chronos;
	HashMap<Integer, Chrono> chronos_cpu;

	HashMap<Integer, Chrono> querychronos;

	HashMap<Integer, StatManager> statManagers;
	HashMap<Integer, TimeManager> timeManagers;

	HashMap<Integer, Long> wallTime;

	HashMap<Integer, Long> acqTime;

	HashMap<Integer, ACQ_Network> CL;
	HashMap<Integer, ACQ_Network> CL_i;
	HashMap<Integer, ACQ_Bias> biases;
	public HashMap<Integer, Boolean> results;
	public double average_L = 0.0;
	public HashMap<Integer, Integer> average_Li = new HashMap<>();
	public int nb_queries = 0;
	public HashMap<Integer, Integer> nb_queries_i = new HashMap<>();
	public double ratio = 0.0;
	public double MQ = 0;
	public double max_t = 0;
	public double avg_t = 0;
	public HashMap<Integer, Integer> non_asked_queries = new HashMap<>();

	public int nb_sharedqueries = 0;

	public HashMap<Integer, Integer> MQ_i = new HashMap<>();
	public HashMap<Integer, Float> avg_query_size = new HashMap<>();

	public Collective_Stats() {
		this.chronos = new HashMap<>();
		this.chronos_cpu = new HashMap<>();
		this.statManagers = new HashMap<>();
		this.timeManagers = new HashMap<>();
		this.CL = new HashMap<>();
		this.CL_i = new HashMap<>();
		this.biases = new HashMap<>();
		this.results = new HashMap<>();
		this.wallTime = new HashMap<>();
		this.acqTime = new HashMap<>();

	}

	public Long getWallTime(Integer id) {
		return wallTime.get(id);
	}

	public void saveWallTime(Integer id, Long value) {
		wallTime.put(id, value);
	}

	public Long getAcqTime(Integer id) {
		return acqTime.get(id);
	}

	public void saveAcqTime(Integer id, Long value) {
		acqTime.put(id, value);
	}

	public HashMap<Integer, Long> getAcqTime() {
		return acqTime;
	}

	public HashMap<Integer, Chrono> getChronos() {
		return chronos;
	}
	public HashMap<Integer, Chrono> getChronosCPU() {
		return chronos_cpu;
	}
	public void setChronos(HashMap<Integer, Chrono> chronos) {
		this.chronos = chronos;
	}

	public HashMap<Integer, StatManager> getStatManagers() {
		return statManagers;
	}

	public void setStatManagers(HashMap<Integer, StatManager> statManagers) {
		this.statManagers = statManagers;
	}

	public HashMap<Integer, TimeManager> getTimeManagers() {
		return timeManagers;
	}

	public void setTimeManagers(HashMap<Integer, TimeManager> timeManagers) {
		this.timeManagers = timeManagers;
	}

	public HashMap<Integer, ACQ_Network> getCL() {
		return CL;
	}

	public void setCL(HashMap<Integer, ACQ_Network> cL) {
		CL = cL;
	}

	public void setCL_i(HashMap<Integer, ACQ_Network> cL) {
		CL_i = cL;
	}

	public HashMap<Integer, ACQ_Bias> getBiases() {
		return biases;
	}

	public void setBiases(HashMap<Integer, ACQ_Bias> biases) {
		this.biases = biases;
	}

	public HashMap<Integer, Boolean> getResults() {
		return results;
	}

	public void setResults(HashMap<Integer, Boolean> results) {
		this.results = results;
	}

	public void saveTimeManager(int id, TimeManager timeManager) {
		timeManagers.put(id, timeManager);
	}

	public void savestatManager(int id, StatManager statManager) {
		statManagers.put(id, statManager);
	}

	public void saveBias(int id, ACQ_Bias bias) {
		biases.put(id, bias);

	}

	public void saveLearnedNetwork(int id, ACQ_Network learnedNetwork) {
		CL.put(id, learnedNetwork);

	}

	public void saveLearnedNetwork_i(int id, ACQ_Network learnedNetwork) {
		CL_i.put(id, learnedNetwork);

	}

	public void saveResults(int id, boolean result) {
		results.put(id, result);

	}

	public void saveChronos(int id, Chrono chrono) {
		chronos.put(id, chrono);

	}
	public void saveChronosCPU(int id, Chrono chrono) {
		chronos_cpu.put(id, chrono);

	}
	public HashMap<Integer, Chrono> getQuerychronos() {
		return querychronos;
	}

	public void setQuerychronos(HashMap<Integer, Chrono> querychronos) {
		this.querychronos = querychronos;
	}

	public Double getAverage_L() {

		return average_L;

	}

	public HashMap<Integer, Integer> getAverage_Li() {
		return average_Li;
	}

	public double getNb_queries() {
		return nb_queries;
	}

	public HashMap<Integer, Integer> getNb_queries_i() {
		return nb_queries_i;
	}

	public double getRatio() {
		return ratio;
	}

	public double getMQ() {
		for (int i = 0; i < this.getStatManagers().size(); i++) {
			this.MQ += this.getStatManagers().get(i).getNbCompleteQuery();
		}
		return MQ;
	}

	public HashMap<Integer, Integer> getMQ_i() {
		return MQ_i;
	}

	public HashMap<Integer, Float> getAvg_query_size() {
		return avg_query_size;
	}

	public void Printstats(int nb_threads, ACQ_Learner learner, ACQ_Bias bias, DefaultExperience expe,
			int shared_queries) {
		double totaltime = 0.0;
		int totalqueries = 0;
		for (int i = 0; i < this.getChronos().size(); i++) {
			System.out.print("\n\n");
			System.out.println("================== Thread :: " + i + "========================");
			System.out.println(this.getStatManagers().get(i) + "\n" + this.getTimeManagers().get(i).getResults());
			this.non_asked_queries.put(i, this.getStatManagers().get(i).getNon_asked_query());
			this.MQ_i.put(i, this.getStatManagers().get(i).getNbCompleteQuery());
			this.nb_queries_i.put(i, this.getStatManagers().get(i).getNbCompleteQuery()
					+ this.getStatManagers().get(i).getNbPartialQuery());
			totalqueries += this.getStatManagers().get(i).getNbCompleteQuery()
					+ this.getStatManagers().get(i).getNbPartialQuery();
			this.nb_queries = totalqueries;

			DecimalFormat df = new DecimalFormat("0.00E0");
			double totalTime = 0;
			totalTime = (double) this.getChronos().get(i).getResult("total", TimeUnit.MS) / 1000.0;
			System.out.println("------Execution times------");

			for (String serieName : this.getChronos().get(i).getSerieNames()) {
				if (!"total".equals(serieName)) {
					double serieTime = (double) this.getChronos().get(i).getResult(serieName, TimeUnit.MS) / 1000.0;
					totaltime += serieTime;
					System.out.println(serieName + " : " + df.format(serieTime));
				}
			}

			System.out.println("Total time : " + df.format(totalTime));
			System.out.println("-------Learned Network & Bias Size--------");
			System.out.println("Learned Network Size : " + this.getCL().get(i).size());
			System.out.println("Bias Initial Size : " + this.getBiases().get(i).getInitial_size());
			System.out.println("Bias Final Size : " + this.getBiases().get(i).getSize());
			System.out.println("==========================================================");
		}

		ConstraintFactory constraintFactory = new ConstraintFactory();
		ACQ_Network learned_network = new ACQ_Network(constraintFactory, bias.getVars());
		int i = 0;
		for (ACQ_Network n : this.getCL().values()) {
			this.average_Li.put(i, n.size());
			learned_network.addAll(n, false);
			i++;
		}
		int Bias_init = 0;
		int Bias_final = 0;
		for (ACQ_Bias b : this.getBiases().values()) {
			Bias_init += b.getInitial_size();
			Bias_final += b.getSize();
		}
		System.out.print("Complete CL :: \n\n");
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setTimeout(false);
		ACQ_Query q = solver.solveA(learned_network);
		System.out.println("================== Total this========================");
		System.out.println("Total Queries : " + totalqueries);
		DecimalFormat df = new DecimalFormat("0.00E0");
		System.out.println("Total Execution time : " + df.format(totaltime / nb_threads));
		nb_sharedqueries = shared_queries;
		System.out.println("Total Shared Queries : " + shared_queries);
		this.average_L = learned_network.size();
		System.out.println("Leaned Network Size : " + learned_network.size());
		System.out.println("Total Biases Initial Size :" + Bias_init);
		System.out.println("Total Biases Final Size :" + Bias_final);
		for (Integer key : this.results.keySet()) {
			if (this.results.get(key))
				System.out.println("Thread n° : " + key + " = Converged");
			else
				System.out.println("Thread n° : " + key + " = Collapsed");

		}
		System.out.println("query :: " + Arrays.toString(q.values));
		System.out.println("Classification :: " + learner.ask(q));
		System.out.println("==========================================================");
	}

	public void saveQueryChronos(int id, Chrono chrono) {
		querychronos.put(id, chrono);

	}

	public HashMap<Integer, Chrono> getQueryChronos() {
		return querychronos;
	}

	public int getNb_sharedqueries() {
		return nb_sharedqueries;
	}

	public void setNb_sharedqueries(int nb_sharedqueries) {
		this.nb_sharedqueries = nb_sharedqueries;
	}

	public double getMax_t() {
		return max_t;
	}

	public double getAvg_t() {
		return avg_t;
	}

	public HashMap<Integer, Integer> getNon_Asked_Queries() {
		return non_asked_queries;
	}

	public void setNon_Asked_Queries(HashMap<Integer, Integer> non_asked_queries) {
		this.non_asked_queries = non_asked_queries;
	}

	public HashMap<Integer, ACQ_Network> getCL_i() {
		return CL_i;
	}


	
}
