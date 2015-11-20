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

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A command-line class for computing Fuzzy B-Cubed between two senses labelings
 */
public class FuzzyBCubed {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("java fuzzy-bcubed.jar gold.key to-evaluate.key");
            return;
        }
        score(new File(args[0]), new File(args[1]));
    }
        
    public static double[] score(File goldKeyFile, 
                                 File testKeyFile) throws Exception {
        
        // Load both keys
        Map<String,Map<String,Map<String,Double>>> goldKey = 
            KeyUtil.loadKey(goldKeyFile);
        Map<String,Map<String,Map<String,Double>>> testKey = 
            KeyUtil.loadKey(testKeyFile);

        Map<String,Double> termToAvgPrecision = new HashMap<String,Double>();
        Map<String,Double> termToAvgRecall = new HashMap<String,Double>();

        for (Map.Entry<String,Map<String,Map<String,Double>>> e : goldKey.entrySet()) {
            String term = e.getKey();
            Map<String,Map<String,Double>> instanceToGoldRatings = e.getValue();
            Map<String,Map<String,Double>> instanceToTestRatings = testKey.get(term);

            // Check that there are ratings for this word, and if not, note that
            // it had zero for both scores and continue to the next work
            if (instanceToTestRatings == null) {
                System.out.printf("%s had no instances labeled in the evaluation key%n",
                                  term);
                termToAvgPrecision.put(term, 0d);
                termToAvgRecall.put(term, 0d);
                continue;
            }

            // NOTE: sometimes the gold key will contain fewer instances than
            // the test key, e.g., when the provided gold key is only a subset
            // of the instances and the user is providing their same systems
            // key.  In such cases, the correct behavior is to filter down the
            // test key to only contain those instances in the gold key.  Thanks
            // to Jing Wang for noticing this bug.
            instanceToTestRatings.keySet()
                .retainAll(instanceToGoldRatings.keySet());

            double[] result = computeBCubed(instanceToGoldRatings, 
                                            instanceToTestRatings);

            double avgPrecision = result[0];
            double avgRecall = result[1];

            termToAvgPrecision.put(term, avgPrecision);
            termToAvgRecall.put(term, avgRecall);            
        }

        double pSum = 0;
        double rSum = 0;

        // Generate the report
        System.out.println("===================================================================");
        System.out.println("term\tprecision\trecall\tf-score");
        System.out.println("-------------------------------------------------------------------");
        for (Map.Entry<String,Double> e : termToAvgPrecision.entrySet()) {
            String term = e.getKey();
            double avgPrecision = e.getValue();
            double avgRecall = termToAvgRecall.get(term);
            double fScore = 
                (avgPrecision + avgRecall > 0) 
                ? (2 * avgPrecision * avgRecall) / (avgPrecision + avgRecall)
                : 0;

            System.out.println(term + "\t" + avgPrecision + "\t" + avgRecall + "\t" + fScore);
            pSum += avgPrecision;
            rSum += avgRecall;
        }
        System.out.println("-------------------------------------------------------------------");
        // Print out the aggregate
        double precision = pSum / termToAvgPrecision.size();
        double recall = rSum / termToAvgPrecision.size();
        double fscore = (precision + recall > 0) 
            ? (2 * precision * recall) / (precision + recall)
            : 0;
            
        System.out.println("all\t" + precision + "\t" + recall + "\t" + fscore);        
        System.out.println("===================================================================");
        
        return new double[] { precision, recall, fscore };
    }

    /**
     * Computes the fuzzy agreement between two items, each represented as
     * mappings from their assigned clusters to those clusters' membership
     * weights.
     */
    static double fuzzyAgreement(Map<String,Double> c1weights, 
                                 Map<String,Double> c2weights) {
        Set<String> clusters = new HashSet<String>(c1weights.keySet());
        clusters.retainAll(c2weights.keySet());
        
        if (clusters.isEmpty())
            return 0;
        
        double agreement = 0;
        for (String c : clusters) {
            Double w1 = c1weights.get(c);
            if (w1 == null)
                w1 = 0d;
            Double w2 = c2weights.get(c);
            if (w2 == null)
                w2 = 0d;
            agreement += 1 - Math.abs(w1 - w2);
        }
        return agreement;
    }

    /**
     * Computes Fuzzy B-Cubed between two labeings for the instances of a term,
     * returning an array containg the fuzzy precision and recall values.
     *
     * @param instanceToGoldRatings a mapping from the instance identifier to
     *        the sense ratings for that instance, represented as a map from
     *        sense id to weight
     */
    public static double[] computeBCubed(Map<String,Map<String,Double>> instanceToGoldRatings,
                                         Map<String,Map<String,Double>> instanceToTestRatings) {

        double precisionSum = 0;        
        double recallSum = 0;        
        
        // For each pair-wise comparison of elements, compute the precision
        for (Map.Entry<String,Map<String,Double>> e
                 : instanceToGoldRatings.entrySet()) {
            String instance1Id = e.getKey();
            Map<String,Double> i1goldRatings = e.getValue();
            Map<String,Double> i1testRatings = instanceToTestRatings.get(instance1Id);
            if (i1testRatings == null)
                i1testRatings = Collections.<String,Double>emptyMap();

            Set<String> i1clusters = i1goldRatings.keySet();
           
            int i1precisionCount = 0;
            double i1precisionSum = 0;

            // Find all the instances in the gold key that have any of its clusters
            for (Map.Entry<String,Map<String,Double>> e2
                 : instanceToGoldRatings.entrySet()) {

                if (instance1Id.equals(e2.getKey()))
                    continue;

                Set<String> i2clusters = e2.getValue().keySet();

                // If they share at least one common label in the gold standard
                if (!Collections.disjoint(i1clusters, i2clusters)) {
                    // Get the second instance's ratings
                    Map<String,Double> i2goldRatings = e2.getValue();
                    Map<String,Double> i2testRatings = instanceToTestRatings.get(e2.getKey());
                    if (i2testRatings == null)
                        i2testRatings = Collections.<String,Double>emptyMap();
                    
                    double goldFuzzyAgreement = 
                        fuzzyAgreement(i1goldRatings, i2goldRatings);
                    double testFuzzyAgreement = 
                        fuzzyAgreement(i1testRatings, i2testRatings);
                    
                    if (goldFuzzyAgreement > 0)
                        i1precisionSum += Math.min(goldFuzzyAgreement, testFuzzyAgreement)
                            / goldFuzzyAgreement;
                    i1precisionCount++;
                }
            }

            // Compute the average precision for this item.  Check for if it was
            // a singleton cluster
            double i1precision = (i1precisionCount == 0)
                ? 0 : i1precisionSum  / i1precisionCount;

            // Add this to the total precision for the solution
            precisionSum += i1precision;
        }
        
        // For each pair-wise comparison of elements, compute the recall
        for (Map.Entry<String,Map<String,Double>> e
                 : instanceToTestRatings.entrySet()) {
            String instance1Id = e.getKey();
            Map<String,Double> i1goldRatings = instanceToGoldRatings.get(instance1Id);
            Map<String,Double> i1testRatings = e.getValue();
            if (i1goldRatings == null)
                i1goldRatings = Collections.<String,Double>emptyMap();

            Set<String> i1clusters = i1testRatings.keySet();
            if (i1clusters.isEmpty())
                continue;
            
            int i1recallCount = 0;
            double i1recallSum = 0;

            // Find all the instances in the gold key that have any of its clusters
            for (Map.Entry<String,Map<String,Double>> e2
                 : instanceToTestRatings.entrySet()) {

                if (instance1Id.equals(e2.getKey()))
                    continue;

                Set<String> i2clusters = e2.getValue().keySet();

                // If they share at least one common label in the test labeling
                if (!Collections.disjoint(i1clusters, i2clusters)) {
                    // Get the second instance's ratings
                    Map<String,Double> i2goldRatings = instanceToGoldRatings.get(e2.getKey());
                    Map<String,Double> i2testRatings = e2.getValue();
                    if (i2goldRatings == null)
                        i2goldRatings = Collections.<String,Double>emptyMap();
                    
                    double goldFuzzyAgreement = 
                        fuzzyAgreement(i1goldRatings, i2goldRatings);
                    double testFuzzyAgreement = 
                        fuzzyAgreement(i1testRatings, i2testRatings);
                    
                    if (testFuzzyAgreement > 0)
                        i1recallSum += Math.min(goldFuzzyAgreement, testFuzzyAgreement)
                            / testFuzzyAgreement;
 
                    i1recallCount++;
                }
            }

            // Compute the average recall for this item.  Check for if it was a
            // singleton cluster
            double i1recall = (i1recallCount == 0) 
                ? 0 : i1recallSum  / i1recallCount;

            // Add this to the total recall for the solution
            recallSum += i1recall;
        }

        // Average the scores across all times
        double precision = precisionSum / instanceToGoldRatings.size();
        double recall = recallSum / instanceToGoldRatings.size();

        return new double[] { precision, recall };
    }
}
