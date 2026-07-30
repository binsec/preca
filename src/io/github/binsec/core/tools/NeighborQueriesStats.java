/******************************************************************************/
/*  This file is part of PRECA.                                               */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.    */
/*                                                                            */
/*  Copyright (C) 2019-2026                                                   */
/*  CEA (Commissariat à l'énergie atomique et aux énergies                    */
/*       alternatives)                                                        */
/*                                                                            */
/*  you can redistribute it and/or modify it under the terms of the GNU       */
/*  Lesser General Public License as published by the Free Software           */
/*  Foundation, version 2.1.                                                  */
/*                                                                            */
/*  It is distributed in the hope that it will be useful,                     */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of            */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             */
/*  GNU Lesser General Public License for more details.                       */
/*                                                                            */
/*  See the GNU Lesser General Public License version 2.1                     */
/*  for more details (enclosed in the file licenses/LGPLv2.1).                */
/*                                                                            */
/******************************************************************************/

package io.github.binsec.core.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.chocosolver.solver.constraints.Constraint;
import org.sat4j.minisat.core.QueueTest;

import io.github.binsec.core.ACQ_CONACQ;
import io.github.binsec.core.ACQ_DCA;
import constracq.core.acqconstraint.ACQ_IConstraint;
import constracq.core.acqconstraint.ACQ_Network;
import constracq.core.acqconstraint.ConstraintFactory;
import constracq.core.acqconstraint.ConstraintFactory.ConstraintSet;
import constracq.core.learner.ACQ_Bias;
import constracq.core.learner.ACQ_Query;
import io.github.binsec.core.learner.Answer;

public class NeighborQueriesStats {

    private ACQ_Bias bias;
	private ArrayList<ACQ_Query> queries;
	private Map<ACQ_Query, Integer> minDistances = new LinkedHashMap<>();
	Map<ACQ_Network, Integer> MSSId = new LinkedHashMap<>();


    public NeighborQueriesStats(ACQ_CONACQ acquisition, ACQ_Bias bias){
		this.bias = bias;
		//this.queries = acquisition.getQueries();
    }
	public NeighborQueriesStats(ACQ_DCA acquisition, ACQ_Bias bias){
		this.bias = bias;
		this.queries = acquisition.getQueries();
    }

	
    public ACQ_Network getMSS(ACQ_Query e){
		ConstraintFactory fact = bias.getNetwork().getFactory();
		ACQ_Network MSS = new ACQ_Network(fact);
		for (ACQ_IConstraint c : bias.getConstraints()) {
			if (c.check(e)) {
				MSS.add(c, true);
			}
		}
		return MSS;
	}
   
	//calculer les distances entre les MSSes:
	public int distance(ACQ_Network MSS1, ACQ_Network MSS2){
		int d =0;
		for(ACQ_IConstraint c : MSS1.getConstraints()){
			if(!MSS2.contains(c)) d+=1;
		}
		return d;
	}
	

	//mapping <requete, voisins>
	public Map<ACQ_Query, ArrayList<ACQ_Query>> getNeighbors() {
        Map<ACQ_Query, ArrayList<ACQ_Query>> neighbors = new LinkedHashMap<>();
        int count =0;
        for (ACQ_Query e_i : queries) {
            ArrayList<ACQ_Query> closestNeighbors = new ArrayList<>();
            int minDistance = Integer.MAX_VALUE;
            ACQ_Network mss_i = getMSS(e_i);
            MSSId.put(mss_i, count);
            //System.out.println("MSSID = "+getMSSId(mss_i,MSSId));
            //System.out.println("count = "+ count);

            count++;
            for (ACQ_Query e_j : queries) {
                if (!e_i.equals(e_j)) {
                    ACQ_Network mss_j = getMSS(e_j);
                    int dist = distance(mss_i, mss_j);  
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestNeighbors.clear();
                        closestNeighbors.add(e_j);
                    } else if (dist == minDistance) {
                        closestNeighbors.add(e_j);
                    }
                }
            }
            neighbors.put(e_i, closestNeighbors);
			minDistances.put(e_i, minDistance);
        }
        return neighbors;
    }
	

	public Map<ACQ_Query,Integer> getMinDistances(){
		return minDistances;
	}

	public String mapToString(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Map<ACQ_Query,Integer> minDistances) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()) {
            ACQ_Query query = entry.getKey();
			int minDistance = minDistances.get(query);
            ArrayList<ACQ_Query> neighbors = entry.getValue();
            sb.append("Requête ").append(getMSS(query).toString()).append(" a pour voisins à distance : " + minDistance +"\n");
            for (ACQ_Query neighbor : neighbors) {
                sb.append("    - ").append(getMSS(neighbor).toString()).append("\n");
            }
        }
        return sb.toString();
    }

	//print the neighbors
	public String NumberNeighborsToString(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Map<ACQ_Query, Integer> minDistances, Boolean classif) {
		StringBuilder sb = new StringBuilder();
		Answer answer = Answer.YES;
		for (Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()) {
			ACQ_Query query = entry.getKey();
			int classifCount = 0;
			if (classif){
				if(query.getClassification() == answer) {
					int minDistance = minDistances.get(query);
					ArrayList<ACQ_Query> neighbors = entry.getValue();
					for (ACQ_Query neighbor : neighbors) {
						if (neighbor.getClassification() == answer) {
							classifCount++;
						}
					}
					sb.append("(").append(classifCount).append(",").append(neighbors.size())
					.append(",").append(minDistance+")"); //(query_index,#N_same_classification,#total_N,distance)
				}
			}else{
				if(query.getClassification() != answer) {
					int minDistance = minDistances.get(query);
					ArrayList<ACQ_Query> neighbors = entry.getValue();
					for (ACQ_Query neighbor : neighbors) {
						if (neighbor.getClassification() != answer) {
							classifCount++;
						}
					}
					sb.append("(").append(classifCount).append(",").append(neighbors.size())
					.append(",").append(minDistance+")"); //(query_index,#N_same_classification,#total_N,distance)
				}
			}
		}
		return sb.toString();
	}
	//get the number of pos or neg neighbors
	public Integer getNumberNeighborsSameClassif(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, ACQ_Query query) {
		int nbN = 0;
		ArrayList<ACQ_Query> neighbors = neighborsMap.get(query);
		for (ACQ_Query neighbor : neighbors) {
			if (neighbor.getClassification() == query.getClassification()) {
				nbN++;
			}
		}
		return nbN;
	}
	
	public Double getMeanPerCentDCA(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Boolean classif, ACQ_DCA acquisition) {
		
		int TotalClassifQueries =0;
		double SumPercent = 0.0;
		if(classif){
			TotalClassifQueries = acquisition.getNumberQueries(Answer.YES);
			for(Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()){
				ACQ_Query query = entry.getKey();
				if(query.getClassification()==Answer.YES){
					SumPercent += (double)getNumberNeighborsSameClassif(neighborsMap,query)*100/entry.getValue().size();
				}
			}
		}else{
			TotalClassifQueries = acquisition.getNumberQueries(Answer.NO)+acquisition.getNumberQueries(Answer.UKN);
			for(Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()){
				ACQ_Query query = entry.getKey();
				if(query.getClassification()!=Answer.YES){
					SumPercent += (double)getNumberNeighborsSameClassif(neighborsMap,query)*100/entry.getValue().size();
				}
			}

		}
		
		return (double)SumPercent/TotalClassifQueries;
	}
	
	public int getMSSId(ACQ_Network mss, Map<ACQ_Network, Integer> MapMSSId) {
		int id =0;
		for(Map.Entry<ACQ_Network, Integer> entry : MapMSSId.entrySet()) {
			ACQ_Network mssMap = entry.getKey();
			if(mssMap.equals(mss)) {
				id = entry.getValue();
				break;
			}
		}
		return id;
	}
	
	//afficher (mss_i,class_i) : (mss_k, class_k), (), (), .... "ces voisins"
	public String getMSSNeighborsWithClassification(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap) {
	    StringBuilder sb = new StringBuilder();
	    for (Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()) {
	        ACQ_Query query = entry.getKey();
	        ACQ_Network mss = getMSS(query);
	        Answer classification = query.getClassification();
	        
	        sb.append("(").append(getMSSId(mss,MSSId)).append(", ").append(classification.name()).append(") : ");
	        
	        ArrayList<ACQ_Query> neighbors = entry.getValue();
	        for (int i = 0; i < neighbors.size(); i++) {
	            ACQ_Query neighbor = neighbors.get(i);
	            ACQ_Network neighborMSS = getMSS(neighbor); 
	            Answer neighborClassification = neighbor.getClassification(); 
	            
	            sb.append("(").append(getMSSId(neighborMSS,MSSId)).append(", ")
	              .append(neighborClassification.name()).append(")");
	            
	            if (i < neighbors.size() - 1) {
	                sb.append(", "); 
	            }
	        }
	        sb.append("\n");
	    }

	    return sb.toString();
	}
	
	//afficher les queries par leur MSSes :
	public String toStringMSS(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap) {
	    StringBuilder sb = new StringBuilder();
	    for (Map.Entry<ACQ_Query, ArrayList<ACQ_Query>> entry : neighborsMap.entrySet()) {
	    	ACQ_Query query = entry.getKey();
	    	ACQ_Network queryMSS = getMSS(query);
	    	sb.append("(").append(getMSSId(queryMSS,MSSId)).append(", ").append(query.getClassification()).append("), ");
	    }
	    if (sb.length() > 0) {
	        sb.delete(sb.length() - 2, sb.length());
	    }
	    return sb.toString();
	}
	
	
	//find closest neighbors tout en les classifiant 
	public List<ACQ_Query> findClosestNeighborsPosPriority(ACQ_Query query, Set<ACQ_Query> visited, Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Answer classif) {
	    List<ACQ_Query> neighbors = new ArrayList<>();
	    ArrayList<ACQ_Query> allNeighbors = neighborsMap.getOrDefault(query, new ArrayList<>());
	    for (ACQ_Query neighbor : allNeighbors) {
	        if (!visited.contains(neighbor) && neighbor.getClassification() == classif) {
	            neighbors.add(neighbor);
	        }
	    }
	    for (ACQ_Query neighbor : allNeighbors) {
	        if (!visited.contains(neighbor) && neighbor.getClassification() != classif) {
	            neighbors.add(neighbor);
	        }
	    }
	    return neighbors;
	}
	//find closest neighbors of last visited MSS and with min dist, sorted pos first
	public List<ACQ_Query> findClosestUnvisitedGlobalPos(List<ACQ_Query> visitedList, Set<ACQ_Query> visited, Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap) {
	    List<ACQ_Query> closestQueries = new ArrayList<>();
	    int minDistance = Integer.MAX_VALUE;
	    for (int i = visitedList.size() - 1; i >= 0; i--) {
	        ACQ_Query lastVisited = visitedList.get(i);
	        for (ACQ_Query query : queries) {
	            if (!visited.contains(query)) {
	                int dist = distance(getMSS(lastVisited), getMSS(query));
	                if (dist < minDistance) {
	                    minDistance = dist;
	                    closestQueries.clear();
	                    closestQueries.add(query);
	                } else if (dist == minDistance) {
	                    closestQueries.add(query);
	                }
	            }
	        }
	        if (!closestQueries.isEmpty()) {
	            break;
	        }
	    }
	    
	    closestQueries.sort((a, b) -> {
	        if (a.getClassification() == Answer.YES && b.getClassification() != Answer.YES) {
	            return -1; 
	        } else if (a.getClassification() != Answer.YES && b.getClassification() == Answer.YES) {
	            return 1; 
	        }
	        return Integer.compare(getMSSId(getMSS(a),MSSId), getMSSId(getMSS(b),MSSId)); 
	    });
	    return closestQueries;
	}
	
	//TODO : MG (UKN not treated)
	//best order for MS (begin with pos neighbors)
	public String PosPriorityNGA(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Answer classif) {
	    StringBuilder sb = new StringBuilder();
	    Queue<ACQ_Query> queue = new LinkedList<>();
	    Set<ACQ_Query> visited = new HashSet<>();
	    List<ACQ_Query> visitedList = new ArrayList<>(); 
	    
	    for (ACQ_Query query : queries) {
	        if (query.getClassification() == classif) {
	            queue.add(query);
	            visited.add(query);
	            visitedList.add(query);
	            break;
	        }
	    }
	    while (!queue.isEmpty() || visited.size() < queries.size()) {
	        if (queue.isEmpty()) {
	            List<ACQ_Query> nextClosestQueries = findClosestUnvisitedGlobalPos(visitedList, visited, neighborsMap);
	            for (ACQ_Query nextQuery : nextClosestQueries) {
	                if (!visited.contains(nextQuery)) {
	                    queue.add(nextQuery);
	                    visited.add(nextQuery);
	                    visitedList.add(nextQuery);
	                }
	            }
	        }
	        if (!queue.isEmpty()) {
	            ACQ_Query currentQuery = queue.poll();
	            sb.append("(").append(getMSSId(getMSS(currentQuery),MSSId)).append(", ").append(currentQuery.getClassification()).append("), ");
	            List<ACQ_Query> closestNeighbors = findClosestNeighborsPosPriority(currentQuery, visited, neighborsMap, classif);
	            for (ACQ_Query neighbor : closestNeighbors) {
	                if (!visited.contains(neighbor)) {
	                    queue.add(neighbor);
	                    visited.add(neighbor);
	                    visitedList.add(neighbor);
	                }
	            }
	        }
	    }
	    if (sb.length() > 0) {
	        sb.delete(sb.length() - 2, sb.length());
	    }
	    return sb.toString();
	}

	//-----Worst Case-------------------------------------------------------------------------------------------------------------------------------------------------
	
	//find closest neighbors of last visited MSS and with min dist, sorted neg first
	public List<ACQ_Query> findClosestUnvisitedGlobalNeg(List<ACQ_Query> visitedList, Set<ACQ_Query> visited, Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap) {
	    List<ACQ_Query> closestQueries = new ArrayList<>();
	    int minDistance = Integer.MAX_VALUE;
	    for (int i = visitedList.size() - 1; i >= 0; i--) {
	        ACQ_Query lastVisited = visitedList.get(i);
	        for (ACQ_Query query : queries) {
	            if (!visited.contains(query)) {
	                int dist = distance(getMSS(lastVisited), getMSS(query));
	                if (dist < minDistance) {
	                    minDistance = dist;
	                    closestQueries.clear();
	                    closestQueries.add(query);
	                } else if (dist == minDistance) {
	                    closestQueries.add(query);
	                }
	            }
	        }
	        if (!closestQueries.isEmpty()) {
	            break;
	        }
	    }
	    
	    closestQueries.sort((a, b) -> {
	        if (a.getClassification() != Answer.YES && b.getClassification() == Answer.YES) {
	            return -1; 
	        } else if (a.getClassification() == Answer.YES && b.getClassification() != Answer.YES) {
	            return 1; 
	        }
	        return Integer.compare(getMSSId(getMSS(a),MSSId), getMSSId(getMSS(b),MSSId)); 
	    });
	    return closestQueries;
	}
	
	//find closest neighbors tout en les classifiant 
	public List<ACQ_Query> findClosestNeighborsNegPriority(ACQ_Query currentQuery, Set<ACQ_Query> visited, Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap) {
        List<ACQ_Query> closestNeighbors = new ArrayList<>();
        int minDistance = Integer.MAX_VALUE;

        if (neighborsMap.containsKey(currentQuery)) {
            for (ACQ_Query neighbor : neighborsMap.get(currentQuery)) {
                if (!visited.contains(neighbor)) {
                    int dist = distance(getMSS(currentQuery), getMSS(neighbor));
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestNeighbors.clear();
                        closestNeighbors.add(neighbor);
                    } else if (dist == minDistance) {
                        closestNeighbors.add(neighbor);
                    }
                }
            }
        }

        closestNeighbors.sort((q1, q2) -> {
            if (q1.getClassification() != Answer.YES && q2.getClassification() == Answer.YES) {
                return -1; 
            } else if (q1.getClassification() == Answer.YES && q2.getClassification() != Answer.YES) {
                return 1; 
            }
            return Integer.compare(getMSSId(getMSS(q1), MSSId), getMSSId(getMSS(q2), MSSId));
        });
        return closestNeighbors;
    }
	
	//TODO : MG (UKN not treated)
	//worst order for MS (begin with neg neighbors)
	public String NegPriorityNGA(Map<ACQ_Query, ArrayList<ACQ_Query>> neighborsMap, Answer classif) {
	    StringBuilder sb = new StringBuilder();
	    Queue<ACQ_Query> queue = new LinkedList<>();
	    Set<ACQ_Query> visited = new HashSet<>();
	    List<ACQ_Query> visitedList = new ArrayList<>();

	    for (ACQ_Query query : queries) {
	        if (query.getClassification() == classif) {
	            queue.add(query);
	            visited.add(query);
	            visitedList.add(query);
	            break;
	        }
	    }
	    while (!queue.isEmpty() || visited.size() < queries.size()) {
	        if (queue.isEmpty()) {
	            List<ACQ_Query> nextClosestQueries = findClosestUnvisitedGlobalNeg(visitedList, visited, neighborsMap);
	            for (ACQ_Query nextQuery : nextClosestQueries) {
	                if (!visited.contains(nextQuery)) {
	                    queue.add(nextQuery);
	                    visited.add(nextQuery);
	                    visitedList.add(nextQuery); 
	                }
	            }
	        }

	        if (!queue.isEmpty()) {
	            ACQ_Query currentQuery = queue.poll();
	            sb.append("(").append(getMSSId(getMSS(currentQuery),MSSId)).append(", ").append(currentQuery.getClassification()).append("), ");

	            List<ACQ_Query> closestNeighbors = findClosestNeighborsNegPriority(currentQuery, visited, neighborsMap);
	            for (ACQ_Query neighbor : closestNeighbors) {
	                if (!visited.contains(neighbor)) {
	                    queue.add(neighbor);
	                    visited.add(neighbor);
	                    visitedList.add(neighbor); 
	                }
	            }
	        }
	    }
	    if (sb.length() > 0) {
	        sb.delete(sb.length() - 2, sb.length());
	    }

	    return sb.toString();
	}

	
	
}
