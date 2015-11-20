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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * The utility class for loading <a
 * href="http://www.senseval.org/senseval3/scoring">SensEval</a> keys from a
 * file.  A key is represented as a {@link Map} from each of its documents to
 * the annotations for each of its associated instances.  Instances are
 * represented as a {@link Map} from the instance identifier to a second {@link
 * Map} from each sense key to its numeric rating.
 */
public class KeyUtil {

    public static boolean loadWithStrictParsing = false;

    /**
     * Loads a key file returning a mapping from each document to its instances,
     * where an instance is a mapping from an instance key to the graded senses
     * that were present in that instance, ignoring lines that are malformed.
     */
    public static Map<String,Map<String,Map<String,Double>>> 
            loadKey(String filename) throws IOException {
        return loadKey(new File(filename), loadWithStrictParsing);
    }

    /**
     * Loads a key file returning a mapping from each document to its instances,
     * where an instance is a mapping from an instance key to the graded senses
     * that were present in that instance, optionally ignoring lines that are
     * malformed.
     *
     * @param isStrict if {@code true} lines that are malformed will cause an
     *        {@link IllegalStateException} to be thrown
     */
    public static Map<String,Map<String,Map<String,Double>>> 
            loadKey(String filename, boolean isStrict) throws IOException {
        return loadKey(new File(filename), isStrict);
    }

    /**
     * Loads a key file returning a mapping from each document to its instances,
     * where an instance is a mapping from an instance key to the graded senses
     * that were present in that instance, ignoring lines that are malformed.
     */
    public static Map<String,Map<String,Map<String,Double>>> 
            loadKey(File file) throws IOException {
        return loadKey(file, loadWithStrictParsing);
    }

    /**
     * Loads a key file returning a mapping from each document to its instances,
     * where an instance is a mapping from an instance key to the graded senses
     * that were present in that instance, optionally ignoring lines that are
     * malformed.
     *
     * @param isStrict if {@code true} lines that are malformed will cause an
     *        {@link IllegalStateException} to be thrown
     */
    public static Map<String,Map<String,Map<String,Double>>> 
           loadKey(File file, boolean isStrict) throws IOException {

        Map<String,Map<String,Map<String,Double>>> documentToInstances =
            new LinkedHashMap<String,Map<String,Map<String,Double>>>();

        BufferedReader br = new BufferedReader(new FileReader(file));
        int lineNo = 1;
        for (String line = null; (line = br.readLine()) != null; lineNo++) {
            String[] arr = line.split(" ");
            if (arr.length < 3) {
                // If the parsing doesn't need to be strict, just ignore this
                // line
                if (!isStrict)
                    continue;
                throw new IllegalStateException(
                    "Malformed sense description on line " + lineNo +
                    " in file " + file + ":\n" + line +
                    "\nSee http://www.senseval.org/senseval3/scoring " +
                    "for format details");
            }
            String document = arr[0];
            String instanceId = arr[1];

            // Iterate over all the senses with associated weights.  Per format
            // guidelines, if no weights are specified, a uniform distribution
            // is used.
            Map<String,Double> senseWeights = new LinkedHashMap<String,Double>();
            double senseWeightSum = 0;
            double maxWeight = 0;
            double weightsSeen = 0;
            for (int i = 2; i < arr.length; ++i) {
                // The sense description may end with an optional comment which
                // is preceeded by a !!
                if (arr[i].startsWith("!!"))
                    break;
                String[] arr2 = arr[i].split("/");
                // If no weights were seen for this sense, then insert it with a
                // dummy value, which will be ignored during normalization 
                if (arr2.length == 1) {
                    senseWeights.put(arr2[0], 0d);
                }
                // If a weight was seen, add it to the sum and record that one
                // was used so that we can normalize
                else {
                    Double weight = null;
                    try {
                        weight = Double.parseDouble(arr2[1]);
                    } catch (NumberFormatException nfe) {
                        throw new Error(
                            "Malformed sense weight in " + file +
                            " on line " + lineNo +
                            ":\n" + line + 
                            "\nSee http://www.senseval.org/senseval3/scoring "+
                            "for format details");
                    }
                    senseWeights.put(arr2[0], weight);
                    senseWeightSum += weight;
                    if (weight > maxWeight)
                        maxWeight = weight;
                    weightsSeen++;
                }
            }

            // Once all the senses have been seen for this instance, either
            // normalize them, or if not enough were specified, set them with a
            // uniform value.
            if (weightsSeen != senseWeights.size()) {
                for (Map.Entry<String,Double> e : senseWeights.entrySet()) 
                    e.setValue(1d);                
            }
            else {
                for (Map.Entry<String,Double> e : senseWeights.entrySet()) 
                    // e.setValue(e.getValue() / senseWeightSum);
                    e.setValue(e.getValue() / maxWeight);
            }

            // When the sense weights have been properly set or normalized, add
            // them to the instance mapping
            Map<String,Map<String,Double>> instanceToSenses = 
                documentToInstances.get(document);
            if (instanceToSenses == null) {
                instanceToSenses = new LinkedHashMap<String,Map<String,Double>>();
                documentToInstances.put(document, instanceToSenses);
            }
            instanceToSenses.put(instanceId, senseWeights);
        }
        br.close();
        return documentToInstances;
    }
}
