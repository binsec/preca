############################################################################
#  This file is part of PRECA.                                             #
#  PRECA is part of the BINSEC toolbox for binary-level program analysis.  #
#                                                                          #
#  Copyright (C) 2019-2023                                                 #
#    CEA (Commissariat à l'énergie atomique et aux énergies                #
#         alternatives)                                                    #
#                                                                          #
#  you can redistribute it and/or modify it under the terms of the GNU     #
#  Lesser General Public License as published by the Free Software         #
#  Foundation, version 2.1.                                                #
#                                                                          #
#  It is distributed in the hope that it will be useful,                   #
#  but WITHOUT ANY WARRANTY; without even the implied warranty of          #
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           #
#  GNU Lesser General Public License for more details.                     #
#                                                                          #
#  See the GNU Lesser General Public License version 2.1                   #
#  for more details (enclosed in the file licenses/LGPLv2.1).              #
#                                                                          #
############################################################################

from plumbum import local, ProcessExecutionError
from z3 import BitVec, Solver, And, Or, sat, unsat, unknown
from tqdm import tqdm
from math import sqrt
import argparse
import json
import re

def mean(l):
    if len(l) == 0:
        return None
    l = [ i if i != None else 0 for i in l ]
    return round(float(sum(l)) / len(l), 2)

def standard_deviation(l):
    if len(l) == 0:
        return None
    m = mean(l)
    res = 0
    for e in l:
        assert e > 0
        res += (e - m)**2 
    return round(sqrt(float(res) / len(l)), 2)

def parse_results(results):
    parsed = json.loads(results)
    return parsed

def getnargs(conffile):
    with open(conffile, "r") as f:
        nargslines = [ l.strip() for l in f.readlines() if l.startswith("nargs") ]

    assert(len(nargslines) == 1)

    return int(nargslines[0].split(":")[1].strip())

def display_results(arg):
    
    globals().update(arg)

    print("""Mean convergence time: {} s \u00B1 {}""".format(mean(convTimes), standard_deviation(convTimes)))
    print("""Min convergence time: {} s""".format(min(convTimes) if len (convTimes) > 0 else None))
    print("""Max convergence time: {} s""".format(max(convTimes) if len (convTimes) > 0 else None))
    print("""Total convergence time: {} s\n""".format(sum(convTimes) if len (convTimes) > 0 else None))

    print("Mean number of query {}".format(mean(nqueries)))
    print("Mean query generation time {} s".format(mean(mean_querygens)))
    print("Mean time rate to learn first constraint {}%".format(mean(mean_first_learned_rate)))
    print("Mean number of positive queries {}".format(mean(mean_nb_pos_query)))
    print("Mean number of negative queries {}\n".format(mean(mean_nb_neg_query)))
    
    print("Mean clause size: {}".format(mean(mean_clause_size)))
    print("Min clause size: {}".format(min(mean_clause_size) if len(mean_clause_size) > 0 else None))
    print("Max clause size: {}\n".format(max(mean_clause_size) if len(mean_clause_size) > 0 else None))

    print("#Collapse: {} / {}".format(ncollapse, ntasks))
    print("#Timeout: {} / {}".format(ntimeout, ntasks))
    print("#Timeout but found a sound approx: {} / {}".format(ntimeouted_but_find, ntasks))
    print("#Not equiv: {} - {} / {}".format(notequiv, notequiv+equiv_ukn, len(convTimes)))
    print("#Not implies: {} - {} / {}".format(notimplies, notimplies+implies_ukn, len(convTimes)))
    print("#False: {} - {} / {}\n".format(nfalse, nfalse+nfalse_ukn, len(convTimes)))
    print("#Ask timeouted: {}".format(n_answer_timeouted))

def nb_args(expr):
    return max([ int(i) for i in re.findall(r"x(\d+)", expr) ]) +1
    
def stats(data, timeout):
    ntasks = 0
    ntimeout = 0
    ntimeouted_but_find = 0
    convTimes = []
    mean_querygens = []
    nqueries = []
    notequiv = 0
    equiv_ukn = 0
    notimplies = 0
    implies_ukn = 0
    nfalse = 0
    nfalse_ukn = 0
    mean_first_learned_rate = []
    mean_nb_pos_query = []
    mean_nb_neg_query = []
    n_answer_timeouted = 0
    mean_clause_size = []
    ncollapse = 0

    for i in data.keys():
        ntasks += 1
        datanargs = getnargs(data[i]["conffile"])

        if data[i].get("error") == "yes":
            continue

        if data[i]["collapse"] == "yes":
            ncollapse += 1
            continue

        assert data[i]["timeouted"] in ["yes", "no"]

        toprocess = True

        if data[i]["timeouted"] == "yes" or (timeout != None and data[i]["convTime"] > timeout): 
                ntimeout += 1
                if data[i].get("last_sound_network_time") != None:
                    if data[i]["network"] != None and (timeout == None or data[i]["last_sound_network_time"] < timeout):
                        ntimeouted_but_find += 1
                    else:
                        toprocess = False
                else:
                    toprocess = False
        #if data[i]["timeouted"] == "no" and (timeout == None or data[i]["convTime"] <= timeout):
        
        if toprocess:
            assert(data[i]["network"] != None)
            convTimes.append(data[i]["convTime"])
            mean_querygens.append(data[i]["query_gen_mean"])
            nqueries.append(data[i]["nb_queries"])

            if data[i].get("time_first_constr_learned") != None:
                mean_first_learned_rate.append((data[i]["time_first_constr_learned"] * 100.0) / data[i]["convTime"])

            mean_nb_pos_query.append(data[i]["nb_pos_queries"])
            mean_nb_neg_query.append(data[i]["nb_neg_queries"])

            mean_clause_size.append(data[i]["clause_size"])

            assert data[i]["equiv"] in [ "yes", "no", "ukn" ] 
            if data[i]["equiv"] == "no":
                notequiv += 1
            elif data[i]["equiv"] == "ukn":
                equiv_ukn += 1
            #else:
            #    print(data[i]["conffile"])

            assert data[i]["implies"] in [ "yes", "no", "ukn" ]
            if data[i]["implies"] == "no":
                notimplies += 1
            elif data[i]["implies"] == "ukn":
                implies_ukn += 1

            assert data[i]["isfalse"] in [ "yes", "no", "ukn" ] 
            if data[i]["isfalse"] == "yes":
                nfalse += 1
            elif data[i]["isfalse"] == "ukn":
                nfalse_ukn += 1

    
    argument = {
        "ntasks": ntasks,
        "convTimes": convTimes,
        "mean_querygens": mean_querygens, 
        "nqueries": nqueries, 
        "notequiv": notequiv, 
        "equiv_ukn": equiv_ukn, 
        "notimplies": notimplies, 
        "implies_ukn": implies_ukn, 
        "nfalse": nfalse, 
        "nfalse_ukn": nfalse_ukn, 
        "ntimeout": ntimeout, 
        "ntimeouted_but_find": ntimeouted_but_find,
        "mean_first_learned_rate": mean_first_learned_rate, 
        "mean_nb_pos_query": mean_nb_pos_query, 
        "mean_nb_neg_query": mean_nb_neg_query,
        "n_answer_timeouted": n_answer_timeouted,
        "mean_clause_size": mean_clause_size,
        "ncollapse": ncollapse,
    }

    display_results(argument)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--file', required=True, type=str, help="results file (json)")
    parser.add_argument('--timeout', required=False, type=int, help="max convergence time")
    args = parser.parse_args()

    timeout = args.timeout

    data = json.load(open(args.file, "r"))
    stats(data, args.timeout)

