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

type perm_t = Write | Read | Exec

val setpermissions : unit -> unit

val check : Bitvector.t -> perm_t -> bool
