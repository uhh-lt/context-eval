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
 * account their respective sense weightings.
 */
public class GradedReweightedKeyMapper implements KeyMapper {

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
    public Map<String,Map<String,Map<String,Double>>>
        convert(Map<String,Map<String,Map<String,Double>>> goldKey,
                Map<String,Map<String,Map<String,Double>>> testKey,
                Set<String> trainingInstanceIds) {

        Map<String,Map<String,Map<String,Double>>> outputKey 
            = new LinkedHashMap<String,Map<String,Map<String,Double>>>();

        for (String term : goldKey.keySet()) {
            Map<String,Map<String,Double>> goldInstances = goldKey.get(term);
            Map<String,Map<String,Double>> testInstances = testKey.get(term);
            Map<String,Map<String,Double>> remappedInstances
                = remap(goldInstances, testInstances, trainingInstanceIds);

            outputKey.put(term, remappedInstances);
        }
        return outputKey;
    }

    /**
     * Performs the remapping process for a single term's instances
     */
    private static Map<String,Map<String,Double>> 
            remap(Map<String,Map<String,Double>> goldInstances,
                  Map<String,Map<String,Double>> testInstances,
                  Set<String> trainingInstanceIds) {
        
        Map<String,Map<String,Double>> remapped = 
            new LinkedHashMap<String,Map<String,Double>>();

        // If there were no instances
        if (testInstances == null)
            return remapped;

        // Create a mapping from each of the sense labels to an index in the
        // mapping matrix
        Map<String,Integer> goldSenseIds = new LinkedHashMap<String,Integer>();
        Map<String,Integer> testSenseIds = new LinkedHashMap<String,Integer>();

        for (String instanceId : trainingInstanceIds) {
            Map<String,Double> gsPerceptions = goldInstances.get(instanceId);
            Map<String,Double> tsPerceptions = testInstances.get(instanceId);
            // Check that we had a sense labeling in both keys for this
            // particular instance
            if (gsPerceptions == null || tsPerceptions == null)
                continue;
            for (String ts : tsPerceptions.keySet()) {
                if (!testSenseIds.containsKey(ts))
                    testSenseIds.put(ts, testSenseIds.size());
            }

            for (String gs : gsPerceptions.keySet()) {
                if (!goldSenseIds.containsKey(gs))
                    goldSenseIds.put(gs, goldSenseIds.size());
            }
        }

        // If there were no instances in common
        if (testSenseIds.size() == 0 || goldSenseIds.size() == 0)
            return remapped;

        double[][] mappingMatrix = 
            new double[testSenseIds.size()][goldSenseIds.size()];

        for (String instanceId : trainingInstanceIds) {
            Map<String,Double> gsPerceptions = goldInstances.get(instanceId);
            Map<String,Double> tsPerceptions = testInstances.get(instanceId);
            // Check that we had a sense labeling in both keys for this
            // particular instance
            if (gsPerceptions == null || tsPerceptions == null)
                continue;
            for (Map.Entry<String,Double> test : tsPerceptions.entrySet()) {
                String ts = test.getKey();
                double tsRating = test.getValue();
                int tsIndex = testSenseIds.get(ts);
                for (Map.Entry<String,Double> gold : gsPerceptions.entrySet()) {
                    String gs = gold.getKey();
                    double gsRating = gold.getValue();
                    int gsIndex = goldSenseIds.get(gs);
                    double score = tsRating * gsRating;

                    mappingMatrix[tsIndex][gsIndex] += score;
                }
            }
        }

        // Normalize the rows of the matrix
        for (int r = 0; r < mappingMatrix.length; ++r) {
            double sum = 0;
            for (int c = 0; c < mappingMatrix[0].length; ++c)
                sum += mappingMatrix[r][c];
            for (int c = 0; c < mappingMatrix[0].length; ++c)
                mappingMatrix[r][c] /= sum;
        }

        // Once the mapping matrix is built, identify the test instances that we
        // should remap
        Set<String> testInstanceIds 
            = new HashSet<String>(testInstances.keySet());
        testInstanceIds.removeAll(trainingInstanceIds);

        // Iterate over each 
        for (String testInstanceId : testInstanceIds) {
            
            // This instance's perceptions need to be converted to a row bector
            // for all these senses
            Map<String,Double> tsPerceptions = 
                testInstances.get(testInstanceId);
            
            // The result of multiplying the test vector by the
            // induced-reference sense matrix
            double[] testVector = new double[mappingMatrix.length];
            
            for (Map.Entry<String,Integer> e : testSenseIds.entrySet()) {
                int col = e.getValue();
                Double testSensePerception = tsPerceptions.get(e.getKey());
                // testSensePerception might be null if the test rating is for
                // an induced sense that was not seen during the mapping stage
                if (testSensePerception != null) {
                    testVector[col] = testSensePerception;
                }
            }
            
            // Multiply the vector by the sense matrix
            int rows = testVector.length;
            int cols = mappingMatrix[0].length;
            double[] result = new double[cols];
            for (int c = 0; c < cols; ++c) {
                double resultValue = 0;
                for (int i = 0; i < rows; ++i)
                    resultValue += testVector[i] * mappingMatrix[i][c];
                result[c] = resultValue;
            }

            // Create a mapping from each gold sense to the test senses
            Map<String,Double> remappedPerceptions =
                new HashMap<String,Double>();

            for (int i = 0; i < result.length; ++i) {
                double score = result[i];
                if (score > 0) {
                    String gs = null;
                    for (Map.Entry<String,Integer> e : goldSenseIds.entrySet()) {
                        if (e.getValue().equals(i)) {
                            gs = e.getKey();
                            break;
                        }
                    }
                    if (gs == null)
                        throw new IllegalStateException(
                            "Unmapped index " + i + " in " + goldSenseIds);

                    remappedPerceptions.put(gs, score);
                }
            }

            // If we were able to map the test key labeling to at least one
            // sense in the gold standard sense inventory, then report the
            // labeling.  Otherwise, just omit an empty labeling.
            if (!remappedPerceptions.isEmpty())
                remapped.put(testInstanceId, remappedPerceptions);
        }

        return remapped;
    }
}
