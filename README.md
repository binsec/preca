# PreCA : Precondition Constraint Acquisition

PreCA performs precondition inference relying on constraint acquisition. PreCA does not need the source code of the function under analysis but only the binary.
PRECA is part of the BINSEC toolbox for binary-level program analysis and is build on top of the [constraint acquisition plateform](https://github.com/lirmm/ConstraintAcquisition).

# Table of Contents

1.  [Requirements](#requirements)
2.  [Installation](#installation)
3.  [Usage](#usage)
4.  [Experiments](#experiments)
    1. [IJCAI'22](ijcai23)
    2. [KR'23](kr23)
5.  [Run your own example](#run-your-own-example)
6.  [VM artifact (OUTDATED)](#vm-artifact-outdated)
7.  [Cite us](#cite-us)
8.  [References](#references)

# Requirements

The PreCA framework depends on:
- [The Java Runtime Environment](https://www.java.com/en/download/)
- [Maven](https://maven.apache.org/) if you want to recompile PreCA
- [Binsec](https://github.com/binsec/binsec) (>= 0.8.1) with Unisim\*
- Python 3 (to run the experiments)


\*We recommend to install Binsec through [opam](https://opam.ocaml.org/), by following these steps: 
```shell
cd path/to/preca
opam switch create . <OCAML_VERSION> # OCAML_VERSION < 5.0.0
opam pin ./binsecplugin/concrete/   # pin the libconcrete package and install its dependencies (Binsec, Unisim ...)
```

To install the python dependencies, run:
```
python3 -m venv path/to/venv
source path/to/venv/bin/activate
pip install -r requirements.txt
```

# Installation

To use PreCA the `PRECA_PATH` environment variable should be set:
```shell
# You can put the following line in your .bashrc file (or equivalent file)
export PRECA_PATH=/path/to/preca/directory   
```

## Run from JAR

The `preca.jar` file is available to use PreCA without compiling it. See the [usage](#usage) section to found out how to use it.

## Compile from source

You can compile the code using maven (the generated jar file is then in the `target` directory):
```shell
mvn clean compile assembly:single
cp target/<jarfile> preca.jar
```

# Usage

You can get PreCA's available options:
```shell
java -jar preca.jar -help
```

PreCA takes as input the path to the configuration file as follows:
```shell
java -jar preca.jar -file <config>
```

Such a configuration file enables to specify which binary and which function is analyzed. You can get the documentation for the configuration file format with:
```shell
java -jar preca.jar -helpconf
```

## Example

To check that your install works correctly, you can run PreCA over the given strcat example:
```shell
java -ea -jar preca.jar -file ./examples/strcat_conacq.txt
```

The output should end with:
```
*************Learned Network CL example ******
network var=4 cst=3
-------------------------
CONSTRAINTS:
Valid(var0)
Valid(var1)
NotOverlap(var0, var1)_or_StrlenEq0(var1)[0, 1, 2, 3]
-------------------------

*************Learned Network CL example (SMTLIB) ******
(and (valid v0) (valid v1) (or (not (overlap v0 v1)) (strleneq v1 #x00000000)))
```

# Experiments

We provide the needed datasets in the `datasets` directory and the script `scripts/bench.py` to replicate the results from our IJCAI'22 and KR'23 papers. 
The help is available through
```shell
python3 ./scripts/bench.py -h
```

Moreover, after running your experiments, you can always recompute the statistics as follows:
```shell
python3 ./scripts/recompute_stats.py --file <json-file> --timeout <seconds>
```

Finally, since our IJCAI'22 paper, we added new constraint to our constraint language. Still, as both our IJCAI'22 and KR'23 papers consider the original set of constraint, it is possible retrict to it through the `--ijcai22` option.

## IJCAI'22

To replicate experiments from our IJCAI'22 paper [1] run the following commands:
```
python3 ./scripts/bench.py --dataset ./datasets/ijcai22/nopost --timeout 3600 --emulto 5 --out <out json> --disj auto --biaslvl max --ijcai22
python3 ./scripts/bench.py --dataset ./datasets/ijcai22/post --timeout 3600 --emulto 5 --out <out json> --disj auto --biaslvl max --ijcai22
```

## KR'23

To replicate results from our KR'23 paper [2], run the following commands:
```
python3 ./scripts/bench.py --dataset ./datasets/kr2023 --timeout 3600 --emulto 5 --out <out json> --disj <disj> --biaslvl <lvl> --ijcai22
```

The `<disj>` option states which methods is used to handle disjunction (see the following table).  
The `<lvl>` states the size of the bias considered and takes value among `min`, `avg` and `max`.

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left">Abbreviation</th>
<th scope="col" class="org-left">Description</th>
</tr>
</thead>

<tbody>
<tr>
<td class="org-left">auto</td>
<td class="org-left">We rely on the heuristics from [1] to generate the considered disjunctions</td>
</tr>


<tr>
<td class="org-left">mss</td>
<td class="org-left">we use DCA [2] to handle disjunctions</td>
</tr>


<tr>
<td class="org-left"> n : int </td>
<td class="org-left">add disjunctions of size up to n</td>
</tr>


<tr>
<td class="org-left"> { i1, ..., in } </td>
<td class="org-left">add disjunctions of size i1, ..., in</td>
</tr>

</tbody>
</table>

# Run your own example

To run your own example, you must create a config file and a Binsec script associated to the function you want to analyze. 
You can take example to the given `examples/strcat_conacq.txt` and `examples/strcat_dca.txt` configuration files, 
as well as their corresponding Binsec script (`datasets/binsec_ini/ijcai22/nopost/strcat.ini`).

For more examples, check the `datasets` directory.

# VM artifact (OUTDATED)

The PreCA artifact for the paper [1] was first published as a a virtual machine (~2.6G), which is available [here](https://zenodo.org/records/6513522#.YnD1GnVfjmE). 
It contains the PreCA jar file, the datasets, and scripts to exercise PreCA and replay major experiments. The user and root password is "password".

# Cite us

```
@inproceedings{DBLP:conf/ijcai/MenguyBLG22,
  author       = {Gr{\'{e}}goire Menguy and
                  S{\'{e}}bastien Bardin and
                  Nadjib Lazaar and
                  Arnaud Gotlieb},
  editor       = {Luc De Raedt},
  title        = {Automated Program Analysis: Revisiting Precondition Inference through
                  Constraint Acquisition},
  booktitle    = {Proceedings of the Thirty-First International Joint Conference on
                  Artificial Intelligence, {IJCAI} 2022, Vienna, Austria, 23-29 July
                  2022},
  pages        = {1873--1879},
  publisher    = {ijcai.org},
  year         = {2022},
  url          = {https://doi.org/10.24963/ijcai.2022/260},
  doi          = {10.24963/ijcai.2022/260},
  timestamp    = {Wed, 27 Jul 2022 16:43:00 +0200},
  biburl       = {https://dblp.org/rec/conf/ijcai/MenguyBLG22.bib},
  bibsource    = {dblp computer science bibliography, https://dblp.org}
}
```

# References

1. [Menguy, Grégoire, et al. "Automated program analysis: Revisiting precondition inference through constraint acquisition." Proceedings of the Thirty-First International Joint Conference on Artificial Intelligence (IJCAI-ECAI 2022), Vienna, Austria. 2022.](https://www.ijcai.org/proceedings/2022/260)

2. [Menguy, Grégoire, et al. "Active disjunctive constraint acquisition." Proceedings of the International Conference on Principles of Knowledge Representation and Reasoning. Vol. 19. No. 1. 2023.](https://proceedings.kr.org/2023/50/)

