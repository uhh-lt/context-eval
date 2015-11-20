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

import java.io.*;

/**
 * A utility class for calculating the <a
 * href="http://en.wikipedia.org/wiki/Entropy_(information_theory)">entropy</a>
 * and <a href="http://en.wikipedia.org/wiki/Conditional_entropy"conditional
 * entropy</a> of the discritized <a
 * href="http://en.wikipedia.org/wiki/Probability_density_function">probability
 * density function<a> generated from the contininous variables representing
 * cluster membership.
 *
 * <p>Note that this class assumes that all variables are samples of cluster
 * membership distributions in [0,1].  Using values outside this range will
 * cause errors in the results but will not throw an exception.
 */
public class Entropy {

    /**
     * Computes the the entropy of the probability density function estimated
     * from the cluster membership samples in {@code var1samples} using the
     * default number of bins (10) to discretize the variable.
     */
    public static double compute(double[] var1samples) {
        return compute(var1samples, 10);
    }

    /**
     * Computes the the entropy of the probability density function estimated
     * from the cluster membership samples in {@code var1samples} using the
     * provided number of bins to discretize the variable.
     */
    public static double compute(double[] var1samples, int numBins) {
        
        double numSamples = var1samples.length;
        
        Counter<Integer> binCounts = new Counter<Integer>();
        // Quantize the samples into bins
        for (int i = 0; i < var1samples.length; ++i) {
            double v1 = var1samples[i];
            binCounts.count(bin(v1, numBins));
        }
        
        double entropy = 0;
        for (int i = 0; i < numBins; ++i) {
            // Count how many items appeared in this bin
            int count = binCounts.get(i);
            if (count > 0) {
                double prob = count / numSamples;
                entropy += prob * Log.log2(prob);
            }
        }
        return -entropy;
    }

    /**
     * Computes H(X|Y) where X is represented as samples contained in {@code
     * var1samples} and the values for Y are in {@code var2samples}
     */
    public static double conditionalEntropy(double[] var1samples, 
                                            double[] var2samples) {
        final int NUM_BINS = 10;
        
        double numSamples = var1samples.length;
        
        Counter<Pair> jointBins = new Counter<Pair>();
        // Quantize the samples into bins
        for (int i = 0; i < var1samples.length; ++i) {
            double v1 = var1samples[i];
            double v2 = var2samples[i];
            jointBins.count(new Pair(bin(v1, NUM_BINS), bin(v2, NUM_BINS)));
        }
        
        double jointEntropy = 0;
        for (int i = 0; i < NUM_BINS; ++i) {
            for (int j = 0; j < NUM_BINS; ++j) {
                // Count how many items appeared in this joint bin
                int count = jointBins.get(new Pair(i, j));
                if (count > 0) {
                    double prob = count / numSamples;
                    jointEntropy += prob * Log.log2(prob);
                }
            }
        }

        // Return H(X,Y) - H(Y), which is equivalent to the conditional entropy
        return -jointEntropy - compute(var2samples);
    }

    /**
     * Bins the value into one of {@code numBins} that exist in the range [0, 1]
     */
    static int bin(double val, int numBins) {
        // Start at 1 to capture the first bin of [0, 1/numBins]
        for (int i = 1; i <= numBins; ++i) {
            if (val <= ((double)i) / numBins)
                return i - 1;
        }
        return numBins - 1;
    }
}
