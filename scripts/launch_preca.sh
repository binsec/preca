#!/bin/bash
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

FILE=""
DISJ="auto"
STRAT=false
BACK=false
TIMEOUT="3600"
EMULTO="5"
ACTIVE=true
BIASLEVEL="max"
IJCAI22=""

POSITIONAL=()

while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -file)
        FILE="$2"
        shift # past argument
        shift # past value
        ;;
        -disj)
        DISJ="$2"
        shift # past argument
        shift # past value
        ;;
        -emulto)
        EMULTO="$2"
        shift # past argument
        shift # past value
        ;;
        -strat)
        STRAT=true
        shift # past argument
        ;;
        -back)
        BACK=true
        shift # past argument
        ;;
        -passive)
        ACTIVE=false
        shift # past argument
        ;;
        -timeout)
        TIMEOUT="$2"
        shift # past argument
        shift # past value
        ;;
        -biaslvl)
        BIASLEVEL="$2"
        shift # past argument
        shift # past value
        ;;
        -ijcai22)
        IJCAI22="--ijcai22"
        shift # past argument
        ;;
        *)    # unknown option
        POSITIONAL+=("$1") # save it in an array for later
        shift # past argument
        ;;
    esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

STR=$(cat $FILE)

conf="""\
${STR//bias:/\#bias:}
$(python3 ./scripts/get_bias.py --file $FILE --lvl $BIASLEVEL $IJCAI22)

active: $ACTIVE 
disj: $DISJ
strat: $STRAT
back: $BACK
verbose: false
json: true
timeout: $TIMEOUT
emultimeout: $EMULTO
action_emultimeout: false
simplify: false"""

conffilename="/tmp/$(basename $FILE).$(date +"%H-%M-%S-%N")"
echo "$conf" > $conffilename

timeout $(($TIMEOUT * 2)) java -ea -jar ./preca.jar -file $conffilename

res=$?

rm $conffilename

exit $res
