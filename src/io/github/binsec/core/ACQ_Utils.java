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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;

import org.json.JSONObject;

import io.github.binsec.core.acqconstraint.ACQ_Network;
import io.github.binsec.core.acqconstraint.ContradictionSet;
import io.github.binsec.core.acqsolver.ACQ_ConstraintSolver;
import io.github.binsec.core.acqsolver.SATSolver;
import io.github.binsec.core.acqvariable.ACQ_CellVariable;
import io.github.binsec.core.learner.ACQ_Bias;
import io.github.binsec.core.learner.ACQ_Learner;
import io.github.binsec.core.learner.ACQ_Query;
import io.github.binsec.core.learner.ACQ_Scope;
import io.github.binsec.core.learner.AnswerTimeoutException;
import io.github.binsec.core.learner.ObservedLearner;
import io.github.binsec.core.tools.Chrono;
import io.github.binsec.core.tools.Collective_Stats;
import io.github.binsec.core.tools.StatManager;
import io.github.binsec.core.tools.TimeManager;
import io.github.binsec.core.tools.TimeUnit;

public class ACQ_Utils {
	public static Collective_Stats stats = new Collective_Stats();
	public static int instance =0;

	public static Collective_Stats executeConacqExperience(DefaultExperience expe, boolean active) {
		ACQ_Bias bias = expe.createBias();
		ACQ_Bias known = expe.getKnownConstraints();
		
		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);
		
		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
		statManager.setMsg("Queries");
		PropertyChangeListener queryListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				switch (evt.getPropertyName()) {
				case "ASK":
					ACQ_Query query = (ACQ_Query) evt.getNewValue();
					statManager.update(query);
					break;
				case "NON_ASKED_QUERY":
					ACQ_Query query_ = (ACQ_Query) evt.getNewValue();
					statManager.update_non_asked_query(query_);
					break;

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		//ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());
		
		TimeUnit unit = TimeUnit.S;
		
		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager("CP");
		timeManager.setUnit(unit);
		Chrono chrono = new Chrono(expe.getClass().getName(), true);
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add(secondToUnit((Float)evt.getNewValue(), unit));
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});
		
		final SATSolver satSolver = expe.createSATSolver();
		satSolver.setLimit(expe.getTimeout());
		
		// observe sat solver time measurement
		final TimeManager satTimeManager = new TimeManager("SAT");
		satTimeManager.setUnit(unit);
		satSolver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
					
					if (evt.getPropertyName().startsWith("END_TIMECOUNT")) {
						satTimeManager.add(chrono.getLast(evt.getPropertyName().substring(4), unit));
					}
				}
			}
		});
		
		/*
		 * Instantiate Strategies
		 */
		ArrayList<ACQ_Network> strat = expe.createStrategy(bias);
		
		
		/*
		 * Instantiate Acquisition algorithm
		 */
		
		
		ACQ_CONACQ acquisition;
		
		if (known != null) {
			acquisition = new ACQ_CONACQ(observedLearner, bias, known, satSolver, solver);
		}
		else {
			acquisition = new ACQ_CONACQ(observedLearner, bias, satSolver, solver);
		}
		
		acquisition.setQueryPrinter(expe.getQueryPrinter());
		acquisition.setWeighted(expe.isWeighted());
		
		/*
		 * Instantiate Background knowledge
		 */
		ContradictionSet backknow = expe.createBackgroundKnowledge(bias, acquisition.mapping);
		int origbksize = backknow != null ? backknow.getSize() : 0;
		acquisition.setBackgroundKnowledge(backknow);
		
		// Param
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setStrat(strat);
		acquisition.setCellVariables(expe.getCells());
		acquisition.setChrono(chrono);
		Long learningTimeout = expe.getLearningTimeout();
		acquisition.setLearningTimeout(learningTimeout);
		
		if (expe.isVerbose()) System.out.println(String.format("Bias size : %d", acquisition.getBias().getSize()));
		if (expe.isVerbose() && backknow != null) System.out.println(String.format("BN size : %d", backknow.getSize()));
		

		if (!active) {
			// Generate 100 random inputs
			
			ACQ_CellVariable[] cells = expe.getCells();
			
			ArrayList<ACQ_Query> classified = new ArrayList<>();
			ACQ_Scope vars = bias.getVars();
			int nvars = vars.size();
			
			for (int i = 0; i<100; i++) {
				int values[] = new int[nvars]; // TODO Check if cell.size taken into account 
				
				int varid = 0;
				for(ACQ_CellVariable cell : cells) {
					Integer vals[] = cell.rand();
					if (vals[0] != null) {
						values[varid] = vals[0];
						varid += 1;
					}
					values[varid] = vals[1];
					varid += 1;
					
					values[varid] = vals[2];
					varid += 1;
				}
				ACQ_Query query = new ACQ_Query(vars, values);
				learner.ask(query);
				classified.add(query);
			}
			
			acquisition.setPreprocAnswered(classified);
			acquisition.setPassive();
		}
		/*
		 * Run
		 */
		chrono.start("total");
		boolean answer_timeouted = false;
		try {
			acquisition.process();
		} catch (AnswerTimeoutException e) {
			answer_timeouted = true;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		chrono.stop("total");
		
		/*
		 * Print results
		 */
		
		ACQ_Network learned = acquisition.getLearnedNetwork();
		ACQ_Network notsimpl = learned;
		chrono.start("simplify");
		if (learned != null) learned = expe.simplify(learned);
		chrono.stop("simplify");
		
		if (expe.getJson()) {
			JSONObject obj = new JSONObject();
			
			obj.put("answer_timeouted", answer_timeouted);
			
			obj.put("time_unit", unit.toString());
			obj.put("nb_queries", statManager.getNbCompleteQuery());
			obj.put("nb_pos_queries", statManager.getNbPositiveQuery());
			obj.put("nb_neg_queries", statManager.getNbNegativeQuery());
			
			Double sq = chrono.getResult("solveQ", unit);
			Double sn = chrono.getResult("solve_network", unit);
			obj.put("constrSolvTime", sq != null && sn != null ? sq + sn : JSONObject.NULL);
			
			Integer sqni = chrono.nbInstances("solveQ");
			Integer snni = chrono.nbInstances("solve_network");
			obj.put("constrSolvNbCall", sqni != null && snni != null ? sqni + snni : JSONObject.NULL);
			
			Double ssolve = chrono.getResult("satsolve", unit);
			obj.put("satSolvTime", ssolve != null ? ssolve : JSONObject.NULL);
			
			Integer ssolveni =  chrono.nbInstances("satsolve");
			obj.put("satSolvNbCall", ssolveni != null ? ssolveni : JSONObject.NULL);
			
			Double tot = chrono.getResult("total", unit);
			obj.put("convTime", tot != null ? tot : JSONObject.NULL);
			
			Double fcl = chrono.getResult("first_constr_learned", unit);
			obj.put("time_first_constr_learned", fcl != null ? fcl : JSONObject.NULL); 
			
			obj.put("network", learned != null ? learned.toString() : JSONObject.NULL);
			obj.put("smtlib", learned != null ? learned.toSmtlib() : JSONObject.NULL);
			
			Double gq = chrono.getResult("gen_query", unit);
			obj.put("query_gen_total", gq != null ? gq : JSONObject.NULL);
			
			Double gqmin = chrono.getMin("gen_query", unit);
			obj.put("query_gen_min", gqmin != null ? gqmin : JSONObject.NULL);
			
			Double gqmax = chrono.getMax("gen_query", unit);
			obj.put("query_gen_max", gqmax != null ? gqmax : JSONObject.NULL);
			
			Double gqmean = chrono.getMean("gen_query", unit);
			obj.put("query_gen_mean", gqmean != null ? gqmean : JSONObject.NULL);
			
			// Be carefull: The last query_gen does not generate queries 
			// but found that no more query need to be asked
			double[] gqa = chrono.getAll("gen_query", unit);
			obj.put("query_gen_all", gqa != null ? gqa : JSONObject.NULL);
			
			obj.put("timeout", learningTimeout/1000.0); // learning TO is in miliseconds
			obj.put("timeouted", acquisition.timeouted ? "yes" : "no");
			
			obj.put("bksize", origbksize);
			obj.put("biassize", bias.getSize());
			
			obj.put("network_not_simpl", notsimpl != null ? notsimpl.toString() : JSONObject.NULL);
			obj.put("smtlib_not_simpl", notsimpl != null ? notsimpl.toSmtlib() : JSONObject.NULL);
			
			Double simplTime = chrono.getResult("simplify", unit);
			obj.put("simplTime", learned != null ? simplTime : JSONObject.NULL);
			
			System.out.println(obj.toString());
		}
		else if (answer_timeouted) {
			System.out.println("\nAsk timeouted");
		} else {
			System.out.println((expe.isVerbose() ? "\n" : "") + statManager + "\n\n" + timeManager.getResults() 
				+ "\n\n" + satTimeManager.getResults());
			DecimalFormat df = new DecimalFormat("0.00E0");
			double totalTime = (double) chrono.getResult("total", unit);
			double mean_query_time = active ? (double) chrono.getMean("gen_query", unit) : 0.0;
			double simplTime = (double) chrono.getResult("simplify", unit);
				
			System.out.println("\n------ Execution times ------");
			System.out.println("Mean query generation time: " + df.format(mean_query_time) + unit);
			System.out.println("Simplification time : " + df.format(simplTime) + unit);
			System.out.println("Convergence time : " + df.format(totalTime) + unit);
			
			System.out.println("\n*************Learned Network CL example ******");
			System.out.println(learned != null ? learned.toString() : null);
			
			System.out.println("*************Learned Network CL example (SMTLIB) ******");
			System.out.println(learned != null ? learned.toSmtlib() : null + "\n");
			
		}
		return stats;
	}
	
	public static Collective_Stats executeDCAExperience(DefaultExperience expe) {
		/*
		 * prepare bias
		 */
		ACQ_Bias bias = expe.createBias();
		
		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);
		
		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
		statManager.setMsg("Queries");
		PropertyChangeListener queryListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				switch (evt.getPropertyName()) {
				case "ASK":
					ACQ_Query query = (ACQ_Query) evt.getNewValue();
					statManager.update(query);
					break;
				case "NON_ASKED_QUERY":
					ACQ_Query query_ = (ACQ_Query) evt.getNewValue();
					statManager.update_non_asked_query(query_);
					break;

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		//ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());
		
		TimeUnit unit = TimeUnit.S;
		
		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager("CP");
		timeManager.setUnit(unit);
		Chrono chrono = new Chrono(expe.getClass().getName(), true);
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add(secondToUnit((Float)evt.getNewValue(), unit));
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});
		
		final SATSolver satSolver = expe.createSATSolver();
		satSolver.setLimit(expe.getTimeout());
		
		// observe sat solver time measurement
		final TimeManager satTimeManager = new TimeManager("SAT");
		satTimeManager.setUnit(unit);
		satSolver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
					
					if (evt.getPropertyName().startsWith("END_TIMECOUNT")) {
						satTimeManager.add(chrono.getLast(evt.getPropertyName().substring(4), unit));
					}
				}
			}
		});
		
		/*
		 * Instantiate Strategies
		 */
		ArrayList<ACQ_Network> strat = expe.createStrategy(bias);
		
		
		/*
		 * Instantiate Acquisition algorithm
		 */
		
		
		ACQ_DCA acquisition;
		
		acquisition = new ACQ_DCA(bias, expe.getKnownConstraints(), observedLearner, satSolver, solver);
		
		if (expe.isVerbose()) System.out.println(String.format("Bias size : %d", bias.getSize()));
		
		acquisition.setQueryPrinter(expe.getQueryPrinter());
		acquisition.setGrow2(expe.getGrow2());
		
		/*
		 * Instantiate Background knowledge
		 */
		ContradictionSet backknow = expe.createBackgroundKnowledge(bias, acquisition.mapping);
		int origbksize = backknow != null ? backknow.getSize() : 0;
		if (expe.isVerbose() && backknow != null) System.out.println(String.format("BN size : %d", origbksize));
		acquisition.setBackgroundKnowledge(backknow);
		
		// Param
		acquisition.setVerbose(expe.isVerbose());
		//acquisition.setLog_queries(expe.isLog_queries());
		acquisition.setStrat(strat);
		//acquisition.setCellVariables(expe.getCells());
		
		Long learningTimeout = expe.getLearningTimeout();
		acquisition.setLearningTimeout(learningTimeout);
		/*
		 * Run
		 */
		chrono.start("total");
		boolean answer_timeouted = false;
		try {
			acquisition.process(chrono);
		} catch (AnswerTimeoutException e) {
			answer_timeouted = true;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		chrono.stop("total");
		
		/*
		 * Print results
		 */
		
		ACQ_Network learned = !acquisition.timeouted ? acquisition.getLearnedDNF() : null;
		
		ACQ_Network notsimpl = learned;
		chrono.start("simplify");
		if (learned != null) learned = expe.simplify(learned, acquisition.muses);
		chrono.stop("simplify");
		
		if (expe.getJson()) {
			JSONObject obj = new JSONObject();
			
			obj.put("answer_timeouted", answer_timeouted);
			
			obj.put("time_unit", unit.toString());
			obj.put("nb_queries", statManager.getNbCompleteQuery());
			obj.put("nb_pos_queries", statManager.getNbPositiveQuery());
			obj.put("nb_neg_queries", statManager.getNbNegativeQuery());
			
			Double sq = chrono.getResult("solveQ", unit);
			Double sn = chrono.getResult("solve_network", unit);
			obj.put("constrSolvTime", sq != null && sn != null ? sq + sn : JSONObject.NULL);
			
			Integer sqni = chrono.nbInstances("solveQ");
			Integer snni = chrono.nbInstances("solve_network");
			obj.put("constrSolvNbCall", sqni != null && snni != null ? sqni + snni : JSONObject.NULL);
			
			Double ssolve = chrono.getResult("satsolve", unit);
			obj.put("satSolvTime", ssolve != null ? ssolve : JSONObject.NULL);
			
			Integer ssolveni =  chrono.nbInstances("satsolve");
			obj.put("satSolvNbCall", ssolveni != null ? ssolveni : JSONObject.NULL);
			
			Double tot = chrono.getResult("total", unit);
			obj.put("convTime", tot != null ? tot : JSONObject.NULL);
			
			Double gq = chrono.getResult("gen_query", unit);
			obj.put("query_gen_total", gq != null ? gq : JSONObject.NULL);
			
			Double gqmin = chrono.getMin("gen_query", unit);
			obj.put("query_gen_min", gqmin != null ? gqmin : JSONObject.NULL);
			
			Double gqmax = chrono.getMax("gen_query", unit);
			obj.put("query_gen_max", gqmax != null ? gqmax : JSONObject.NULL);
			
			Double gqmean = chrono.getMean("gen_query", unit);
			obj.put("query_gen_mean", gqmean != null ? gqmean : JSONObject.NULL);
			
			obj.put("network", learned != null ? learned.toString() : JSONObject.NULL);
			obj.put("smtlib", learned != null ? learned.toSmtlib() : JSONObject.NULL);
			
			obj.put("network_not_simpl", notsimpl != null ? notsimpl.toString() : JSONObject.NULL);
			obj.put("smtlib_not_simpl", notsimpl != null ? notsimpl.toSmtlib() : JSONObject.NULL);
			
			obj.put("timeout", learningTimeout/1000.0); // learning TO is in miliseconds
			obj.put("timeouted", acquisition.timeouted ? "yes" : "no");
			
			obj.put("biassize", bias.getSize());
			obj.put("bksize", origbksize);
			
			Double simplTime = chrono.getResult("simplify", unit);
			obj.put("simplTime", learned != null ? simplTime : JSONObject.NULL);
			
			System.out.println(obj.toString());
		}
		else if (answer_timeouted) {
			System.out.println("\nAsk timeouted");
		} else {
			System.out.println((expe.isVerbose() ? "\n" : "") + statManager + "\n\n" + timeManager.getResults() 
				+ "\n\n" + satTimeManager.getResults());
			
			DecimalFormat df = new DecimalFormat("0.00E0");
			double totalTime = (double) chrono.getResult("total", unit);
			double simplTime = (double) chrono.getResult("simplify", unit);
			double mean_query_time = (double) chrono.getMean("gen_query", unit);
	
			System.out.println("\n------ Execution times ------");
			System.out.println("Mean query generation time: " + df.format(mean_query_time) + unit);
			System.out.println("Simplification time : " + df.format(simplTime) + unit);
			System.out.println("Convergence time : " + df.format(totalTime) + unit);
			
			
			System.out.println("\n*************Learned Network CL example ******");
			System.out.println(learned != null ? learned.toString() : null);
			
			System.out.println("*************Learned Network CL example (SMTLIB) ******");
			System.out.println(learned != null ? learned.toSmtlib() : null + "\n");
			
		}
		return stats;
	}
	
	private static Float secondToUnit(Float f, TimeUnit unit) {
		switch (unit) {
		case S:
			return f;
		case MS:
			return f*1000;
		case NS:
			return f*1000000000;
			
		default:
			assert false : "unknow time unit";
			return null;
		}
	}

	public static int[] bitSet2Int(BitSet bs) {
		int[] result = new int[bs.cardinality()];
		int counter = 0;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			result[counter++] = i;
		}
		return result;
	}
		
	public static int[] mergeWithoutDuplicates(int[] a, int[] b) {
		BitSet bs = new BitSet();
		for (int numvar : a)
			bs.set(numvar);
		for (int numvar : b)
			bs.set(numvar);
		return bitSet2Int(bs);
	}

}
