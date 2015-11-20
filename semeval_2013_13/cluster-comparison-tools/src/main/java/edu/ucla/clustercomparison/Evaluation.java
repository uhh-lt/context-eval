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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A functional interface for evaluation methods that compare two sense
 * inventories.  
 */
public interface Evaluation {

    /**
     * Returns a mapping from each instance to its score according to this
     * evaluation
     *
     * @param testInstances the set of instances shared by both keys that should
     *        be evaluated
     * @param termToNumSenses a mapping from each lemma form to the number of
     *        senses that it has
     *
     * @see KeyUtil for a description of how keys are represented
     */
    public Map<String,Double> test(
        Map<String,Map<String,Map<String,Double>>> test,
        Map<String,Map<String,Map<String,Double>>> gold,
        Set<String> testInstances,
        Map<String,Integer> termToNumSenses);
}
