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
package io.github.binsec.core.learner;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * The scope of a constraint is a set of variables (one or more) that are
 * involved in the constraint. More generally, the scope of a network is all the
 * variables used among all the constraints of the network.
 */
public final class ACQ_Scope implements Iterable<Integer> {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((variables == null) ? 0 : variables.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACQ_Scope other = (ACQ_Scope) obj;
		if (variables == null) {
			if (other.variables != null)
				return false;
		} else if (!variables.equals(other.variables))
			return false;
		return true;
	}

	/**
	 * Empty scope
	 */
	public static ACQ_Scope EMPTY = new ACQ_Scope(new BitSet());

	/**
	 * BitSet structure is used to represent the variables of this scope.
	 * 
	 */
	private final BitSet variables;

	/**
	 * Empty constructor, initialize this scope with an empty BitSet
	 */
	public ACQ_Scope() {
		this(new BitSet());
	}

	/**
	 * Constructor from an Iterable over Integer elements
	 * 
	 * @param iterable Iterable over Integer elements
	 */
	public ACQ_Scope(Iterable<Integer> iterable) {
		this(new BitSet());
		iterable.iterator().forEachRemaining(x -> variables.set(x));
	}

	/**
	 * Constructor from a Spliterator over Integer elements
	 * 
	 * @param iteratorVar Spliterator over Integer elements
	 */
	public ACQ_Scope(Spliterator<Integer> iteratorVar) {
		this(new BitSet());
		iteratorVar.forEachRemaining(x -> variables.set(x));
	}

	/**
	 * Constructor from an int array or from multiples int
	 * 
	 * @param vars Arbitrary Number of Arguments
	 */
	public ACQ_Scope(int... vars) {
		this(new BitSet());
		for (int var : vars) {
			variables.set(var);
		}
	}

	/**
	 * Constructor from a List of scopes
	 * 
	 * @param vars Arbitrary Number of Arguments
	 */
	public ACQ_Scope(List<ACQ_Scope> vars) {
		this(new BitSet());
		for (ACQ_Scope scope : vars) {
			scope.iterator().forEachRemaining(x -> variables.set(x));
		}
	}

	/**
	 * Constructor from a specified BitSet
	 * 
	 * @param bs BitSet
	 */
	public ACQ_Scope(BitSet bs) {
		variables = bs;
	}

	/**
	 * Returns the amount of variable of this scope
	 * 
	 * @return Amount of variables
	 */
	public final int size() {
		return variables.cardinality();
	}

	/**
	 * Returns the index of the first bit that is set to true
	 * 
	 * @return index of the first bit set to true. If no such bit exists -1 is
	 *         returned
	 */
	public final int getFirst() {
		return variables.nextSetBit(0);
	}

	/**
	 * Returns the index of the second bit that is set to true
	 * 
	 * @return index of the second bit set to true. If no such bit exists -1 is
	 *         returned
	 */
	public final int getSecond() {
		int first = getFirst();
		return first < 0 ? -1 : variables.nextSetBit(first + 1);
	}

	/**
	 * Returns the index of the third bit that is set to true
	 * 
	 * @return index of the third bit set to true. If no such bit exists -1 is
	 *         returned
	 */
	public final int getThird() {
		int second = getSecond();
		return second < 0 ? -1 : variables.nextSetBit(second + 1);
	}

	/**
	 * Checks if the specified variable (identified by a number) is contained in
	 * this scope
	 * 
	 * @param var A variable
	 * @return true if the specified variable is contained in this scope
	 */
	public final boolean contains(int var) {
		return variables.get(var);
	}

	/**
	 * Checks if the variables of the specified scope are all included in this scope
	 * 
	 * @param anotherSet Scope to check such as anotherSet is included in this scope
	 * @return true if all the variables of the specified scope are contained in
	 *         this scope
	 */
	public final boolean containsAll(ACQ_Scope anotherSet) {

		// *******version 1********
		// for(int i = 0; i < anotherSet.variables.length(); i++) {
		// if(anotherSet.variables.get(i)) {
		// if(variables.get(i)) {
		// // var is in this and anotherSet
		// }else {
		// // var is not in this
		// return false;
		// }
		// }
		// }
		// return true;

		// *******version 2********
		BitSet bs = anotherSet.variables;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			// operate on index i here
			if (!contains(i)) {
				return false;
			}
			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}
		return true;

		// return anotherSet.variables.intersects(variables);
	}

	/**
	 * Returns a new scope that contains both variables of this scope and the
	 * specified scope
	 * 
	 * @param other Scope
	 * @return A new scope with the variables of this scope and the specified scope
	 */
	// etc. toutes les fonctions utiles de comparaisons d'ensembles de variables
	public final ACQ_Scope union(ACQ_Scope other) {
		BitSet bs = (BitSet) variables.clone();
		bs.or(variables);
		bs.or(other.variables);
		return new ACQ_Scope(bs);
	}

	/*
	 * public final IntStream stream() { return variables.stream(); }
	 */
	/**
	 * Returns an iterator over the variables of this scope
	 * 
	 * @return An iterator over the variables of this scope
	 */
	@Override
	public final Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			int cur = variables.nextSetBit(0);

			@Override
			public boolean hasNext() {
				return cur >= 0;
			}

			@Override
			public Integer next() {
				int old = cur;
				cur = variables.nextSetBit(old + 1);
				return old;
			}
		};
	}

	@Override
	public Spliterator<Integer> spliterator() {
		return variables.stream().spliterator();
	}

	/**
	 * Split this scope into two new scopes. The two scopes have no variables in
	 * common and the union of these two scopes returns the original scope.
	 * 
	 * @return An array of two scopes
	 */
	public ACQ_Scope[] split() {
		assert size() > 1;
		/*
		 * Spliterator<Integer> spliterator = spliterator(); Spliterator<Integer>
		 * spliterator2 = spliterator.trySplit();
		 */
		BitSet bs, bs1 = new BitSet(), bs2 = new BitSet();
		int count = 0;
		bs = bs1;
		for (Iterator<Integer> it = variables.stream().iterator(); it.hasNext();) {
			bs.set(it.next());
			count++;
			if (count == variables.cardinality() / 2)
				bs = bs2;
		}

		return new ACQ_Scope[] { new ACQ_Scope(bs1), new ACQ_Scope(bs2) };
	}

	public ACQ_Scope[] shuffleSplit() {
		assert size() > 1;
		BitSet bs = new BitSet(), bs1 = new BitSet(), bs2 = new BitSet();
		int count = 0;
		bs = bs1;

		// save all indexes into an list
		List<Integer> orderedList = new ArrayList<Integer>();
		List<Integer> shuffledList = new ArrayList<Integer>();
		for (Iterator<Integer> it = variables.stream().iterator(); it.hasNext();) {
			int j = it.next();
			orderedList.add(j);
			shuffledList.add(j);
		}

		// shuffle this list
		do {
			Collections.shuffle(shuffledList);
		} while (shuffledList.equals(orderedList));

		// iterate over the shuffled list
		for (int i : shuffledList) {
			bs.set(i);
			count++;
			// when on the half of the list, change the bitset
			if (count == variables.cardinality() / 2)
				bs = bs2;
		}
		return new ACQ_Scope[] { new ACQ_Scope(bs1), new ACQ_Scope(bs2) };
	}

	@Override
	public String toString() {
		return variables.toString();
	}

	/**
	 * *****TODO FIX THE SIZE OF THE RETURNED ARRAY***** ***** DONE
	 * NL:23-06-19-20:40*********************
	 * 
	 * For a specified query, it returns the values of this query regarding this
	 * scope
	 * 
	 * @param query An example
	 * @return Values of the variables of this scope in the specified query
	 */
	public int[] getProjection(ACQ_Query query) {
		int index = 0;

		int[] tmp = new int[variables.size()];
		int size = 0;
		for (Iterator<Integer> it = iterator(); it.hasNext();) {
			size++;
			tmp[index++] = query.getValue(it.next());
		}

		int[] values = new int[size];
		for (int i = 0; i < size; i++)
			values[i] = tmp[i];

		return values;

	}

	public BitSet getVariables() {
		return variables;
	}

	public ACQ_Scope diff(ACQ_Scope scope) {
		BitSet bs = (BitSet) scope.variables.clone();
		bs.andNot(variables);
		return new ACQ_Scope(bs);
	}

	public boolean intersect(ACQ_Scope scope) {
		BitSet bs = (BitSet) scope.variables.clone();
		return bs.intersects(variables);
	}

	public void expend() {

		variables.set(variables.nextClearBit(0));

	}

	public ACQ_Scope[] split_into(int nb) {

		ACQ_Scope[] results = new ACQ_Scope[nb];

		int D = variables.cardinality() / (nb + 1);
		int R = variables.cardinality() % (nb + 1);

		int size[] = new int[nb];

		for (int i = 0; i < nb; i++) {
			if (R >= 0) {
				size[i] = D + 1;
				R--;
			} else
				size[i] = D;
		}

		BitSet bs;
		BitSet[] bstab = new BitSet[nb];

		for (int i = 0; i < nb; i++)
			bstab[i] = new BitSet();
		int count = 0;
		bs = bstab[0];
		int i = 0;
		for (Iterator<Integer> it = variables.stream().iterator(); it.hasNext();) {
			bs.set(it.next());
			count++;
			if (i < nb - 1 && count == size[i]) {
				i++;
				bs = bstab[i];
				count = 0;
			}
		}

		for (int j = 0; j < nb; j++)
			results[j] = new ACQ_Scope(bstab[j]);

		return results;

	}

	public CopyOnWriteArrayList<ACQ_Scope> split_into(int nb, int size, int index) {

		int index_ = index;
		int nb_slices = nb / size;
		CopyOnWriteArrayList<ACQ_Scope> results = new CopyOnWriteArrayList<>();
		BitSet bs = new BitSet();

		int count = 0;
		int tmp = 0;
		for (int i = 0; count < nb_slices && i < nb; i++) {
			bs.set(index_);
			index_ = (index_ + 1) % nb;
			tmp++;
			if (tmp >= size) {

				results.add(new ACQ_Scope(bs));
				bs = new BitSet();
				tmp = 0;
				count++;
			}
		}

		return results;

	}

	private static void helper(CopyOnWriteArrayList<ACQ_Scope> combinations, int data[], int start, int end,
			int index) {
		if (index == data.length) {
			ACQ_Scope combination = new ACQ_Scope(data.clone());
			combinations.add(combination);
		} else if (start <= end) {
			data[index] = start;
			helper(combinations, data, start + 1, end, index + 1);
			helper(combinations, data, start + 1, end, index);
		}
	}

	/****************
	 * k combinations of a set of n elements
	 * 
	 * @param n
	 * @param r
	 * @return
	 */
	public static CopyOnWriteArrayList<ACQ_Scope> generate(int n, int r) {
		CopyOnWriteArrayList<ACQ_Scope> combinations = new CopyOnWriteArrayList<>();
		helper(combinations, new int[r], 0, n - 1, 0);
		return combinations;
	}

}
