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

exception Undef = Types.Undef
exception Uninterp = Types.Uninterp
exception Non_mergeable = Types.Non_mergeable

module Bv = Term.Bv

type _ Types.value += Concrete : Bitvector.t Types.value

type 'a test = 'a Types.test =
  | True of 'a
  | False of 'a
  | Both of { t : 'a; f : 'a }

module SMap = Basic_types.String.Map
module IMap = Basic_types.Int.Map

module Memory = Lmap.Make (struct
  type t = Bv.t

  let equal = Bv.equal

  let len x = (Bv.size_of x) lsr 3

  let crop ~lo ~hi bv = Bv.extract ~lo:(lo*8) ~hi:(((hi+1)*8)-1) bv 

  let concat = Bv.append

end)


let my_random size = 
    if Random.bool () then
        Bv.zeros size
    else
        Bv.rand size

module State (QS : Types.QUERY_STATISTICS) :
  Types.RAW_STATE with type Value.t = Bitvector.t = struct
  module Uid = struct
    (* Useless because we never create symbolic variables *)
    type t = Suid.t

    let zero = Suid.zero
    let succ = Suid.incr
    let compare = Suid.compare
  end

  module Value = struct
    type t = Bitvector.t

    let kind = Concrete
    let constant = Fun.id
    let var _ _ size = my_random size
    let unary = Bv.unary
    let binary = Bv.binary
    let ite cond v1 v2 = if Bv.is_zero cond then v2 else v1
  end

  type t = {
    vsymbols : Bv.t IMap.t;
    (* collection of visible symbols *)
    varrays : Memory.t SMap.t;
    (* collection of visible arrays *)
    vmemory : Memory.t;
    (* visible memory *)
  }

  let pp_array ppf memory =
      Format.pp_open_vbox ppf 2;
      Memory.iter (fun addr value -> 
        Format.fprintf ppf "%s -> %a@," (Z.format "%#x" addr) Bv.pp_hex_or_bin value) 
      memory;
      Format.pp_close_box ppf ()


  let pp ppf state = 
            IMap.iter
                 (fun id value ->
                   match Dba.Var.from_id id with
                   | { name; info; _ } when info <> Temp -> 
                           Format.fprintf ppf "%s = %a@," name Bv.pp_hex_or_bin value
                   | _ | (exception Not_found) -> ())
                 state.vsymbols;
            Format.fprintf ppf "MEM_0:@,%a" pp_array state.vmemory;
            SMap.iter (fun name mem -> 
                Format.fprintf ppf "%s :@,%a" name pp_array mem)
                state.varrays

  let empty () =
    {
      vsymbols = IMap.empty;
      varrays = SMap.empty;
      vmemory = Memory.empty;
    }

  let alloc ~array t = { t with varrays = SMap.add array Memory.empty t.varrays }

  let assign ({ id; _ } : Types.Var.t) value t =
    { t with vsymbols = IMap.add id value t.vsymbols }

  let bswap =
    let rec iter e i r =
      if i = 0 then r
      else
        iter e (i - 8) (Bv.append (Bv.extract ~hi:(i - 1) ~lo:(i - 8) e) r)
    in
    fun e ->
      let size = Bv.size_of e in
      assert (size land 0x7 = 0);
      iter e (size - 8) (Bv.extract ~hi:(size - 1) ~lo:(size - 8) e)

  let chg_endianess value dir = match (dir: Machine.endianness) with
  | LittleEndian -> value
  | BigEndian -> bswap value

  let write ~addr value dir t =
      if not (Perm.check addr Perm.Write) then begin
          Printf.printf "[EMUL ERROR] Invalid memory write\n"; exit 0;
      end else
    { t with vmemory = Memory.store (Bv.value_of addr) (chg_endianess value dir) t.vmemory }

  let store name ~addr value dir t =
    match SMap.find name t.varrays with
    | exception Not_found -> raise_notrace (Uninterp name)
    | ar ->
        {
          t with
          varrays =
            SMap.add name (Memory.store (Bv.value_of addr) (chg_endianess value dir) ar) t.varrays;
        }

  let lookup ({ id; _ } as var : Types.Var.t) t =
    try IMap.find id t.vsymbols with Not_found -> raise_notrace (Undef var)

  let miss memory addr len = 
      let generated = my_random (len*8) in
      memory := Memory.store addr generated !memory;
      generated

  let read ~addr bytes dir t =
      if not (Perm.check addr Perm.Read) then begin 
          Printf.printf "[EMUL ERROR] Invalid memory read\n"; exit 0;
      end else
    let new_mem = ref t.vmemory in
    let miss = miss new_mem in
    let value = (chg_endianess (Memory.select miss (Bv.value_of addr) bytes t.vmemory) dir) in
    value, if !new_mem == t.vmemory then t else { t with vmemory = !new_mem }

  let select name ~addr bytes dir t =
      (* TODO should I ensure perm? *)
    try
      let arr = SMap.find name t.varrays in
      let new_mem = ref arr in
      let miss = miss new_mem in
      let value = (chg_endianess (Memory.select miss (Bv.value_of addr) bytes arr) dir) in
      value, if !new_mem == arr then t else { t with varrays = SMap.add name !new_mem t.varrays }
    with Not_found -> raise_notrace (Uninterp name)

  let blit offset buf len over =
      (* TODO: Should I ensure perm ?*)
    let s = Bigarray.Array1.dim buf in
    if len <= s then
      let buf = Bigarray.Array1.sub buf 0 len in
      let bvbuff = Bv.create (Z.of_bits (String.init len (fun i ->
              Char.unsafe_chr (Bigarray.Array1.unsafe_get buf i))))
              (len lsl 3)
      in
      Memory.store offset bvbuff over
    else
      if s = 0 then Memory.store offset (Bv.zeros (len*8)) over
      else
        let bvbuff = Bv.create (Z.of_bits (String.init s (fun i ->
                Char.unsafe_chr (Bigarray.Array1.unsafe_get buf i))))
                (len lsl 3) (* bitvec re stored in little endian so padding with 0s only mean to increase the size of the Bv *)
        in
        Memory.store offset bvbuff over

  let memcpy ~addr len buf t = { t with vmemory = blit (Bv.value_of addr) buf len t.vmemory }
  let merge ~parent:_ _ _ = raise_notrace Non_mergeable
  let assertions _ = []

  let assume test t = (* TODO redef  a partir de expect *)
      if Bv.is_one test then Some t else None

  let test test t =
      if Bv.is_one test then True t else False t

  let get_value e _ = e

  let get_a_value = get_value 

  let enumerate e ?n:_ ?except:_ t = [(e, t)]

  let _expect e bv t =
      if Bv.equal e bv then Some t else None 

  let pp_smt _ _ _ = ()

  let to_formula _ = Formula.empty

  let downcast _ = None
end

