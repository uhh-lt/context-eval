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
 * The implementation of the <a
 * href="http://en.wikipedia.org/wiki/Jaccard_index">Jaccard index</a> for
 * comparing two sense labelings
 */
public class JaccardIndex extends AbstractEvaluation {
   
    /**
     * Computes the Jaccard Index of the two sense listings
     */
    @Override protected double evaluateInstance(
            Map<String,Double> goldSenseRatings,
            Map<String,Double> testSenseRatings,
            int numSenses) {

    
        int inCommon = 0;
        for (String sense : goldSenseRatings.keySet()) {
            if (testSenseRatings.containsKey(sense))
                inCommon++;
        }
                     
        int unionSize = goldSenseRatings.size() + testSenseRatings.size()
            - inCommon;
        
        double jaccardIndex =  inCommon / (double)unionSize;
        return jaccardIndex;
    }
}
