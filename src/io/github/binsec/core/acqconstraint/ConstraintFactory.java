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

package io.github.binsec.core.acqconstraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.github.binsec.core.ACQ_Utils;
import io.github.binsec.core.learner.ACQ_Query;


/**
 * This class acts as a constraint repository. It register constraints and
 * provides iterable sets of constraints. because of this, the nested class
 * ConstraintSet can provide set of constraints treated more efficiently with
 * BitSet logical operators
 * 
 *
 * @author agutierr
 */
public class ConstraintFactory {
	/*
	 * mapping constraint id (String) to numeral (Integer)
	 */
	HashMap<String, Integer> constraintsIndex = new HashMap<>();
	ArrayList<ACQ_IConstraint> constraintArray = new ArrayList<>();

	public ConstraintFactory() {
	}

	/*
	 * Each constraint is associated to an integer constraint .toString() is
	 * used as identifier TODO a ACQ_IConstraint witch deliver an ID string ?
	 */
	public synchronized int registerConstraint(ACQ_IConstraint cst) {
		Integer id = constraintsIndex.get(cst.toString());
		if (id == null) // register new constraint
		{
			constraintArray.add(cst);
			int newId = constraintArray.size() - 1;
			int size = constraintsIndex.size();
			constraintsIndex.put(cst.toString(), newId);
			if (size == constraintsIndex.size())
				new Exception().printStackTrace();
			return newId;
		} else { // replace constraint
			constraintArray.set(id, cst);
		}
		return id;
	}

	/*
	 * constraint can be registerd lazily
	 */
	public int getConstraintId(ACQ_IConstraint cst) {
		Integer id = constraintsIndex.get(cst.toString());
		if (id != null)
			return id;
		else
			return registerConstraint(cst);
	}

	public ACQ_IConstraint getConstraint(int id) {
		return constraintArray.get(id);
	}

	/*
	 * 
	 */
	public ConstraintSet createSet() {
		return new ConstraintSet();
	}

	public ConstraintSet createSet(ACQ_IConstraint... cstr_set) {
		return new ConstraintSet(cstr_set);
	}

	public ConstraintSet createSet(ConstraintSet anotherSet) {
		return new ConstraintSet(anotherSet);
	}

	public class ConstraintSet implements Iterable<ACQ_IConstraint> {
		BitSet constraintSet = null;

		private ConstraintSet() {
			constraintSet = new BitSet();
		}

		private ConstraintSet(ACQ_IConstraint... cstr_set) {
			constraintSet = new BitSet();
			for (ACQ_IConstraint c : cstr_set)
				constraintSet.set(getConstraintId(c));
		}

		private ConstraintSet(ConstraintSet anotherSet) {
			constraintSet = new BitSet();
			constraintSet.or(anotherSet.constraintSet);
		}

		/**
		 * Returns the set of constraints violated by query_bgd
		 * 
		 * @param query_bgd
		 *            Positive or negative example
		 * @return set of constraints violated by query_bgd
		 */
		public ConstraintSet getKappa(ACQ_Query query_bgd) {
			ConstraintSet set = createSet();
			for (ACQ_IConstraint cst : this) {
				if (query_bgd.scope.containsAll(cst.getScope())) {
					if (!((ACQ_Constraint) cst).check(query_bgd)) {
						set.add(cst);
					}
				}
			}
			return set;
		}

		public int[] getVariables() {
			BitSet bs=new BitSet();
			for (ACQ_IConstraint cst : this)
				for(int numvar:cst.getVariables())
					bs.set(numvar);
			return ACQ_Utils.bitSet2Int(bs);
		}
 
		public boolean check(ACQ_MetaConstraint candidate) {

			for (ACQ_IConstraint cst : this)
				if (cst instanceof ACQ_MetaConstraint)
					if (cst.getName().equals(candidate.getName())
							&& ((ACQ_MetaConstraint) cst).constraintSet.equals(candidate.constraintSet))
						return true;
			return false;
		}

		/**
		 * Add a constraint to this set of constraints
		 * 
		 * @param cst
		 *            constraint
		 */
		public void add(ACQ_IConstraint cst) {
			int id = getConstraintId(cst);
			constraintSet.set(id);
		}

		/**
		 * Remove a constraint from this set of constraint
		 * 
		 * @param cst
		 *            constraint
		 * @return true if the constraint has been successfully removed
		 */
		public void remove(ACQ_IConstraint cst) {
			int id = getConstraintId(cst);
			constraintSet.clear(id);
		}

		/**
		 * Returns the size of this set of constraints
		 * 
		 * @return size of this set of constraints
		 */
		public int size() {
			return constraintSet.cardinality();
		}

		public ACQ_IConstraint get_Constraint(int id) {
			BitSet tmp = (BitSet)this.constraintSet.clone();
			for (int i = 0; i < id; i++) {
				int nbs = tmp.nextSetBit(0);
				tmp.clear(nbs);
			}
			int realid = tmp.nextSetBit(0);
			return constraintArray.get(realid);
		}
		
		/**
		 * <p>
		 * Retains only the elements in this set that are contained in the
		 * specified set (optional operation). In other words, removes from this
		 * set all of its elements that are not contained in the specified set.
		 * If the specified collection is also a set, this operation effectively
		 * modifies this set so that its value is the intersection of the two
		 * sets.
		 * </p>
		 * 
		 * @param other_set
		 *            set containing elements to be retained in this set
		 */
		public void retainAll(ConstraintSet other_set) {
			constraintSet.and(other_set.constraintSet);
		}

		/**
		 * Checks if this set of constraints is empty
		 * 
		 * @return true if this set is empty
		 */
		public boolean isEmpty() {
			return constraintSet.isEmpty();
		}

		/**
		 * Remove from this set of constraints all constraints contained in the
		 * specified set
		 * 
		 * @param temp_kappa
		 *            set of constraints to remove
		 */
		public void removeAll(ConstraintSet temp_kappa) {
			constraintSet.andNot(temp_kappa.constraintSet);
		}

		public boolean contains(ACQ_IConstraint candidate) {
			int id = getConstraintId(candidate);
			return (constraintSet.get(id));
		}
		
		public boolean contains(ConstraintSet candidate) {
			for (ACQ_IConstraint c : candidate) {
				if (!contains(c)) {
					return false;
				}
			}
			return true;
		}

		public void addAll(ConstraintSet set) {
			constraintSet.or(set.constraintSet);
		}

		@Override
		public String toString() {
			return "ConstraintSet [constraints=" + constraintSet + "]";
		}
		
		
		public String toString2() {
			return "ConstraintSet [constraints=" + new ACQ_Network(new ConstraintFactory(), this) + "]";
		}
		

		public ConstraintSet getNextLevelCandidates(int level) {

			ConstraintSet newLevel= createSet();


			if(level==1) {
				for(ACQ_IConstraint cst: this) {
					if(!(cst instanceof ACQ_MetaConstraint))
						newLevel.add(cst);
				}

				return newLevel;
			}
			for(ACQ_IConstraint cst: this) {
				if(cst instanceof ACQ_ConjunctionConstraint)
					if(((ACQ_ConjunctionConstraint) cst).getNbCsts()==level)
						newLevel.add(cst);
			}

			return newLevel;
		}
		
		public int get_levels() {

			int max_level=1;

			for(ACQ_IConstraint cst: this)

				if(cst instanceof ACQ_ConjunctionConstraint) 

					max_level= (max_level> ((ACQ_ConjunctionConstraint) cst).getNbCsts()) ?
							max_level : ((ACQ_ConjunctionConstraint) cst).getNbCsts();


			return max_level;
		}

		/**
		 * Returns an iterator of this set of constraints
		 * 
		 * @return iterator of this set of constraints
		 */
		public Iterator<ACQ_IConstraint> iterator() {
			return new Iterator<ACQ_IConstraint>() {
				int index = -2;

				@Override
				public boolean hasNext() {
					if (index == -1)
						return false;
					if (index == -2)
						return constraintSet.nextSetBit(0) >= 0;
					else
						return constraintSet.nextSetBit(index + 1) >= 0;
				}

				@Override
				public ACQ_IConstraint next() {
					if (index == -1)
						throw new NoSuchElementException();
					if (index == -2)
						index = constraintSet.nextSetBit(0);
					else
						index = constraintSet.nextSetBit(index + 1);
					if (index == -1)
						throw new NoSuchElementException();
					else
						return getConstraint(index);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();

				}
			};
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			for (ACQ_IConstraint c : this)
			result = prime * result + c.hashCode();
			
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
			ConstraintSet other = (ConstraintSet) obj;
			
			if (this.size() != other.size()) {
				return false;
			}
			
			for (ACQ_IConstraint c : this) {
				if (!other.contains(c)) {
					return false;
				}
			}
			
			return true;
		}
	}
}
