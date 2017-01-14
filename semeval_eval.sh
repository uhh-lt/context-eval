#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "usage: semeval_eval.sh <path-to-golden.key> <path-to-system.dataset>"
    echo "example: semeval_eval.sh semeval_2013_13/keys/gold/all.key data/Dataset-SemEval-2013-13-adagram-ukwac-wacky-raw.csv"
    exit
fi


cct_jar="semeval_2013_13/cluster-comparison-tools/target/cluster-comparison-tools-1.0.0-jar-with-dependencies.jar"
golden=$1  
system_dataset=$2  
sense_mapping=""  # or "--no-remapping" for wordnet sense identifiers .wn.
header="--no_header"  # or "" if header is present


python dataset2key.py $system_dataset "$system_dataset.key" $header

for metric in edu.ucla.clustercomparison.cl.JaccardIndexScorer edu.ucla.clustercomparison.cl.WeightedNdcgScorer  edu.ucla.clustercomparison.cl.WeightedTauScorer edu.ucla.clustercomparison.FuzzyNormalizedMutualInformation edu.ucla.clustercomparison.FuzzyBCubed
do
    printf "$metric\n"
    java -cp $cct_jar $metric $sense_mapping $golden "$system_dataset.key"
done
