/*
 * Created on 23 mai 07 by coletta 
 *
 */
package constracq.core.combinatorial;

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
