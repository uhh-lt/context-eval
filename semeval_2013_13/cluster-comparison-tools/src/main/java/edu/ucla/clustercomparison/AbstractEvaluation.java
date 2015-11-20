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
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;


/**
 * An abstract base class for evaluation methods that handles the common
 * behavior of looping over both keys and calling {@link
 * #evaluateInstance(Map,Map,int)} for each instance in common.
 */
public abstract class AbstractEvaluation implements Evaluation {

    /**
     * The logger for this class
     */
    private final Logger LOGGER =
        Logger.getLogger(AbstractEvaluation.class.getName());
    
    /**
     * Performs the evaluation on the given sense ratings, where each {@link
     * Map} is a mapping from a sense to its rating for the particular instance.
     *
     * @return the evaluation score 
     */
    protected abstract double evaluateInstance(
        Map<String,Double> goldSenseRatings,
        Map<String,Double> testSenseRatings,
        int numSenses);

    /**
     * Returns a mapping from each instance to its score
     */
    public Map<String,Double>
        test(Map<String,Map<String,Map<String,Double>>> test,
             Map<String,Map<String,Map<String,Double>>> gold,
             Set<String> testInstances,
             Map<String,Integer> termToNumSenses) {
        
        Map<String,Double> instanceToScore = new HashMap<String,Double>();

        // We pair against each of the terms and corresponding instance in the
        // gold standard
        for (Map.Entry<String,Map<String,Map<String,Double>>> e 
                 : gold.entrySet()) {
            
            String term = e.getKey();
            veryVerbose(LOGGER, "testing %s", term);
            Integer numSenses = termToNumSenses.get(term);
            if (numSenses == null) {
                throw new IllegalStateException("Missing number of senses for " + term);
            }
            Map<String,Map<String,Double>> instanceToGoldSenses = e.getValue();
            Map<String,Map<String,Double>> instanceToTestSenses = 
                test.get(term);

            // In the event that the user did not specify any instances for a
            // term in the gold key, just skip this term
            if (instanceToTestSenses == null) 
                continue;

            // For each instance, compute the Jaccard Index between the gold
            // standard sense listing and what is present in the test key
            for (Map.Entry<String,Map<String,Double>> e2 
                     : instanceToGoldSenses.entrySet()) {

                String instance = e2.getKey();

                // Check that this instance is one that we should be scoring and
                // if not, move on
                if (!testInstances.contains(instance))
                    continue;

                Map<String,Double> goldSenses = e2.getValue();
                Map<String,Double> testSenses = 
                    instanceToTestSenses.get(instance);
                
                // If the test key did not provide a sense rating for this
                // instace, skip it
                if (testSenses == null)
                    continue;
                
                double score = evaluateInstance(goldSenses, testSenses,
                                                numSenses);
                                
                if (Double.isNaN(score) || Double.isInfinite(score)) {
                    throw new IllegalStateException(
                        getClass().getName() + " returned an evaluation score "+
                        " that is outside the acceptable bounds: " + score);
                }
                instanceToScore.put(instance, score);                
            }
        }
        return instanceToScore;
    }
}
