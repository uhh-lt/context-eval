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
 * Utility class for mapping objects to indices
 */
class Indexer<K> extends HashMap<K,Integer> {

    /**
     * Returns the item associated with index {@code i} or {@code null} if it is
     * unassociated with any value.
     */
    public K find(int i) {
        for (Map.Entry<K,Integer> e : entrySet())
            if (e.getValue().intValue() == i)
                return e.getKey();
        return null;
    }

    /**
     * Returns the index of {@code k} or code -1 if {@code k} was not present.
     */
    public int lookup(K k) {
        Integer i = get(k);
        return (i == null) ? -1 : i;
    }

    /**
     * Returns the index to which {@code k} is assigned, mapping {@code k} to a
     * new index if it is currently not assigned one.
     */
    public int index(K k) {
        Integer i = get(k);
        if (i == null) {
            i = size();
            put(k, i);
        }
        return i;
    }

    public void indexAll(Collection<K> items) {
        for (K k : items)
            index(k);
    }
}
