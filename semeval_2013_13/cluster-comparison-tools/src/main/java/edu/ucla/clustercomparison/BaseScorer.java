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

import java.util.*;

import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * The main scoring procedure for supervised evaluations.  This class handles
 * the 80/20 test-train split and sense mapping, delegating the specific form of
 * evaluation to its subclasses.
 */
public abstract class BaseScorer {
    
    static final int randomSeed = 42;

    /**
     * The source of randomness for dividing the train and test splits.  This
     * randomness is deterministic thanks to the constant seed.
     */
    static final Random rand = new Random(randomSeed);
    
    /**
     * The logger for this class
     */
    private static final Logger LOGGER = 
        Logger.getLogger(BaseScorer.class.getName());;
    
    /**
     * Computes the score of the evaluation between the two SensEval keys file,
     * optionally performing remapping and optionally writing the remapped key
     * to {@code outputKeyFile}.
     *
     * @param goldKeyFile the SensEval key file against which the test key is to
     *        compared
     * @param testKeyFile the SensEval key file to be compared.  If the key is
     *        using a different sense inventory, {@code performRemapping} should
     *        be set to {@code true}.
     * @param outputKeyFile if {@code performRemapping} is {@code true}, the
     *        remapped key is written to this file if non-{@code null}.
     * @param performRemapping whether the test key should have its instance
     *        labels remapped into the gold key's label set using the default
     *        {@link GradedReweightedKeyMapper} algorithm
     */
    public double[] score(File goldKeyFile, File testKeyFile,
                          File outputKeyFile, boolean performRemapping) 
              throws Exception {

        // Load the gold standard and induced key files
        Map<String,Map<String,Map<String,Double>>> goldKey = 
            KeyUtil.loadKey(goldKeyFile);
        Map<String,Map<String,Map<String,Double>>> testKey = 
            KeyUtil.loadKey(testKeyFile);     
        
        return score(goldKey, testKey, outputKeyFile, performRemapping);
    }

    /**
     * Computes the score of the evaluation between the two SensEval keys file,
     * optionally performing remapping and optionally writing the remapped key
     * to {@code outputKeyFile}.
     *
     * @param goldKeyFile the SensEval key file against which the test key is to
     *        compared
     * @param testKeyFile the SensEval key file to be compared.  If the key is
     *        using a different sense inventory, {@code performRemapping} should
     *        be set to {@code true}.
     * @param outputKeyFile if {@code performRemapping} is {@code true}, the
     *        remapped key is written to this file if non-{@code null}.
     * @param keyMapper an algorithm for converting sense keys in one inventory
     *        into another, or {@code null} if no remapping is to be performed
     */
    public double[] score(File goldKeyFile, File testKeyFile,
                          File outputKeyFile, KeyMapper keyMapper) 
            throws Exception {

        // Load the gold standard and induced key files
        Map<String,Map<String,Map<String,Double>>> goldKey = 
            KeyUtil.loadKey(goldKeyFile);
        Map<String,Map<String,Map<String,Double>>> testKey = 
            KeyUtil.loadKey(testKeyFile);     
        
        return score(goldKey, testKey, outputKeyFile, keyMapper);
    }

    /**
     * Computes the score of the evaluation between the two keys, optionally
     * performing remapping and optionally writing the remapped key to {@code
     * outputKeyFile}.
     *
     * @param goldKey the key against which the test key is to compared
     * @param testKey the key to be compared.  If the key is using a different
     *        sense inventory, {@code performRemapping} should be set to {@code
     *        true}.
     * @param outputKeyFile if {@code performRemapping} is {@code true}, the
     *        remapped key is written to this file if non-{@code null}.
     * @param performRemapping whether the test key should have its instance
     *        labels remapped into the gold key's label set using the default
     *        {@link GradedReweightedKeyMapper} algorithm
     */
    public double[] score(Map<String,Map<String,Map<String,Double>>> goldKey,
                        Map<String,Map<String,Map<String,Double>>> testKey,
                        File outputKeyFile, boolean performRemapping) 
            throws Exception {
        return score(goldKey, testKey, outputKeyFile, 
                     ((performRemapping) 
                      ? new GradedReweightedKeyMapper() : null));
    }

    /**
     * Computes the score of the evaluation between the two keys, optionally
     * performing remapping and optionally writing the remapped key to {@code
     * outputKeyFile}.
     *
     * @param goldKey the key against which the test key is to compared
     * @param testKey the key to be compared.  If the key is using a different
     *        sense inventory, {@code performRemapping} should be set to {@code
     *        true}.
     * @param outputKeyFile if {@code performRemapping} is {@code true}, the
     *        remapped key is written to this file if non-{@code null}.
     * @param keyMapper an algorithm for converting sense keys in one inventory
     *        into another, or {@code null} if no remapping is to be performed
     */
    public double[] score(Map<String,Map<String,Map<String,Double>>> goldKey,
                          Map<String,Map<String,Map<String,Double>>> testKey,
                          File outputKeyFile, KeyMapper keyMapper) 
            throws Exception {

        
        PrintWriter outputGradedVectorKey = 
            (outputKeyFile == null) ? null : new PrintWriter(outputKeyFile);

        List<String> allInstances = new ArrayList<String>();
        for (Map<String,Map<String,Double>> m : goldKey.values())
            allInstances.addAll(m.keySet());
        
        // Create a list of all the indices of the instances in the gold key and
        // then permute that list in a deterministic manner to decide on the
        // 80/20 splits.  The shuffle avoids any potiential bias that was in the
        // original ordering.
        List<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < allInstances.size(); ++i)
            indices.add(i);
        Collections.shuffle(indices, rand);
        
        // Create the sets of training instances by dividing the key into 80%
        // train, 20% test
        List<Set<String>> trainingSets = new ArrayList<Set<String>>();
        for (int i = 0; i < 5; ++i)
            trainingSets.add(new HashSet<String>());
        for (int i = 0; i < allInstances.size(); ++i) {
            String instance = allInstances.get(i);
            int toExclude = i % trainingSets.size();
            for (int j = 0; j < trainingSets.size(); ++j) {
                if (j == toExclude)
                    continue;
                trainingSets.get(j).add(instance);
            }
        }
              

        // Perform a quick sanity check with respect to the remapping
        Set<String> goldSenses = new HashSet<String>();
        for (Map.Entry<String,Map<String,Map<String,Double>>> e : goldKey.entrySet()) {
            String word = e.getKey();
            for (Map<String,Double> ratings : e.getValue().values())
                goldSenses.addAll(ratings.keySet());
        }
        
        Set<String> testSenses = new HashSet<String>();
        for (Map.Entry<String,Map<String,Map<String,Double>>> e : testKey.entrySet()) {
            String word = e.getKey();
            for (Map<String,Double> ratings : e.getValue().values())
                testSenses.addAll(ratings.keySet());
        }
        double origSize = goldSenses.size();        
        goldSenses.removeAll(testSenses);
//         if (goldSenses.size() / origSize < 0.25 && performRemapping) {
//             System.err.println("ATTENTION: " +                               
//                 "It appears that your test key is using the same sense-IDs as\n"+
//                 "the supplied gold standard key, but you are also remapping\n" +
//                 "the testing key's labels.  Are you sure you don't want to\n" +
//                 "use the --no-remapping option?");
//         }
//         else if (origSize == goldSenses.size() && !performRemapping) {
//             System.err.println("ATTENTION: " +
//                 "It appears that your test key is not using the same sense-IDs\n"+
//                 "as the supplied gold standard key, but you are not remapping\n" +
//                 "the testing key's sense labels into the gold key's sense\n" +
//                 "inventory.  Are you sure you want to use the --no-remapping\n"+
//                 "option?");
//         }


        // We approximate the number of senses for this term by examining the
        // totality of senses used for the term in the gold standard labeling.
        // In the case where the user has specified that they are using the same
        // sense inventory (perfromRemapping==false), then we also include these
        // senses, as they use may be referring to senses that aren't in the
        // gold standard solution.
        Map<String,Integer> termToNumberSenses = 
            new HashMap<String,Integer>();
        for (Map.Entry<String,Map<String,Map<String,Double>>> e : goldKey.entrySet()) {
            String term = e.getKey();
            Set<String> senses = new HashSet<String>();
            for (Map<String,Double> ratings : e.getValue().values())
                senses.addAll(ratings.keySet());
            if (keyMapper == null) {
                Map<String,Map<String,Double>> m = testKey.get(term);
                if (m != null) {
                    for (Map<String,Double> ratings : m.values())
                        senses.addAll(ratings.keySet());
                }
            }
            termToNumberSenses.put(term, senses.size());
        }

        // Score the test key
        Map<String,Double> instanceScores  
            = runEval(getEvaluation(), keyMapper,
                      goldKey, testKey, trainingSets, 
                      allInstances, outputKeyFile, keyMapper != null,
                      termToNumberSenses);
        
        
        Map<String,String> instanceToWord = new HashMap<String,String>();
        for (Map.Entry<String,Map<String,Map<String,Double>>> e : goldKey.entrySet()) {
            String word = e.getKey();
            for (String instance : e.getValue().keySet())
                instanceToWord.put(instance, word);
        }

        Map<String,List<Double>> termToScores = new LinkedHashMap<String,List<Double>>();
        for (String term : goldKey.keySet())
            termToScores.put(term, new ArrayList<Double>());

        for (Map.Entry<String,Double> e : instanceScores.entrySet()) {
            String inst = e.getKey();
            String term = instanceToWord.get(inst);
            List<Double> scores = termToScores.get(term);
            scores.add(e.getValue());
        }

        // Generate the report
        double allScoresSum = 0;
        double numAnswered = 0;
        int na = 0;
        System.out.println("===================================================================");
        System.out.printf("term\taverage_score\trecall\tf-score%n");
        System.out.println("-------------------------------------------------------------------");
        for (Map.Entry<String,List<Double>> e : termToScores.entrySet()) {
            String term = e.getKey();
            double numInstances = goldKey.get(term).size();
            List<Double> scores = e.getValue();
            double recall = scores.size() / numInstances;
            numAnswered += scores.size();
            na += scores.size();
            double sum = 0;
            for (Double d : scores) {
                if (Double.isNaN(d) || Double.isInfinite(d)) 
                    throw new Error();
                sum += d;
            }
            allScoresSum += sum;
            double avg = (scores.size() > 0) ? sum / scores.size() : 0;
            if (Double.isNaN(avg) || Double.isInfinite(avg)) 
                throw new IllegalStateException();
            double fscore = (avg + recall > 0) 
                ? (2 * avg * recall) / (avg + recall)
                : 0;
            
            System.out.println(term + "\t" + avg + "\t" + recall + "\t" + fscore);
        }
         System.out.println("-------------------------------------------------------------------");
        // Print out the aggregate
        double avg = (numAnswered > 0) ? allScoresSum / numAnswered : 0;
        // Recall is the percentage of all instances that were answered
        // correctly.  If a test key answers all instances, recall is the same
        // as precision
        double recall = (na == allInstances.size()) 
            ? avg
            : avg * (numAnswered / allInstances.size());
        double fscore = (avg + recall > 0) 
            ? (2 * avg * recall) / (avg + recall)
            : 0;
            
         System.out.println("all\t" + avg + "\t" + recall + "\t" + fscore);        
         System.out.println("===================================================================");
         return new double[] { avg, recall, fscore };
    }

    /**
     * Returns the evaluation to be used
     */
    protected abstract Evaluation getEvaluation();

    /**
     * Computes the evaluation over the all the test-training splits.
     */
    Map<String,Double> runEval(Evaluation evaluation,
                               KeyMapper keyMapper, 
                               Map<String,Map<String,Map<String,Double>>> goldKey,
                               Map<String,Map<String,Map<String,Double>>> testKey,
                               List<Set<String>> trainingSets,
                               List<String> allInstances,
                               File outputKey, 
                               boolean performRemapping,
                               Map<String,Integer> termToNumberSenses) throws IOException {
        
        Map<String,Double> instanceScores = new LinkedHashMap<String,Double>();

        PrintWriter outputKeyWriter = (outputKey == null) 
            ? null : new PrintWriter(outputKey);

        Map<String,Map<String,Map<String,Double>>> allRemappedTestKey        
            = (performRemapping && outputKeyWriter != null) 
            ? new TreeMap<String,Map<String,Map<String,Double>>>()
            : null;

        int round = 0;
        for (Set<String> trainingInstances : trainingSets) {

            // Map the induced senses to gold standard senses
            Map<String,Map<String,Map<String,Double>>> remappedTestKey = 
                (performRemapping)
                ? keyMapper.convert(goldKey, testKey, trainingInstances)
                : testKey;
            
            // If the user has specified that we need to produce the output key,
            // write it now
            if (performRemapping && outputKeyWriter != null) {
                // Merge this remapped test set with the total key
                for (Map.Entry<String,Map<String,Map<String,Double>>> e 
                         : remappedTestKey.entrySet()) {
                    String doc = e.getKey();
                    Map<String,Map<String,Double>> instRatings = e.getValue();
                    Map<String,Map<String,Double>> curInstRatings = 
                        allRemappedTestKey.get(doc);
                    if (curInstRatings == null) {
                        // Sort them so that term.pos.2 < term.pos.10
                        curInstRatings = new TreeMap<String,Map<String,Double>>(
                            new InstanceComparator());
                        curInstRatings.putAll(instRatings);
                        allRemappedTestKey.put(doc, curInstRatings);
                    }
                    else {
                        assert Collections.disjoint(
                            curInstRatings.keySet(), instRatings.keySet())
                            : "Overlapping instances in the test / train sets";
                        curInstRatings.putAll(instRatings);
                    }
                }
            }
                

            // Determine which set of instances should be tested
            Set<String> instancesToTest = new LinkedHashSet<String>();
            for (Map<String,?> m : goldKey.values())
                instancesToTest.addAll(m.keySet());
            instancesToTest.removeAll(trainingInstances);
            
            verbose(LOGGER, "Testing split %d ", round);
            
            Map<String,Double> scores = 
                evaluation.test(remappedTestKey, goldKey, instancesToTest,
                                termToNumberSenses);
            instanceScores.putAll(scores);
            round++;
        }

        // Finish writing the key 
        if (outputKeyWriter != null) {

            for (Map.Entry<String,Map<String,Map<String,Double>>> e : 
                     goldKey.entrySet()) {
                String t = e.getKey();
                Set<String> toRetain = e.getValue().keySet();
                Map<String,Map<String,Double>> test = allRemappedTestKey.get(t);
                if (test != null)
                    test.keySet().retainAll(toRetain);
            }

            verbose(LOGGER, "Saving remapped key file to ", outputKey);
            writeKey(allRemappedTestKey, outputKeyWriter);
            outputKeyWriter.close();  
        }      
        
        return instanceScores;
    }        
    
    /**
     * Writes the Senseval key file for this remapping
     */
    static void writeKey(Map<String,Map<String,Map<String,Double>>> remappedTestKey,
                         PrintWriter pw) {

        for (Map.Entry<String,Map<String,Map<String,Double>>> e 
                 : remappedTestKey.entrySet()) {
            String term = e.getKey();
            Map<String,Map<String,Double>> instances = e.getValue();
            for (Map.Entry<String,Map<String,Double>> e2 
                     : instances.entrySet()) {
                String instance = e2.getKey();
                Map<String,Double> senses = e2.getValue();
                StringBuilder sb = new StringBuilder(term);
                sb.append(' ').append(instance);
                for (Map.Entry<String,Double> e3 : senses.entrySet()) {
                    sb.append(' ').append(e3.getKey())
                        .append('/').append(e3.getValue());
                }
                pw.println(sb);
            }
        }
    }

    static class InstanceComparator implements Comparator<String> {

        /**
         * Compares instances labeled as term.pos.number, sorting them by term,
         * pos, and number.
         */
        public int compare(String s1, String s2) {
            String[] arr1 = s1.split("\\.");
            String[] arr2 = s2.split("\\.");
            // Sort by term first
            int m = arr1[0].compareTo(arr2[0]);
            if (m != 0)
                return m;
            // Then by pos
            m = arr1[1].compareTo(arr2[1]);
            if (m != 0)
                return m;
            // Last by number
            int i = Integer.parseInt(arr1[2]);
            int j = Integer.parseInt(arr2[2]);
            return i - j;
        }
    }
}
