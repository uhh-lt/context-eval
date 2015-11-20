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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The implementation of the <a
 * href="http://en.wikipedia.org/wiki/Goodman_and_Kruskal's_gamma">Goodman and
 * Kruskal's gamma</a> for comparing the rankings of two sense labelings
 */
public class GoodmanKruskalGamma extends AbstractEvaluation {

    /**
     * Computes Goodman and Kruskal's gamma between the two sense rankings.
     */
    public double evaluateInstance(final Map<String,Double> goldSensePerceptions,
                                   final Map<String,Double> testSensePerceptions,
                                   int numSenses) {

        // Sort the gold standard senses so the most perceptible sense is first
        // in the list
        List<String> goldSenseOrder = 
            new ArrayList<String>(goldSensePerceptions.keySet());
        Collections.sort(goldSenseOrder, new Comparator<String>() {
                public int compare(String sense1, String sense2) {
                    return -Double.compare(goldSensePerceptions.get(sense1),
                                           goldSensePerceptions.get(sense2));
                }
            });

        // To account for ties, reorder the List as a List<Set<String>> so that
        // sense with tied positions have the same rank (index).  We need this
        // list so that we can later add in all the senses from the test ranking
        // that were not in the gold ranking.
        List<Set<String>> goldRanking = new ArrayList<Set<String>>();
        Map<String,Integer> senseToGoldRanking = new HashMap<String,Integer>();
        for (int i = 0, rank = 0; i < goldSenseOrder.size(); ++i) {
            String sense = goldSenseOrder.get(i);
            // If this is the first sense, the we'll have to create a new set
            // for it
            if (i == 0) {
                Set<String> senses = new HashSet<String>();
                senses.add(sense);
                goldRanking.add(senses);
            }
            // Otherwise, check if the current sense has the same perceptibility
            // as the previous senses.  If so, then it has the same rank as the
            // other senses
            else {
                double d1 = goldSensePerceptions.get(sense);
                double d2 = goldSensePerceptions.get(goldSenseOrder.get(i-1));
                if (d1 == d2) {
                    goldRanking.get(rank).add(sense);
                }            
                // Last, if a later sense does not have the same perceptibility,
                // then it takes on a lower rank and gets put in its own set.
                else {
                    rank++;
                    Set<String> senses = new HashSet<String>();
                    senses.add(sense);
                    goldRanking.add(senses);
                }
            }
            senseToGoldRanking.put(sense, rank);
        }

        // Create a list of the test senses.  Note that this list may contain
        // more senses than are present in the gold standard list.
        List<String> testSenseOrder = 
            new ArrayList<String>(testSensePerceptions.keySet());
        // Sort the senses so the most perceptible sense is first in the list
        Collections.sort(testSenseOrder, new Comparator<String>() {
                public int compare(String sense1, String sense2) {
                    return -Double.compare(testSensePerceptions.get(sense1),
                                           testSensePerceptions.get(sense2));
                }
            });

        // To account for ties, reorder the List of test sense perceptiblities
        // as a List<Set<String>> so that sense with tied positions have the
        // same rank (index).  We need this list so that we can later add in all
        // the senses from the gold ranking that were not in the test ranking.
        List<Set<String>> testRanking = new ArrayList<Set<String>>();
        Map<String,Integer> senseToTestRanking = new HashMap<String,Integer>();
        for (int i = 0, rank = 0; i < testSenseOrder.size(); ++i) {
            String sense = testSenseOrder.get(i);

            // If this is the first sense, the we'll have to create a new set
            // for it
            if (i == 0) {
                Set<String> senses = new HashSet<String>();
                senses.add(sense);
                testRanking.add(senses);
            }
            // Otherwise, check if the current sense has the same perceptibility
            // as the previous senses.  If so, then it has the same rank as the
            // other senses
            else {
                double d1 = testSensePerceptions.get(sense);
                double d2 = testSensePerceptions.get(testSenseOrder.get(i-1));
                if (d1 == d2) {
                    testRanking.get(rank).add(sense);
                }
                // Last, if a later sense does not have the same perceptibility,
                // then it takes on a lower rank and gets put in its own set.
                else {
                    rank++;
                    Set<String> senses = new HashSet<String>();
                    senses.add(sense);
                    testRanking.add(senses);
                }
            }
            senseToTestRanking.put(sense, rank);
        }
        
        // Once we have the rankings for both the gold and test senses, we need
        // to take into account the senses that were not present in each.  These
        // senses get tacked on to the end of the rankings with one rank lower
        // than the most perceptible sense.  This rank-addition can be thought
        // of making explicit a lower rank that denotes senses who were not
        // perceptible in the first place (i.e., senses whose perceptiblity is
        // 0).
        Set<String> sensesNotInGold = new HashSet<String>();
        for (String sense : testSensePerceptions.keySet()) {
            if (!goldSensePerceptions.containsKey(sense))
                sensesNotInGold.add(sense);
        }
        // If the test ranking contained senses not in the gold standard, add
        // them as the last ranked
        if (!sensesNotInGold.isEmpty()) {
            int lastRankInGold = goldRanking.size();
            goldRanking.add(sensesNotInGold);
            for (String sense : sensesNotInGold)
                senseToGoldRanking.put(sense, lastRankInGold);
        }

        Set<String> sensesNotInTest = new HashSet<String>();
        for (String sense : goldSensePerceptions.keySet()) {
            if (!testSensePerceptions.containsKey(sense))
                sensesNotInTest.add(sense);
        }
        // If the gold ranking contained senses not in the test standard, add
        // them as the last ranked
        if (!sensesNotInTest.isEmpty()) {
            int lastRankInTest = testRanking.size();
            testRanking.add(sensesNotInTest);
            for (String sense : sensesNotInTest)
                senseToTestRanking.put(sense, lastRankInTest);
        }


        Set<String> allSenses = 
            new HashSet<String>(goldSensePerceptions.keySet());
        allSenses.addAll(testSensePerceptions.keySet());

        int concordant = 0;
        int discordant = 0;
        boolean foundTies = false;

        // For each pair of senses (s1, s2), count how many times these relative
        // rankings of (s1, s2) agree or disagree in the gold and test rankings.
        // Not that this discounts cases were s1 and s2 are tied in one of the
        // rankings, in which case the ranking code at the end takes into
        // account the ties.
        for (String sense1 : allSenses) {
            for (String sense2 : allSenses) {
                if (sense1.equals(sense2))
                    break;
                
                // Get the ranks of each of the two senses in both the gold and
                // test sense lists
                int s1goldRank = senseToGoldRanking.get(sense1);
                int s2goldRank = senseToGoldRanking.get(sense2);
                int s1testRank = senseToTestRanking.get(sense1);
                int s2testRank = senseToTestRanking.get(sense2);                

                // Since we're calculating tau-b, check for ties in the ranks in
                // either the gold or test rankings.  Skip the comparison if
                // there were ties
                if (s1goldRank != s2goldRank && s1testRank != s2testRank) {
                    if ((s1goldRank < s2goldRank && s1testRank < s2testRank)
                        || (s1goldRank > s2goldRank && s1testRank > s2testRank))
                        concordant++;
                    else
                        discordant++;
                }
                // If we found ties, record it so we can calculate tau-b instead
                // of tau-a
                else
                    foundTies = true;
            }
        }


        int n = concordant - discordant;
        int length = allSenses.size();
        double d = (.5 * (length * (length-1)));

        if (foundTies) {
            double n1 = 0;
            for (int i = 0; i < goldRanking.size(); ++i) {
                Set<String> senses = goldRanking.get(i);
                int ties = senses.size();
                if (ties > 1) 
                    n1 += (ties * (ties-1)) * .5;                
            }

            double n2 = 0;
            for (int i = 0; i < testRanking.size(); ++i) {
                Set<String> senses = testRanking.get(i);
                int ties = senses.size();
                if (ties > 1) 
                    n2 += (ties * (ties-1)) * .5;                
            }
            
            // System.out.printf("%d / sqrt((%f - %f) * (%f  - %f))%n", n, d, n1, d, n2);

            // Check for the edge case where one of the order ranked everything
            // equally
            if (n1 == d || n2 == d)
                return  .5; // when normalized
            double gamma = n / Math.sqrt((d - n1) * (d - n2));
            // normalize to [0,1]
            return  (gamma + 1) / 2;
        }
        else {
            // System.out.printf("%d / %f%n", n, d);
            // NOTE: d == 0 only in the case where both solutions have perceived
            // only one sense (which are both the same), in which case the
            // ranking doesn't change
            if (d == 0) 
                return 1;
            double gamma = n / d;
            return (gamma + 1) / 2;
        }
    }
}
