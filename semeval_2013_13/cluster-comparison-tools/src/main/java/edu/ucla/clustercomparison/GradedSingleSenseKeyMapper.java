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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.*;

import java.util.logging.Logger;


/**
 * An implementation of the remapping procedure described in <a
 * href="http://cs.ucla.edu/~jurgens/papers/jurgens-2012-evaluation-of-graded-wsd-using-wsi.corrected.pdf">Jurgens
 * (2012)</a> that learns the sense mapping between two labelings by taking into
 * account their respective sense weightings and then outputs a <b>single
 * sense</b> for the sense assignment of each instance, using the highest
 * weighted sense produced from the mapping.
 */
public class GradedSingleSenseKeyMapper implements KeyMapper {

    /**
     * The {@link KeyMapper} that does the distribution-based remapping, from
     * which this class selects the highest weighted sense.
     */
    private final GradedReweightedKeyMapper mapper;
    
    public GradedSingleSenseKeyMapper() {
        mapper = new GradedReweightedKeyMapper();
    }

    /**
     * Performs a supervised mapping from the senses in the test key file to the
     * gold standard senses using only the specified instances for constructing
     * the mapping, and then remaps all non-training instances in the test key
     * into the gold key labelings, returning the resulting key with single
     * sense assignments.
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
    public Map<String,Map<String,Map<String,Double>>>
        convert(Map<String,Map<String,Map<String,Double>>> goldKey,
                Map<String,Map<String,Map<String,Double>>> testKey,
                Set<String> trainingInstanceIds) {

        // Get the graded sense assignments from the mapping using the regular
        // approach
        Map<String,Map<String,Map<String,Double>>> outputKey 
            = mapper.convert(goldKey, testKey, trainingInstanceIds);

        // Then remove all but the highest weighted sense
        for (Map<String,Map<String,Double>> m : outputKey.values()) {
            for (Map<String,Double> m2 : m.values()) {
                clean(m2);
            }
        }
        
        return outputKey;
    }

    /**
     * Removes all but the highest-weighted sense from the sense ratings,
     * deterministically breaking ties by keeping the sense whose label is
     * lexicographically least.
     */
    private void clean(Map<String,Double> senseRatings) {
        if (senseRatings.isEmpty())
            return;

        Double max = 0d;
        Set<String> senses = new HashSet<String>();
        for (Map.Entry<String,Double> e : senseRatings.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                senses.clear();
                senses.add(e.getKey());
            }
            else if (e.getValue().equals(max))
                senses.add(e.getKey());
        }
       
        if (senses.size() == 1) {
            senseRatings.keySet().retainAll(senses);
        }
        // Sort the tied senses so that tie-breaking is deterministic
        else {
            SortedSet<String> sorted = new TreeSet<String>(senses);
            senseRatings.keySet().retainAll(
                Collections.<String>singleton(sorted.iterator().next()));
        }
    }
}
