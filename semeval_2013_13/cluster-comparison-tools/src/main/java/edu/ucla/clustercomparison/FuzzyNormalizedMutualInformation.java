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
 * The command line program for computing the Fuzzy Normalized Mutual
 * Information between two sense clusterings.  This class is the fuzzy extension
 * of the method of <a href="http://arxiv.org/pdf/0802.1218.pdf">Lancichetti et
 * al. (2009)</a> for computing Normalized Mutual Information between
 * overlapping clusters.
 */
public class FuzzyNormalizedMutualInformation {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("java fuzzy-nmi.jar gold.key to-evaluate.key");
            return;
        }
        score(new File(args[0]), new File(args[1]));
    }
        
    public static double score(File goldKeyFile, 
                               File testKeyFile) throws Exception {

        // Load the keys, which are returned as mapping from terms to all of
        // their instances' graded sense labelings.
        Map<String,Map<String,Map<String,Double>>> goldKey = 
            KeyUtil.loadKey(goldKeyFile);
        Map<String,Map<String,Map<String,Double>>> testKey = 
            KeyUtil.loadKey(testKeyFile);

        Map<String,Double> termToNmi = new HashMap<String,Double>();


        for (Map.Entry<String,Map<String,Map<String,Double>>> e4 : goldKey.entrySet()) {
            String term = e4.getKey();
            Map<String,Map<String,Double>> instanceToGoldRatings = e4.getValue();
            Map<String,Map<String,Double>> instanceToTestRatings = testKey.get(term);

            // Check that there are ratings for this word, and if not, note that
            // it had zero for both scores and continue to the next work
            if (instanceToTestRatings == null) {
                System.out.printf("%s had no instances labeled in the evaluation key%n",
                                  term);
                termToNmi.put(term, 0d);
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

            double nmi = computeNmi(instanceToGoldRatings, instanceToTestRatings);

            termToNmi.put(term, nmi);
        }

        // Generate the report
        System.out.println("===================================================================");
        System.out.println("term\tFuzzy Normalized Mutual Information");
        System.out.println("-------------------------------------------------------------------");
        double nmiSum = 0;
        for (Map.Entry<String,Double> e : termToNmi.entrySet()) {
            String term = e.getKey();
            double nmi = e.getValue();
            nmiSum += nmi;
            System.out.println(term + "\t" + nmi);
        }
        System.out.println("-------------------------------------------------------------------");
        // Print out the aggregate
        double avgNmi = nmiSum / termToNmi.size();
        System.out.println("all\t" + avgNmi);
        System.out.println("===================================================================");
        return avgNmi;

    }

    /**
     * Computes the Fuzzy Normalized Mutual Information between the two
     * clusterings.
     */
    public static double computeNmi(Map<String,Map<String,Double>> instanceToGoldRatings,
                                    Map<String,Map<String,Double>> instanceToTestRatings) {
            // Compute the set of instances that are mutually labeled with both
            // gold and test instances
            Set<String> allInstances = 
                new HashSet<String>(instanceToGoldRatings.keySet());
            allInstances.addAll(instanceToTestRatings.keySet());
            final int N = allInstances.size();

            // Cache the set of gold and test senses
            Set<String> goldSenses = new HashSet<String>();
            for (Map<String,Double> m : instanceToGoldRatings.values())
                for (String s : m.keySet())
                    goldSenses.add(s);
            Set<String> testSenses = new HashSet<String>();
            for (Map<String,Double> m : instanceToTestRatings.values())
                for (String s : m.keySet())
                    testSenses.add(s);
            
            // Creating a mapping from all senses to their distribution over all
            // the instances
            Map<String,double[]> testSensesToDistribution 
                = new HashMap<String,double[]>();
            Map<String,double[]> goldSensesToDistribution 
                = new HashMap<String,double[]>();

            for (String g : goldSenses)
                goldSensesToDistribution.put(g, new double[N]);
            for (String t : testSenses)
                testSensesToDistribution.put(t, new double[N]);

            // Create a maping for all instances to a unique set of indices
            Indexer<String> instanceToIndex = new Indexer<String>();
            for (String i : allInstances)
                instanceToIndex.index(i);

            // Creating a mapping from all senses to their ratings over all the
            // instances
            for (Map.Entry<String,Map<String,Double>> e 
                     : instanceToGoldRatings.entrySet()) {
                String instance = e.getKey();
                for (Map.Entry<String,Double> e2 : e.getValue().entrySet())
                    goldSensesToDistribution.
                        get(e2.getKey())[instanceToIndex.get(instance)] = e2.getValue();
            }
            for (Map.Entry<String,Map<String,Double>> e 
                     : instanceToTestRatings.entrySet()) {
                String instance = e.getKey();
                for (Map.Entry<String,Double> e2 : e.getValue().entrySet())
                    testSensesToDistribution.
                        get(e2.getKey())[instanceToIndex.get(instance)] = e2.getValue();
            }

            // Compute the entropy for each label set as a whole
            double h_G = 0;
            double h_T = 0;
            for (Map.Entry<String,double[]> e : testSensesToDistribution.entrySet()) {
                double de = DiscretizedDifferentialEntropy.compute(e.getValue());
                h_T += de;
            }
            for (Map.Entry<String,double[]> e : goldSensesToDistribution.entrySet()) {
                double de = DiscretizedDifferentialEntropy.compute(e.getValue());
                h_G += de;
            }

            // Compute the conditional entropy of the gold senses given the test
            // senses, i.e., H(G|T)
            double h_GT = 0;
            for (String gold : goldSenses) {
                double[] gInstanceRatings = 
                    goldSensesToDistribution.get(gold);
                double h_gT = Double.MAX_VALUE;
                for (String test : testSenses) {
                    double[] tInstanceRatings = 
                        testSensesToDistribution.get(test);
                    if (skip(gInstanceRatings, tInstanceRatings))
                        continue;
                    // Compute the relative entropy between the test and gold
                    // senses' rating distributions
                    double conditionalEntropy = DiscretizedDifferentialEntropy
                        .compute(gInstanceRatings, tInstanceRatings);
                    
                    // If this is the lowest entropy for the current gold
                    // sense then we set H(g_i | T) 
                    if (conditionalEntropy < h_gT)
                        h_gT = conditionalEntropy;
                }

                // If we didn't find another sense labeling that wasn't an
                // inverse of this label, use H(g)
                if (h_gT == Double.MAX_VALUE)
                    h_gT = DiscretizedDifferentialEntropy.compute(gInstanceRatings);
                    
                // Note the division here to normalize the total for H(G|T)
                h_GT += h_gT; 
            }

            // Compute the conditional entropy of the test senses given the gold
            // senses, i.e., H(G|T)
            double h_TG = 0;
            for (String test : testSenses) {
                double[] tInstanceRatings = 
                    testSensesToDistribution.get(test);
                double h_tG = Double.MAX_VALUE;
                for (String gold : goldSenses) {
                    double[] gInstanceRatings = 
                        goldSensesToDistribution.get(gold);
                    if (skip(tInstanceRatings, gInstanceRatings))
                        continue;

                    // Compute the relative entropy between the test and gold
                    // senses' rating distributions
                    double conditionalEntropy = DiscretizedDifferentialEntropy
                        .compute(tInstanceRatings, gInstanceRatings);
                    
                    // If this is the lowest entropy for the current gold
                    // sense then we set H(g_i | G) 
                    if (conditionalEntropy < h_tG)
                        h_tG = conditionalEntropy;
                }

                // If we didn't find another sense labeling that wasn't an
                // inverse of this label, use H(t)
                if (h_tG == Double.MAX_VALUE) {
                    h_tG = DiscretizedDifferentialEntropy
                        .compute(tInstanceRatings);
                }


                // Note the division here to normalize the total for H(G|T)
                h_TG += h_tG; // testSenses.size();
            }
                     
            
            double mi = .5 * (h_G - h_GT + h_T - h_TG);
            double nmi = mi / Math.max(h_G, h_T);
            
            return nmi;
    }
    
    /**
     * Returns the entropy of the value
     */ 
    private static double h(double d) {
        return (d == 0) ? 0 : -d * Math.log(d);
    }
    
    /**
     * Returns true if the information of the two clusters' item memberships
     * (denoted as X and Y) is higher because one is the complement of the
     * other.  See Eq. B.14 in Lancichetti et al. for details.
     */
    private static boolean skip(double[] X, double[] Y) {

        Counter<String> c = new Counter<String>();

        for (int i = 0; i < X.length; ++i) {
            double x = X[i];
            double y = Y[i];
            String state = null;
            if (x > 0 && y > 0) 
                state = "+X+Y";
            else if (x > 0 && y == 0) 
                state = "+X-Y";
            else if (x == 0 && y > 0) 
                state = "-X+Y";
            else if (x == 0 && y == 0) 
                state = "-X-Y";
            else
                throw new AssertionError();
            c.count(state);
        }

        // NOTE: This is the inverse predicate of what is in Lancichetti et
        // al. since we're testing whether to skip this comparison
        return h(c.getProbability("+X+Y")) + h(c.getProbability("-X-Y")) 
            < h(c.getProbability("+X-Y")) + h(c.getProbability("-X+Y"));
    }
}
