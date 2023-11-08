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


type Ast.Instr.t += Exit of Ast.Expr.t Ast.loc
type Ast.Instr.t += IRandom of Ast.Loc.t Ast.loc * Ast.Expr.t Ast.loc * Ast.Expr.t Ast.loc

type Ir.builtin += Exit_with of Dba.Expr.t
type Ir.builtin += SetRandom of Dba.Var.t * Dba.Expr.t * Dba.Expr.t

let () =
    Exec.register_plugin
        (module struct
            let name = "true-exit"

            let grammar_extension = [
                Dyp.Add_rules [
                    (
                        (
                            "instr", 
                            [
                                Dyp.Regexp (RE_String "exit");
                                Dyp.Regexp (RE_Char '(');
                                Dyp.Non_ter ("expr", No_priority);
                                Dyp.Regexp (RE_Char ')');
                            ],
                            "default_priority",
                            [] 
                        ),
                        fun _ -> function 
                        | [ _; _; Libparser.Syntax.Expr status; _; ] -> 
                                (Libparser.Syntax.Stmt [ Exit status; ], [])
                        | _ -> assert false 
                    );
                    (
                        (
                            "instr", 
                            [
                                Dyp.Non_ter ("loc", No_priority);
                                Dyp.Regexp (RE_String ":=");
                                Dyp.Regexp (RE_String "rand");
                                Dyp.Regexp (RE_Char '(');
                                Dyp.Non_ter ("expr", No_priority);
                                Dyp.Regexp (RE_Char ',');
                                Dyp.Non_ter ("expr", No_priority);
                                Dyp.Regexp (RE_Char ')');
                            ],
                            "default_priority",
                            [] 
                        ),
                        fun _ -> function 
                            | [ Libparser.Syntax.Loc loc; _; _; _; Libparser.Syntax.Expr e1; _; Libparser.Syntax.Expr e2; _; ] -> 
                                (Libparser.Syntax.Stmt [ IRandom (loc, e1, e2); ], [])
                        | _ -> assert false 
                    );
                ];
            ]

            let instruction_printer = Some (fun ppf -> function
            | Exit (status, _) ->
                Format.fprintf ppf "exit(%a)" Ast.Expr.pp status;
                true
            | IRandom ((loc, _), (e1, _), (e2, _)) ->
                Format.fprintf ppf "%a := random(%a, %a)" Ast.Loc.pp loc Ast.Expr.pp e1 Ast.Expr.pp e2;
                true
            | _ -> false)

            let declaration_printer = None

            let extension :
                type a b.
                (module Types.EXPLORATION_STATISTICS) ->
                (module Path.S with type t = a) ->
                (module Types.STATE with type t = b) ->
                (module Exec.EXTENSION with type path = a and type state = b) option =
                    fun stats path state ->
                        if Options.is_enabled () && Options.Engine.get () = Splugin.Concrete then
                            Some (module struct
                                module P = (val path)
                                module S = (val state)
                                module Stats = (val stats)
                                module Eval = Eval.Make (P) (S)
                                type path = P.t
                                and state = S.t

                                let initialization_callback = None

                                let declaration_callback = None

                                let instruction_callback = Some (fun inst env ->
                                    match inst with
                                    | Exit (Int z, _) ->
                                       [ Ir.Builtin (Exit_with (Dba.Expr.constant (Bitvector.create z 8))) ]
                                    | Exit status ->
                                            [ Ir.Builtin (Exit_with (Script.eval_expr status env)) ]
                                    | IRandom (lval, (Int min, _), (Int max, _)) ->
                                        (match Script.eval_loc lval env with
                                        | Var var -> 
                                            [ Ir.Builtin 
                                                (SetRandom (var,
                                                    (Dba.Expr.constant (Bitvector.create min 8)), 
                                                    (Dba.Expr.constant (Bitvector.create max 8))))
                                            ]
                                        | _ -> failwith "Can only get a random value into a variable")

                                    | _ -> [])

                                let process_callback = None

                                let exit_with status _addr path _depth state =
                                    Options.Logger.info "@[<v 2>Exploration@,%a@]" Stats.pp ();
                                    let code = (Bitvector.to_uint (Bitvector.extract (Eval.get_value status state path) { hi=7; lo=0 })) in
                                    if code == 0 then Format.printf "Exited with code: %d@." code
                                    else Format.printf "[EMUL ERROR] Exited with code: %d@." code; 
                                    exit 0

                                let genrandom (var, e1, e2) _addr path _depth state =
                                    let imin = (Bitvector.to_uint (Bitvector.extract (Eval.get_value e1 state path) { hi=7; lo=0 })) in
                                    let imax = (Bitvector.to_uint (Bitvector.extract (Eval.get_value e2 state path) { hi=7; lo=0 })) in
                                    let irand = (imin + Random.int (imax - imin)) in
                                    let rand = S.Value.constant (Bitvector.of_int ~size:64 irand) in
                                    Ok (S.assign var rand state)

                                let builtin_callback = Some (function
                                | Exit_with status -> Some (exit_with status)
                                | SetRandom (loc, e1, e2) -> Some (genrandom (loc, e1, e2))
                                | _ -> None)
                                
                                let builtin_printer = Some (fun ppf -> function
                                | Exit_with status ->
                                    Format.fprintf ppf "exit(%a)" Dba_printer.Ascii.pp_bl_term status;
                                    true
                                | SetRandom (_, e1, e2) ->
                                    Format.fprintf ppf "todo := rand(%a, %a)" Dba_printer.Ascii.pp_bl_term e1 Dba_printer.Ascii.pp_bl_term e2;
                                    true
                                | _ -> false)
                                
                                let at_exit_callback = None
                                            
                            end)
                                        
                        else None
        end : Exec.PLUGIN)
