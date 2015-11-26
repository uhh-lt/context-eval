#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "usage: evaluate.sh <path-to-golden.key> <path-to-system.key>"
    echo "example: evaluate.sh keys/gold/all.key keys/systems/AI-KU/remove5-add1000/y-22-cluster-test-remove5-add1000.key"
    exit
fi

# mvn package on the first run to generate cct_jar

cct_jar="cluster-comparison-tools/target/cluster-comparison-tools-1.0.0-jar-with-dependencies.jar"
golden=$1  
system=$2  
sense_mapping=""  # or "--no-remapping" for .wn.

for metric in edu.ucla.clustercomparison.cl.JaccardIndexScorer edu.ucla.clustercomparison.cl.WeightedNdcgScorer  edu.ucla.clustercomparison.cl.WeightedTauScorer edu.ucla.clustercomparison.FuzzyNormalizedMutualInformation edu.ucla.clustercomparison.FuzzyBCubed
do
    printf "$metric\n"
    java -cp $cct_jar $metric $sense_mapping $golden $system
done
