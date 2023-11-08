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

module PermFile = Concrete_options.PermFile
module Bv = Bitvector

type perm_t = Write | Read | Exec

let readable = ref None
let writable = ref None
let executable = ref None

let parse_permissions str = 
    let vals = String.split_on_char '-' str in
    match vals with
    | min::max::[] -> 
            ((Bv.of_hexstring (String.trim min)), (Bv.of_hexstring (String.trim max)))
    | _ -> failwith "Cannot parse permission file"

let setpermissions () =
    let permfile = PermFile.get () in
    let fd = open_in permfile in
    try
        while true; do
            let line = input_line fd in
            if String.starts_with ~prefix:"readable:" line then
                match (String.split_on_char ':' line) with
                | _::value::_ -> readable := Some (parse_permissions value)
                | _ -> assert false
            else if String.starts_with ~prefix:"writable:" line then
                match (String.split_on_char ':' line) with
                | _::value::_ -> writable := Some (parse_permissions value)
                | _ -> assert false
            else if String.starts_with ~prefix:"executable:" line then
                match (String.split_on_char ':' line) with
                | _::value::_ -> executable := Some (parse_permissions value)
                | _ -> assert false
        done
    with End_of_file ->
        flush stdout;
        close_in fd;;

let check addr perm = match perm with
| Read -> 
        begin match !readable with
        | Some (min, max) -> (Bv.uge addr min) && (Bv.ule addr max)
        | None -> true
        end
| Write -> 
        begin match !writable with
        | Some (min, max) -> (Bv.uge addr min) && (Bv.ule addr max)
        | None -> true
        end
| Exec -> assert false

