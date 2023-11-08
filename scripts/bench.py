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
from z3 import BitVec, sat, unsat, unknown, Solver
from itertools import combinations
from tqdm import tqdm
from math import sqrt
from pathlib import Path
from enum import Enum
import argparse
import json
import re
import sys
import signal

RESULTS = {}
OUT = None

conacq = local["./scripts/launch_preca.sh"]
computestats = local["python3"]["./scripts/recompute_stats.py"]

def parse_results(results):
    parsed = json.loads(results)
    return parsed

def check(smtlib, datasetsmtlib, nargs, types, scopes, tocheck="equiv", size=32):
    smtstr =  """\
(set-option :print-success false)
(declare-fun mem () (Array (_ BitVec 32) (_ BitVec 32)))
(declare-datatype OptBitVec ((none) (some (value (_ BitVec 32)))))
(declare-datatype PtrCell ((tup (ref (OptBitVec)) (value (_ BitVec 32)) (strlen (_ BitVec 32)))))
(declare-datatype IntCell ((tup (ref (OptBitVec)) (value (_ BitVec 32)))))

; Define constraints
(define-fun valid ((x PtrCell)) (Bool) (distinct (value x)  #x00000000))
(define-fun alias ((x PtrCell) (y PtrCell)) (Bool) (= (value x)  (value y)))
(define-fun deref ((x PtrCell) (y PtrCell)) (Bool) (and ((_ is (some ((_ BitVec 32)) (OptBitVec))) (ref y)) (= (value x)  (value (ref y)))))
(define-fun ptrgt ((x PtrCell) (y PtrCell)) (Bool) (bvugt (value x)  (value y)))
(define-fun ptrlt ((x PtrCell) (y PtrCell)) (Bool) (bvult (value x)  (value y)))
(define-fun strleneq ((x PtrCell) (y (_ BitVec 32))) (Bool) (= (strlen x) y))
(define-fun overlap ((x PtrCell) (y PtrCell)) (Bool) 
    (or (and (bvule (value x) (value y)) 
            (bvugt (bvadd (value x) (bvadd #x00000001 (strlen x))) (value y))) 
        (and (bvule (value y) (value x)) 
            (bvugt (bvadd (value y) (bvadd #x00000001 (strlen y))) (value x)))))

(define-fun eq ((x IntCell) (y (_ BitVec 32))) (Bool) (= (value x) y))
(define-fun eq ((x IntCell) (y IntCell)) (Bool) (= (value x) (value y)))

(define-fun sgt ((x IntCell) (y (_ BitVec 32))) (Bool) (bvsgt (value x) y))
(define-fun ugt ((x IntCell) (y (_ BitVec 32))) (Bool) (bvugt (value x) y))

(define-fun sgt ((x IntCell) (y IntCell)) (Bool) (bvsgt (value x) (value y)))
(define-fun ugt ((x IntCell) (y IntCell)) (Bool) (bvugt (value x) (value y)))

(define-fun slt ((x IntCell) (y (_ BitVec 32))) (Bool) (bvslt (value x) y))
(define-fun ult ((x IntCell) (y (_ BitVec 32))) (Bool) (bvult (value x) y))

(define-fun slt ((x IntCell) (y IntCell)) (Bool) (bvslt (value x) (value y)))
(define-fun ult ((x IntCell) (y IntCell)) (Bool) (bvult (value x) (value y)))

(define-fun smod ((x IntCell) (y (_ BitVec 32)) (z (_ BitVec 32))) (Bool) (= (bvsrem (value x) y) z))
(define-fun umod ((x IntCell) (y (_ BitVec 32)) (z (_ BitVec 32))) (Bool) (= (bvurem (value x) y) z))

; Define the uniterpreted function sum
(declare-fun sum 
        ((Array (_ BitVec 32) (_ BitVec 32)) (_ BitVec 32) IntCell)
        (_ BitVec 32))

; Define the uniterpreted function for unknown constraint
(declare-fun uknconstr () Bool)


"""
    for i in range(nargs):
        if types[i] == "PTR":
            smtstr += "\n(declare-fun v{} () PtrCell)\n".format(i)
            smtstr += "(assert (=> (not (valid v{0})) (= (strlen v{0}) #x00000000)))\n".format(i)
            smtstr += "(assert (bvult (value v{0}) (bvadd #x00000001 (bvadd (value v{0}) (strlen v{0})))))\n".format(i)
        elif types[i] in { "INT", "UINT" }:
            smtstr += "\n(declare-fun v{} () IntCell)\n".format(i)
        else:
            assert False


        if not scopes[i]:
            # Non global variables have no reference address
            smtstr += "(assert ((_ is (none () (OptBitVec))) (ref v{})))\n".format(i)
        else:
            # Global variables have reference addresses
            smtstr += "(assert ((_ is (some ((_ BitVec 32)) (OptBitVec))) (ref v{})))\n".format(i)
            smtstr += "(assert (distinct (value (ref v{})) #x00000000))\n".format(i)
            smtstr += "(assert (= (select mem (value (ref v{0}))) (value v{0})))\n".format(i)

    smtstr += "\n"

    ptr_indexes = [i for i in range(nargs) if types[i] == "PTR" ]
    for comb in combinations(ptr_indexes, 2):
        smtstr += "(assert (=> (overlap v{0} v{1}) (= (bvadd (value v{0}) (strlen v{0})) (bvadd (value v{1}) (strlen v{1})))))\n".format(comb[0], comb[1])

    globindexes = [ i for i in range(nargs) if scopes[i] ]
    for comb in combinations(globindexes, 2):
        smtstr += "(assert (distinct (value (ref v{})) (value (ref v{}))))\n".format(comb[0], comb[1])

    if tocheck == "equiv": smtstr += "(assert (distinct {} {}))\n".format(smtlib, datasetsmtlib)
    else: smtstr += "(assert (and {} (not {})))\n".format(smtlib, datasetsmtlib)
    smtstr += "(check-sat)\n(exit)"

    solv = Solver()
    solv.set("timeout", 60000) # 60 s
    solv.from_string(smtstr)
    res = solv.check()
    if res == sat:
        return "no"
    elif res == unsat:
        return "yes"
    elif res == unknown:
        return "ukn"
    else:
        assert False

def parseconf(conffile):
    with open(conffile, "r") as f:
        lines = [ l.strip() for l in f.readlines() if l.strip() != "" ]
    
    res = {}
    for line in lines:
        if line.startswith("expr") or line.startswith("precond"):
            strval = line.split(":")[1].strip()
            res["expr"] = strval
        elif line.startswith("bin"):
            strval = line.split(":")[1].strip()
            res["bin"] = strval
        elif line.startswith("nargs"):
            strval = line.split(":")[1].strip()
            res["nargs"] = int(strval)
        elif line.startswith("types"):
            strval = line.split(":")[1].strip()
            res["types"] = [ s.strip() for s in strval.split(",") if s.strip() != "" ]
        elif line.startswith("globals"):
            strval = line.split(":")[1].strip()
            res["globals"] = [ True if scope.strip() == "true" else False for scope in strval.split(",") ]

    return res

def getMaxClauseSize(network):
    res = 1
    for line in network.split("\n"):
        res = max(res, 1+line.count("_or_"))
    return res

def getDisjSet(cst_expr):
    res = set()
    for constr in cst_expr:
        res.add(1+constr.count("\/"))

    res = res.difference(set([1]))
    res = res if len(res) > 0 else 1
    return str(res)

def bench(conffiles, timeout, conacq_args, outfile, passive, nruns, biaslevel, ijcai22):
    global RESULTS, OUT
    disj = conacq_args["disj"]
    strat = conacq_args["strat"]
    back = conacq_args["back"]
    emulto = conacq_args["emulto"]

    for j in tqdm(range(len(conffiles)*nruns), leave=False):
        i = j // nruns
        conffile = conffiles[i]
        if len(RESULTS) > j and RESULTS[str(j)]["conffile"] == conffile: continue
        parsed = parseconf(conffile)
        smtlibexpr = parsed["expr"]
        binary = parsed.get("bin")
        nargs = parsed["nargs"]
        scopes = parsed["globals"]
        types = parsed["types"]

        retcode, res, stderr = conacq["-file", conffile,
            "-disj", getDisjSet(cst_expr) if disj == "optim" else disj,
            "-emulto", emulto,
            "-strat" if strat else None,
            "-back" if back else None,
            "-passive" if passive else None,
            ("-timeout", timeout) if timeout != None else None,
            "-biaslvl", biaslevel,
            "-ijcai22" if ijcai22 else None,
        ].run(retcode=(0, 1, 124))

        if retcode == 1:
            print(stderr)
            parsed = {}
            parsed["conffile"] = conffile
            parsed["timeouted"] = "no"
            parsed["timeout"] = timeout
            parsed["network"] = None
            parsed["error"] = "yes"
            parsed["collapse"] = "no"

        elif retcode == 124:
            parsed = {}
            parsed["conffile"] = conffile
            parsed["timeouted"] = "yes"
            parsed["timeout"] = timeout
            parsed["network"] = None
            parsed["collapse"] = "no"

        else: 
            parsed = parse_results(res)

            if parsed["network"] == None:
                parsed["conffile"] = conffile
                parsed["collapse"] = "yes" if parsed["timeouted"] == "no" else "no"
                parsed["timeout"] = timeout

            else:
                parsed["conffile"] = conffile
                parsed["target"] = smtlibexpr
                parsed["collapse"] = "no"
                
                parsed["equiv"] = check(parsed["smtlib"], smtlibexpr, nargs, types, scopes)

                equivnotsimpl = check(parsed["smtlib_not_simpl"], smtlibexpr, nargs, types, scopes)
                if parsed["equiv"] != equivnotsimpl: 
                    print("[ERROR] simplification changed semantics from {} to {}".format(equivnotsimpl, parsed["equiv"]))

                parsed["isfalse"] = check(parsed["smtlib"], "false", nargs, types, scopes)

                if parsed["equiv"] == "yes":
                    parsed["implies"] = "yes"
                else:
                    parsed["implies"] = check(parsed["smtlib"], smtlibexpr, nargs, types, scopes, "implies")

                if parsed["timeouted"] == "no" and not parsed["equiv"]:
                    print("[ERROR] Inferred precondition for {} is not equivalent to ground truth".format(conffile), file=sys.stderr)

                parsed["clause_size"] = getMaxClauseSize(parsed["network"])

        RESULTS[str(j)] = parsed
        json.dump(RESULTS, open(OUT, "w"), indent=4, sort_keys=True) 

    return RESULTS

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--dataset', required=True, type=str, help="dataset of expressions (directory)")
    parser.add_argument('--timeout', required=False, type=int, help="timeout for each exec in second")
    parser.add_argument('--emulto', required=True, type=int, help="timeout for queries in second")
    parser.add_argument('--nruns', required=False, type=int, help="number of time to run (default = 1)")
    parser.add_argument('--disj', required=False, type=str, help="Add disjunction constraints")
    parser.add_argument('--strat', action="store_true", help="Add query generation strategy")
    parser.add_argument('--back', action="store_true", help="Add background knowledge")
    parser.add_argument('--biaslvl', type=str, help="Set bias size (min, avg, max)")
    parser.add_argument('--passive', action="store_true", help="Run in passive mode")
    parser.add_argument('--ijcai22', action="store_true", help="Limit to the set of constraint from the IJCAI'22 paper")
    parser.add_argument('--out', required=True, type=str, help="Results output file")
    args = parser.parse_args()

    timeout = args.timeout
    OUT = args.out

    conacq_args = {
        "disj" : args.disj if args.disj != None else "auto",
        "strat" : args.strat,
        "back" : args.back,
        "emulto": args.emulto,
    }
    
    conffiles = [ f.resolve().as_posix() for f in Path(args.dataset).glob("*.txt") ]

    nruns = args.nruns if args.nruns != None else 1
    biaslevel = args.biaslvl if args.biaslvl != None else "max"

    if Path(OUT).exists():
        with open(OUT, "r") as f:
            RESULTS = json.load(f)

    results = bench(conffiles, timeout, conacq_args, OUT, args.passive, nruns, biaslevel, args.ijcai22)
    json.dump(results, open(args.out, "w"), indent=4, sort_keys=True) 
    print(computestats[
        "--file", args.out, 
        ("--timeout", args.timeout) if args.timeout != None else None,
    ](), end="")
