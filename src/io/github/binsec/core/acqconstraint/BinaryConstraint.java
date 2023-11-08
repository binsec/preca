/****************************************************************************/
/*  This file is part of PRECA.                                             */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.  */
/*                                                                          */
/*  Copyright (C) 2019-2023                                                 */
/*    CEA (Commissariat à l'énergie atomique et aux énergies                */
/*         alternatives)                                                    */
/*                                                                          */
/*  you can redistribute it and/or modify it under the terms of the GNU     */
/*  Lesser General Public License as published by the Free Software         */
/*  Foundation, version 2.1.                                                */
/*                                                                          */
/*  It is distributed in the hope that it will be useful,                   */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of          */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           */
/*  GNU Lesser General Public License for more details.                     */
/*                                                                          */
/*  See the GNU Lesser General Public License version 2.1                   */
/*  for more details (enclosed in the file licenses/LGPLv2.1).              */
/*                                                                          */
/****************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.binsec.core.acqconstraint;

/**
 *
 * @author agutierr
 */
public abstract class BinaryConstraint extends ACQ_Constraint{

    public BinaryConstraint(String name,int var1,int var2) {
        super(name,new int[]{var1,var2});
    }
    public BinaryConstraint(String name, BinaryConstraint cst1, BinaryConstraint cst2,int[] variables) {
    	super(name,cst1,cst2,variables);
    
    }
	@Override
	public boolean check(int... value) {
        return check(value[0],value[1]);
    }
    protected abstract boolean check(int value1,int value2);
    
}
