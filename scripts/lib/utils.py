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

def format_input(i):
    try:
        return "#x%08x"%(int(i))
    except:
        return i

def tosmtlib(cst, args):
    elems = cst.split("_")
    if elems[0] in { "GT", "LT" }:
        arg = elems[1]
        argtype = [ ty for v, ty in args if v == arg ][0]
        if argtype == "UINT":
            return "(u{} {} {})".format(elems[0].lower(), format_input(elems[1]), format_input(elems[2]))

        elif argtype == "INT":
            return "(s{} {} {})".format(elems[0].lower(), format_input(elems[1]), format_input(elems[2]))

        else:
            assert False
    elif elems[0] == "Mod":
        arg = elems[1]
        argtype = [ ty for v, ty in args if v == arg ][0]
        if argtype == "UINT":
            return "(u{} {} {} {})".format(elems[0].lower(), format_input(elems[1]), format_input(elems[2]), format_input(elems[3]))

        elif argtype == "INT":
            return "(s{} {} {} {})".format(elems[0].lower(), format_input(elems[1]), format_input(elems[2]), format_input(elems[3]))

        else:
            assert False

    else:
        return "({} {})".format(elems[0].lower(), " ".join([ format_input(i) for i in elems[1:]]))
