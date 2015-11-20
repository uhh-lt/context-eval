/*
 * Copyright 2013 David Jurgens
 *
 * This file is part of the Cluster-Comparison package and is covered under the
 * terms and conditions therein.
 *
 * The Cluster-Comparison package is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation and distributed hereunder to
 * you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.clustercomparison;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for counting instances of an object
 */
class Counter<K> extends HashMap<K,Integer> {
    
    /**
     * The total number of objects counted by this instance.
     */
    private int sum = 0;

    /**
     * Counts {@code k} and returns the number of times that {@code k} has been
     * counted (including this count).
     */
    public int count(K k) {
        Integer i = get(k);
        if (i == null) 
            i = 0;
        put(k, i + 1);
        sum++;
        return i;
    }

    /**
     * Counts all items in {@code c}.
     */
    public void countAll(Collection<K> c) {
        for (K k : c)
            count(k);
        //return r;
    }

    /**
     * Returns the number of times {@code key} has been counted
     */
    @Override public Integer get(Object key) {
        Integer i = super.get(key);
        return (i == null) ? 0 : i;
    }
    
    /**
     * Returns the probability of seeing {@k} among all the items that have been
     * counted thusfar.
     */
    public double getProbability(K k) {
        Integer i = get(k);
        if (i == null) 
            i = 0;
        return (sum == 0) ? 0 : i / ((double)sum);
    }

    public K min() {
        K min = null;
        int count = Integer.MAX_VALUE;
        for (Map.Entry<K,Integer> e : entrySet()) {
            if (e.getValue() < count) {
                count = e.getValue();
                min = e.getKey();
            }
        }
        return min;
    }


    public K max() {
        K max = null;
        int count = 0;
        for (Map.Entry<K,Integer> e : entrySet()) {
            if (e.getValue() > count) {
                count = e.getValue();
                max = e.getKey();
            }
        }
        return max;
    }

    /**
     * Returns how many items have been counted by this instance.
     */
    public int sum() {
        return sum;
    }
}
