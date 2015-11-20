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
 * An implementation of the Fuzzy Rand Index according to <a
 * href="http://hal.upmc.fr/docs/00/73/43/89/PDF/tfuzz-hullermeier-2179303-proof.pdf">Hullermeier
 * and Rifqi (2009)</a> for comparing to fuzzy sense clusterings, possibly in
 * different sense inventories.
 */
public class FuzzyRandIndex {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("java rand-index.jar gold.key to-evaluate.key");
            return;
        }
        
        // Load the keys, which are returned as mapping from terms to all of
        // their instances' graded sense labelings.
        Map<String,Map<String,Map<String,Double>>> goldKey = 
            KeyUtil.loadKey(args[0]);
        Map<String,Map<String,Map<String,Double>>> testKey = 
            KeyUtil.loadKey(args[1]);

        Map<String,Double> termToFri = new HashMap<String,Double>();


        for (Map.Entry<String,Map<String,Map<String,Double>>> e : goldKey.entrySet()) {
            String term = e.getKey();
            Map<String,Map<String,Double>> instanceToGoldRatings = e.getValue();
            Map<String,Map<String,Double>> instanceToTestRatings = testKey.get(term);

            // Check that there are ratings for this word, and if not, note that
            // it had zero for both scores and continue to the next work
            if (instanceToTestRatings == null) {
                System.out.printf("%s had no instances labeled in the evaluation key%n",
                                  term);
                termToFri.put(term, 0d);
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

            double fuzzyRandIndex = computeRI(instanceToGoldRatings,
                                              instanceToTestRatings);

            termToFri.put(term, fuzzyRandIndex);
        }

        // Generate the report
        System.out.println("===================================================================");
        System.out.println("term\tFuzzy Rand Index");
        System.out.println("-------------------------------------------------------------------");
        double friSum = 0;
        for (Map.Entry<String,Double> e : termToFri.entrySet()) {
            String term = e.getKey();
            double fri = e.getValue();
            friSum += fri;
            System.out.println(term + "\t" + fri);
        }
        System.out.println("-------------------------------------------------------------------");
        // Print out the aggregate
        double avgFri = friSum / termToFri.size();
        System.out.println("all\t" + avgFri);
        System.out.println("===================================================================");

    }

    /**
     * Computes the fuzzy Rand Index of the two clusterings
     */
    public static double computeRI(Map<String,Map<String,Double>> instanceToGoldRatings,
                                   Map<String,Map<String,Double>> instanceToTestRatings) {
            // Compute the set of instances that are mutually labeled with both
            // gold and test instances
            Set<String> instancesInCommon = 
                new HashSet<String>(instanceToGoldRatings.keySet());
            instancesInCommon.retainAll(instanceToTestRatings.keySet());

            // Get a record of how many senses we see for each labeling in the
            // mutually labeled set of instances.
            Set<String> gSenses = new HashSet<String>();
            Set<String> tSenses = new HashSet<String>();
            for (String i : instancesInCommon) {
                gSenses.addAll(instanceToGoldRatings.get(i).keySet());
                tSenses.addAll(instanceToTestRatings.get(i).keySet());
            }            

            // Record how many senses were in each label set
            final int numGoldSenses = gSenses.size();
            final int numTestSenses = tSenses.size();

            // Compute the maximum distances between two labeling in each label
            // set.  This is compute on the fly since the number of senses may
            // change between terms.
            final double gMaxDist = 
                distance(fill(gSenses, 1), fill(gSenses, 0), numGoldSenses);
            final double tMaxDist = 
                distance(fill(tSenses, 1), fill(tSenses, 0), numTestSenses);

            // This is a running sum of the level of discordance between the
            // pairs in the two label sets
            double fuzzyDiscordanceSum = 0;

            // Iterate over all pairs of instances, recording the degree of
            // concordance between the labelings
            for (String instance1Id : instancesInCommon) {
                
                Map<String,Double> gRating1 = instanceToGoldRatings.get(instance1Id);
                Map<String,Double> tRating1 = instanceToTestRatings.get(instance1Id);

                for (String instance2Id : instancesInCommon) {
                    // Skip evaluating duplicate pairs
                    if (instance1Id.equals(instance2Id))
                        break;

                    Map<String,Double> gRating2 = 
                        instanceToGoldRatings.get(instance2Id);
                    Map<String,Double> tRating2 = 
                        instanceToTestRatings.get(instance2Id);
                    
                    // Normalize the values to [0,1]
                    double gDistance = 
                        distance(gRating1, gRating2, numGoldSenses) / gMaxDist;
                    double tDistance = 
                        distance(tRating1, tRating2, numTestSenses) / tMaxDist;
                    
                    fuzzyDiscordanceSum += Math.abs(gDistance - tDistance);
                }
            }

            final int n = instancesInCommon.size();
            double fuzzyRandIndex = (n > 1) 
                ? 1 - (fuzzyDiscordanceSum / ((n * (n-1)) / 2)) 
                : 0;
            return fuzzyRandIndex;
    }

    /**
     * Returns a {@link Map} where all the items in {@code s} are associated
     * with the value {@code d}.
     */
    private static Map<String,Double> fill(Set<String> s, double d) {
        Map<String,Double> m = new HashMap<String,Double>();
        for (String str : s)
            m.put(str, d);
        return m;
    }

    /**
     * Returns the Manhattan distance between the instance ratings in the
     * <i>same</i> label set.
     */
    private static double distance(Map<String,Double> rating1,
                                   Map<String,Double> rating2,
                                   int numDimensions) {
        Set<String> union = new HashSet<String>(rating1.keySet());
        union.addAll(rating2.keySet());
        double dist = 0;
        for (String s : union) {
            double r1 = rating1.containsKey(s) ? rating1.get(s) : 0;
            double r2 = rating2.containsKey(s) ? rating2.get(s) : 0;
            dist += Math.abs(r1 - r2);
        }
        
        return dist;
    }
}
