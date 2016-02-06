#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "usage: semeval_2013_13.sh <path-to-golden.key> <path-to-system.dataset>"
    echo "example: semeval_2013_13.sh semeval_2013_13/keys/gold/all.key data/Dataset-SemEval-2013-13-adagram-ukwac-wacky-raw.csv"
    exit
fi

# mvn package on the first run to generate cct_jar

cct_jar="semeval_2013_13/cluster-comparison-tools/target/cluster-comparison-tools-1.0.0-jar-with-dependencies.jar"
golden=$1  
system_dataset=$2  
sense_mapping=""  # or "--no-remapping" for .wn.

python ./semeval_2013_13/dataset2key.py $system_dataset "$system_dataset.key" # --no_header if needed

for metric in edu.ucla.clustercomparison.cl.JaccardIndexScorer edu.ucla.clustercomparison.cl.WeightedNdcgScorer  edu.ucla.clustercomparison.cl.WeightedTauScorer edu.ucla.clustercomparison.FuzzyNormalizedMutualInformation edu.ucla.clustercomparison.FuzzyBCubed
do
    printf "$metric\n"
    java -cp $cct_jar $metric $sense_mapping $golden "$system_dataset.key"
done
