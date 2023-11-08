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

from itertools import combinations
from lib.utils import format_input, tosmtlib
import argparse
import re

IJCAI22_BIAS = { "Valid_X", "Alias_X_Y", "Deref_X_Y", "Eq_X_0", "LT_X_0", "GT_X_0", "Eq_X_Y", "LT_X_Y", "GT_X_Y" }

def parse(filename):
    with open(filename, "r") as f:
        lines = f.readlines()

    res = {}
    for line in lines:
        if line.startswith("bias:"):
            val = [ c.strip() for c in line.split(":")[1].split(",") ]
            res["bias"] = val
        elif line.startswith("precond:"):
            val = line.split(":")[1].strip()
            res["precond"] = val
        elif line.startswith("nargs:"):
            val = int(line.split(":")[1].strip())
            res["nargs"] = val
        elif line.startswith("types:"):
            val = [ ("v%d"%(i), ty.strip()) for i, ty in enumerate(line.split(":")[1].split(",")) ]
            res["args"] = val

    if res.get("args") == None:
        res["args"] = [ ("v%d"%(i), "INT") for i in range(res["nargs"]) ]

    return res

def extend(bias, args):
    res = []
    for cst in bias:
        elems = cst.split("_")
        if elems[0] in { "Valid", "StrlenEq" }:
            for var, ty in args:
                if ty == "PTR":
                    res.append((cst, cst.replace("X", var)))

        elif elems[0] in { "Eq", "GT", "LT", "Mod" } and nargs(cst) == 1:
            for var, ty in args:
                if ty in { "INT", "UINT" }:
                    res.append((cst, cst.replace("X", var)))

        elif elems[0] in { "Alias", "Deref", "Overlap", "PtrGT", "PtrLT" }:
            for (var1, ty1), (var2, ty2) in combinations(args, 2):
                if ty1 == "PTR" and ty2 == "PTR":
                    res.append((cst, cst.replace("X", var1).replace("Y", var2)))

        elif elems[0] in { "Eq", "GT", "LT" } and nargs(cst) == 2:
            for (var1, ty1), (var2, ty2) in combinations(args, 2):
                if (ty1 == "INT" and ty2 == "INT") or (ty1 == "UINT" and ty2 == "UINT") :
                    res.append((cst, cst.replace("X", var1).replace("Y", var2)))
        else:
            assert False

    return res
            
def nargs(cst):
    args = set()
    for elem in cst.split("_"):
        if len(elem) == 1 and elem.isupper():
            args.add(elem)
    return len(args)

def get_bias(precond, bias, args, level):
    res = set()
    assert level in { "min", "avg" }
    for orig, cst in extend(bias, args):
        if tosmtlib(cst, args) in precond:
            if level == "avg":
                res.add(orig)
            else:
                res.add(cst)
    return res

def main(filename, level, ijcai22):
    data = parse(filename)

    ADDBIAS = set()
    for cst in data["bias"]:
        constants = [ int(i) for i in re.findall(r'\d+', cst) if int(i) != 0 ] 
        if len(constants) != 0:
            ADDBIAS.add(cst)

    if level == "max" and not ijcai22:
        res = data["bias"]

    elif level == "max" and ijcai22:
        res = ADDBIAS.union(IJCAI22_BIAS)

    else:
        bias = (IJCAI22_BIAS if ijcai22 else set(data["bias"])).union(ADDBIAS)
        res = get_bias(data["precond"], bias, data["args"], level)

    print("bias: " + ", ".join(res))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--file', required=True, type=str, help="configuration file")
    parser.add_argument('--lvl', required=True, type=str, help="level of the bias (min-ijcai22, avg-ijcai22, max-ijcai22, min, avg, max)")
    parser.add_argument('--ijcai22', action="store_true", help="Limit to the set of constraint from the IJCAI'22 paper")
    
    args = parser.parse_args()

    main(args.file, args.lvl, args.ijcai22)
