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


/**
 * A class for computing an approximation of <a
 * href="http://en.wikipedia.org/wiki/Differential_entropy">Differential
 * Entropy</a>, where samples from a continuous variable are discretized into
 * bins and the entropy is calcuated using <a
 * href="http://en.wikipedia.org/wiki/Information_entropy">discrete methods</a>.
 * This discrete approximation is necessary to avoid the potential negative
 * entropy values that may occur with differential entropy.  The discretization
 * process transforms the <a
 * href="http://en.wikipedia.org/wiki/Probability_density_function">probability
 * density function</a> that would have been genreated from the samples into a
 * <a href="http://en.wikipedia.org/wiki/Multinomial_distribution">multinomial
 * distribution</a> over the probabiity a sample would occur in evenly spaced
 * value ranges.  This class provides functionality for computing discrete
 * approximations of both differential entropy and conditional differential entropy.
 *
 * <p> This class's functionality is currently designed for use with fuzzy
 * variables, which imposes two major functional restrictions (which could be
 * removed with better engineering).  First, the samples are expected to occur
 * in the range [0,1], which indicate membership in a fuzzy cluster.  A robust
 * implementation would allow any sample value.  Second, the class assumes
 * equally sized bins when discretizing the probability density function.  More
 * advanced discretization methods could induce variable sized bins or identify
 * the optimal bin sizes from the data itself.  However, no such process is
 * attempted in this implementation.
 *
 * <p> <i>Note</i>: due to the heavy use of this method, no input checking is
 * done to ensure the input samples are bounded in [0,1].  However, {@code
 * assert} statements are included, which allows the algorithm user to ensure
 * their input is well formed when the Java assertions are enabled.
 */
public class DiscretizedDifferentialEntropy {

    /**
     * The default number of bins used to divide the samples into in order to
     * compute the multinomial appromating the probability density function of a
     * random variable.
     */
    private static final int DEFAULT_NUMBER_OF_BINS = 10;

    /**
     * Computes the multinomial approximation of the probability density
     * function of the variable samples in {@code var1samples}, and returns the
     * entropy of that multinomial as an approximation of the function's
     * differential entropy, using the default number of bins to discretize the
     * values.
     *
     * @param var1samples random samples from a continuous variable, bounded in
     *        the range [0,1]
     */
    public static double compute(double[] var1samples) {
        return compute(var1samples, DEFAULT_NUMBER_OF_BINS);
    }

    /**
     * Computes the multinomial approximation of the probability density
     * function of the variable samples in {@code var1samples}, and returns the
     * entropy of that multinomial as an approximation of the function's
     * differential entropy, using the specified number of bins to discretize the
     * values.
     *
     * @param var1samples random samples from a continuous variable, bounded in
     *        the range [0,1]
     * @param numBins the number of bins in [0,1] into which the values in
     *        {@code var1samples} should be discretized.  For example, setting
     *        this value to {@code 10} would produce bins with ranges of 0.1.
     */
    public static double compute(double[] var1samples, int numBins) {
        assert checkBounds(var1samples) : "var1samples contains values " +
            "that are outside the bounds of [0,1]";
        
        double numSamples = var1samples.length;
        
        Counter<Integer> binCounts = new Counter<Integer>();
        // Discretizes the samples into bins
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
     * Computes the multinomial approximation of the probability density
     * functions of the two variables' samples and then returns the conditional
     * entropy of the two multinomials as an approximation of the two functions'
     * conditional differential entropy, using the default number of bins to
     * discretize the values.
     *
     * @param var1samples random samples from a continuous variable, bounded in
     *        the range [0,1].  The number of samples should be the same as in
     *        {@code var2samples}.
     * @param var2samples random samples from a continuous variable, bounded in
     *        the range [0,1].  The number of samples should be the same as in
     *        {@code var1samples}.
     */
    public static double compute(double[] var1samples, double[] var2samples) {
        return compute(var1samples, var2samples, DEFAULT_NUMBER_OF_BINS);
    }

    /**
     * Computes the multinomial approximation of the probability density
     * functions of the two variables' samples and then returns the conditional
     * entropy of the two multinomials as an approximation of the two functions'
     * conditional differential entropy, using the default number of bins to
     * discretize the values.
     *
     * @param var1samples random samples from a continuous variable, bounded in
     *        the range [0,1].  The number of samples should be the same as in
     *        {@code var2samples}.
     * @param var2samples random samples from a continuous variable, bounded in
     *        the range [0,1].  The number of samples should be the same as in
     *        {@code var1samples}.
     * @param numBins the number of bins in [0,1] into which the values in
     *        {@code var1samples} should be discretized.  For example, setting
     *        this value to {@code 10} would produce bins with ranges of 0.1.    
     */
    public static double compute(double[] var1samples, double[] var2samples, 
                                 int numBins) {
        assert checkBounds(var1samples) : "var1samples contains values " +
            "that are outside the bounds of [0,1]";
        assert checkBounds(var2samples) : "var2samples contains values " +
            "that are outside the bounds of [0,1]";
        
        double numSamples = var1samples.length;
        
        Counter<Pair> jointBins = new Counter<Pair>();
        // Discretizes the samples into bins
        for (int i = 0; i < var1samples.length; ++i) {
            double v1 = var1samples[i];
            double v2 = var2samples[i];
            jointBins.count(new Pair(bin(v1, numBins), bin(v2, numBins)));
        }
        
        double jointEntropy = 0;
        for (int i = 0; i < numBins; ++i) {
            for (int j = 0; j < numBins; ++j) {
                // Count how many items appeared in this joint bin
                int count = jointBins.get(new Pair(i, j));
                if (count > 0) {
                    double prob = count / numSamples;
                    jointEntropy += prob * Log.log2(prob);
                }
            }
        }
        return -jointEntropy - compute(var2samples);
    }

    /**
     * Returns {@code true} if all the values in the sample are in the expected
     * range of [0,1].  This methods is used entirely for assertion-based input
     * validation.
     */
    private static boolean checkBounds(double[] arr) {
        for (double d : arr) {
            if (d < 0 || d > 1)
                return false;
        }
        return true;
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
