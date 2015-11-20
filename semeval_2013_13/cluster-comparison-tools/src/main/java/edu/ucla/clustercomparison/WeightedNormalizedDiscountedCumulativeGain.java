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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * An implementation of <a
 * href="http://en.wikipedia.org/wiki/Normalized_discounted_cumulative_gain#Normalized_DCG">Normalized
 * Discounted Cumulative Gain</a> that weights each of the ranks according the
 * the differences in values.
 */
public class WeightedNormalizedDiscountedCumulativeGain extends AbstractEvaluation {

    public WeightedNormalizedDiscountedCumulativeGain() { }
   
    /**
     * Computes the average deviation in agreement between the gold and test
     * ratings for all reported senses, returning {@code 1} if all sense ratings
     * are in perfect agreement and 0 if none agree.
     */
    @Override protected double evaluateInstance(
            Map<String,Double> goldSenseRatings,
            Map<String,Double> testSenseRatings,
            int numSenses) {
    
        Set<String> allSenses = new HashSet<String>(goldSenseRatings.keySet());
        allSenses.addAll(testSenseRatings.keySet());

        // The sense at [i] has rank i 
        String[] testRanking = rank(testSenseRatings, allSenses);

        // Compute the discounted cumulative gain according to the proposed
        // ranking
        double dcg = 0;
        for (int i = 0; i < testRanking.length; ++i) {
            String sense = testRanking[i];
            double gScore = (goldSenseRatings.containsKey(sense))
                ? goldSenseRatings.get(sense) : 0;
            double tScore = (testSenseRatings.containsKey(sense))
                ? testSenseRatings.get(sense) : 0;
            
            // In the normal computation of DCG, the score is 2^rel - 1,
            // however, we weight this by the relative difference in the gold
            // and test scores to penalize test ratings that produce the same
            // ranking as the gold ranking, but with radically different weights
            double score = (gScore == 0d && tScore == 0d)
                ? 1 // equivalent to (Math.pow(2, 1 + gScore) - 1)
                : ((Math.min(gScore, tScore) / Math.max(gScore, tScore))
                   * (Math.pow(2, 1 + gScore) - 1));

            dcg += score / Log.log2_1p(i+1);
        }

        double idcg = 0;
        // Get a monotonically increasing ordering of the gold sense scores.
        List<Double> tmp = new ArrayList<Double>(goldSenseRatings.values());
        Collections.sort(tmp);
        // Iterate backwards over the scores (i.e., in decreasing order),
        // summing the idcg values
        for (int i = tmp.size() - 1, rank = 0; i >= 0; i--, rank++) {
            // No need to weight the gold score since its ratio is one by
            // definition 
            double score = tmp.get(i);
            idcg += Math.pow(2, 1 + score) / Log.log2_1p(rank+1);
        }
        
        return dcg / idcg;
    }

    /**
     * Produces a ranking for all the senses in {@code allSenses} according to
     * their scores in {@code senseRatings}.  Senses without scores are
     * assumed to score 0.
     */
    private static String[] rank(Map<String,Double> senseRatings, 
                                 Set<String> allSenses) {
        // Use a sorted set in the values to produce a consistent ordering of
        // the senses in the event of a tie.
        NavigableMap<Double,SortedSet<String>> scoreToSenses =
            new TreeMap<Double,SortedSet<String>>();

        for (String sense : allSenses) {            
            Double score = (senseRatings.containsKey(sense))
                ? senseRatings.get(sense) : 0;
            SortedSet<String> senses = scoreToSenses.get(score);
            if (senses == null) {
                senses = new TreeSet<String>();
                scoreToSenses.put(score, senses);
            }
            senses.add(sense);
        }

        // Swap the order so we get the higest-scoring senses in the first ranks
        NavigableMap<Double,SortedSet<String>> highestToLowest =
            scoreToSenses.descendingMap();

        String[] ranked = new String[allSenses.size()];
        int i = 0;
        for (SortedSet<String> senses : highestToLowest.values()) {
            for (String sense : senses)
                ranked[i++] = sense;
        }
        return ranked;
    }
}
