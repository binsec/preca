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
 * Created on 23 mai 07 by coletta 
 *
 */
package io.github.binsec.core.combinatorial;

import java.util.Iterator;

public class PermutationIterator  implements Iterator<int[]> {
    
    int n;
    int r;
    CombinationIterator combinationIterator;
    AllPermutationIterator permutationIterator;
    int[] currentCombination;
    int[] currentPermutation;
    
    public PermutationIterator (int n, int r) {
        this.n = n;
        this.r = r;
        combinationIterator = new CombinationIterator(n,r);
        permutationIterator = new AllPermutationIterator(r);
        currentCombination = combinationIterator.next();
        currentPermutation = new int[r];
    }
    
    public boolean hasNext() {
        return combinationIterator.hasNext()||permutationIterator.hasNext();
    }

    public int[] next() {
        if (!permutationIterator.hasNext()) {
            currentCombination = combinationIterator.next();
            permutationIterator.reset();
        }
        int[] permutationIndex = permutationIterator.next();
        for (int i = 0; i < r; i++) {
            currentPermutation[i]=currentCombination[permutationIndex[i]];
        }
        return currentPermutation;
    }

    public void remove() {
    }
    
    
    
}
