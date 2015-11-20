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


/**
 * An evaluation for computing precision based on direct matches between sense
 * ratings.  This evaluation expects all sense ratings contain a single sense
 */
public class DirectMatch extends AbstractEvaluation {
   
    /**
     * Returns 1 if the ratings have the same sense and 0 if they do not.
     *
     * @throws IllegalArgumentException if either rating contains more than one
     *         sense
     */
    @Override protected double evaluateInstance(
            Map<String,Double> goldSenseRatings,
            Map<String,Double> testSenseRatings,
            int numSenses) {

        if (goldSenseRatings.size() > 1 || testSenseRatings.size() > 1) {
            throw new IllegalArgumentException(
                "The DirectMatch Evaluation cannot be computed for ratings " +
                "with more than one sense per instance.");
        }

        if (goldSenseRatings.isEmpty() || testSenseRatings.isEmpty())
            return 0;

        String s1 = goldSenseRatings.keySet().iterator().next();
        String s2 = testSenseRatings.keySet().iterator().next();
        return (s1.equals(s2)) ? 1 : 0;
    }
}
