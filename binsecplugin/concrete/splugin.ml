(****************************************************************************)
(*  This file is part of PRECA.                                             *)
(*  PRECA is part of the BINSEC toolbox for binary-level program analysis.  *)
(*                                                                          *)
(*  Copyright (C) 2019-2023                                                 *)
(*    CEA (Commissariat à l'énergie atomique et aux énergies                *)
(*         alternatives)                                                    *)
(*                                                                          *)
(*  you can redistribute it and/or modify it under the terms of the GNU     *)
(*  Lesser General Public License as published by the Free Software         *)
(*  Foundation, version 2.1.                                                *)
(*                                                                          *)
(*  It is distributed in the hope that it will be useful,                   *)
(*  but WITHOUT ANY WARRANTY; without even the implied warranty of          *)
(*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *)
(*  GNU Lesser General Public License for more details.                     *)
(*                                                                          *)
(*  See the GNU Lesser General Public License version 2.1                   *)
(*  for more details (enclosed in the file licenses/LGPLv2.1).              *)
(*                                                                          *)
(****************************************************************************)

open Options
open Types

type Options.Engine.t += Concrete

let pluginname = "concrete"

let () = 
    Options.Engine.register pluginname Concrete (
        fun () ->  
                if MaxDepth.is_default () then MaxDepth.set max_int;
                if Concrete_options.PermFile.is_set () then Perm.setpermissions ();
                (module Env.State : STATE_FACTORY)
        )
