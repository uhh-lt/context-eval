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

import java.util.Map;
import java.util.Set;


/**
 * An interface for algorithms that convert one sense labeling into another,
 * using a set of specified instances to learn the mapping procedure.
 */
public interface KeyMapper {

    /**
     * Performs a supervised mapping from the senses in the test key file to the
     * gold standard senses using only the specified instances for constructing
     * the mapping, and then remaps all non-training instances in the test key
     * into the gold key labelings, returning the resulting key.
     *
     * @param goldKey a mapping from words to the instances and their
     *        corresponding gold standard sense ratings
     * @param testKey a mapping from words to the instances and their
     *        corresponding induced sense ratings
     * @param trainingInstanceIds the set of instances IDs in the gold key set
     *        that should be used to construct the induced-to-gold sense mapping
     *
     * @return the sense mapping for all non-training instances using the
     *         induced keys that were coverted to the gold standard labels
     */
    Map<String,Map<String,Map<String,Double>>>
        convert(Map<String,Map<String,Map<String,Double>>> goldKey,
                Map<String,Map<String,Map<String,Double>>> testKey,
                Set<String> trainingInstanceIds);
}
