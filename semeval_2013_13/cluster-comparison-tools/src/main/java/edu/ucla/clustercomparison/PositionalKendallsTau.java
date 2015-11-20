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
import java.util.Set;


/**
 * An implementation of positionally-weighted Kendall's Tau, as described by <a
 * href="http://theory.stanford.edu/~sergei/papers/www10-metrics.pdf">Kumar and
 * Vassilvitskii (2010)</a>.  This class uses a linearly descreasing penalty for
 * swapping the ordering of items, that penalizes misorderings in the first
 * ranks most.
 */
public class PositionalKendallsTau extends AbstractEvaluation {

    /**
     * Computes the positional weighted Kendall's tau of the two sense rankings.
     */
    public double evaluateInstance(final Map<String,Double> goldSenseRatings,
                                   final Map<String,Double> testSenseRatings,
                                   int numSenses) {

        Set<String> all = new HashSet<String>(goldSenseRatings.keySet());
        all.addAll(testSenseRatings.keySet());
        
        if (all.size() == 1)
            return 1;

        // Sanity check if someone the test key is provided more senses that are
        // present in the gold standard sense inventory
        if (all.size() > numSenses) {
            throw new IllegalStateException(
                "Expected " + numSenses + " senses, but these instances have "+
                " a total of " + all.size() + " unique senses: " + all);
        }
        
        List<P> goldOrder = new ArrayList<P>();
        List<P> testOrder = new ArrayList<P>();
        for (String s : all) {
            Double g = goldSenseRatings.get(s);
            Double t = testSenseRatings.get(s);
            goldOrder.add(new P(s, (g == null) ? 0 : g));
            testOrder.add(new P(s, (t == null) ? 0 : t));
        }
        Collections.sort(goldOrder);
        Collections.sort(testOrder);

        int n = all.size();
        
        // Use linearly-decreasing cost
        double[] delta = new double[n];
        for (int i = 0; i < delta.length; ++i) 
            delta[i] = 1 - (i / (double)numSenses);

        // p contains the penality for swaps at each positional distance
        double[] p = new double[n];
        Arrays.fill(p, 1);
        for (int i = 1; i < p.length; ++i) {
            for (int j = 0; j < i; ++j)
                p[i] += delta[j];
        }

        // Create a mappping and inverse mapping from each item to its rank and
        // each rank to the item.  Note that ties are not allowed, so each item
        // is assigned a unqiue rank, with deterministic tie breaking
        Map<String,Integer> goldRanks = new HashMap<String,Integer>();
        String[] invGoldRank = new String[n];
        for (int i = 0; i < goldOrder.size(); ++i) {
            goldRanks.put(goldOrder.get(i).s, i);
            invGoldRank[i] = goldOrder.get(i).s;
        }
        Map<String,Integer> testRanks = new HashMap<String,Integer>();
        String[] invTestRank = new String[n];
        for (int i = 0; i < testOrder.size(); ++i) {
            testRanks.put(testOrder.get(i).s, i);
            invTestRank[i] = testOrder.get(i).s;
        }
        
        double tauDist = 0d;

        // For all i,j pairs where i<j       
        for (int i = 0; i < n; ++i) {

            String s1 = invGoldRank[i];
            int t_i = testRanks.get(s1);

            // Calculate the cost for this swap
            double iCost = (i == t_i)
                ? 1 
                : (p[i] - p[t_i]) / (double)(i - t_i);

            for (int j = i+1; j < n; ++j) {

                String s2 = invGoldRank[j];
                int t_j = testRanks.get(s2);                                
                                                
                double jCost = (j == t_j)
                    ? 1
                    : (p[j] - p[t_j]) / (double)(j - t_j);

                if (t_i > t_j) {
                    tauDist += iCost * jCost;
                }
            }
        }

        // Compute the maxDistance for this instance
        Map<String,Integer> reversedGoldRanks = new HashMap<String,Integer>();
        String[] reversedInvGoldRank = new String[n];
        for (int i = 0; i < goldOrder.size(); ++i) {
            int rev = goldOrder.size() - (i+1);
            reversedGoldRanks.put(goldOrder.get(i).s, rev);
            reversedInvGoldRank[rev] = goldOrder.get(i).s;
        }

        // For all i,j pairs where i<j       
        double maxDist = 0;
        for (int i = 0; i < n; ++i) {

            String s1 = invGoldRank[i];
            int t_i = reversedGoldRanks.get(s1);
            
            // Calculate the cost for this swap
            double iCost = (i == t_i)
                ? 1 
                : (p[i] - p[t_i]) / (double)(i - t_i);

            for (int j = i+1; j < n; ++j) {

                String s2 = invGoldRank[j];
                int t_j = reversedGoldRanks.get(s2);                                
                                                
                double jCost = (j == t_j)
                    ? 1
                    : (p[j] - p[t_j]) / (double)(j - t_j);
                
                if (t_i > t_j) {
                    maxDist += iCost * jCost;
                }
            }
        }

        /*
        double maxDist = 0;
        for (int i = 0; i < n; ++i) {
            int t_i = n - (i + 1);
            double iCost = (i == t_i) ? 1 : (p[i] - p[t_i]) / (i - t_i);
            for (int j = i+1; j < n; ++j) {
                
                // Assume every pair is reversed
                int t_j = n - (j + 1);                
                double jCost = (j == t_j) ? 1 : (p[j] - p[t_j]) / (j - t_j);
                maxDist += iCost * jCost;
            }
        }
        */
       
        return (maxDist == 0) ? 0 : 1 - (tauDist / maxDist);
    }

    /**
     * Utility class for reprsenting a comparable position and value
     */
    static class P implements Comparable<P> {
        
        final String s;
        final double d;

        public P(String s, double d) {
            this.s = s;
            this.d = d;
        }

        @Override public int compareTo(P p) {
            int i = Double.compare(p.d, d);
            // Reversed so larger values are first
            return (i == 0) 
                ? p.s.compareTo(s)
                : i;
        }

        public boolean equals(Object o) {
            if (o instanceof P) {
                P p = (P)o;
                return p.d == d && p.s.equals(s);
            }
            return false;
        }
        
        public String toString() {
            return s + ":" + d;
        }
    }
}
