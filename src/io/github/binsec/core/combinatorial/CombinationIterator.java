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
 * Created on 22 mai 07 by coletta 
 *
 */
package io.github.binsec.core.combinatorial;

import java.math.BigInteger;
import java.util.Iterator;

/*
 * From http://www.merriampark.com/comb.htm
 */

public class CombinationIterator implements Iterator<int[]> {
    private int[] a;
    private int n;
    private int r;
    private int startIndex;
    private BigInteger numLeft;
    private BigInteger total;

    //------------
    // Constructor
    //------------

    public CombinationIterator (int n, int r, int startIndex) {
      this.startIndex = startIndex;
      if (r > n) {
        throw new IllegalArgumentException ();
      }
      if (n < 1) {
        throw new IllegalArgumentException ();
      }
      this.n = n;
      this.r = r;
      a = new int[r];
      BigInteger nFact = getFactorial (n);
      BigInteger rFact = getFactorial (r);
      BigInteger nminusrFact = getFactorial (n - r);
      total = nFact.divide (rFact.multiply (nminusrFact));
      reset ();
    }
    
    public CombinationIterator (int n, int r) {
        this(n,r,0);
    }

    //------
    // Reset
    //------

    public void reset () {
      for (int i = 0; i < a.length; i++) {
        a[i] = i;
      }
      numLeft = new BigInteger (total.toString ());
    }

    //------------------------------------------------
    // Return number of combinations not yet generated
    //------------------------------------------------

    public BigInteger getNumLeft () {
      return numLeft;
    }

    //-----------------------------
    // Are there more combinations?
    //-----------------------------

    public boolean hasNext () {
      return numLeft.compareTo (BigInteger.ZERO) == 1;
    }

    //------------------------------------
    // Return total number of combinations
    //------------------------------------

    public BigInteger getTotal () {
      return total;
    }

    //------------------
    // Compute factorial
    //------------------

    private static BigInteger getFactorial (int n) {
      BigInteger fact = BigInteger.ONE;
      for (int i = n; i > 1; i--) {
        fact = fact.multiply (new BigInteger (Integer.toString (i)));
      }
      return fact;
    }

    //--------------------------------------------------------
    // Generate next combination (algorithm from Rosen p. 286)
    //--------------------------------------------------------

    public int[] next () {

      if (numLeft.equals (total)) {
        numLeft = numLeft.subtract (BigInteger.ONE);
        return shift(a);
      }

      int i = r - 1;
      while (a[i] == n - r + i) {
        i--;
      }
      a[i] = a[i] + 1;
      for (int j = i + 1; j < r; j++) {
        a[j] = a[i] + j - i;
      }

      numLeft = numLeft.subtract (BigInteger.ONE);
      return  shift(a);
    }
    
    private int[] shift(int[] a) {
        if (startIndex != 0) {
            int[] copy = new int[a.length];
            for (int j = 0; j < copy.length; j++) copy[j] = a[j]+startIndex;
            return copy;
        }
        return a;
    }



    public void remove() {
        // TODO Auto-generated method stub
        
    }
}
