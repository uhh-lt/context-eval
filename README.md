This repository contains tools for evaluating the performance of unsupervised Word Sense Disambiguation (WSD) ~~and lexical substitution, the later is also known as "contextualization".~~

The scripts are based on the SemEval 2013 Task 13 dataset and the TWSI 2.0 dataset.


Setup
=====

## Prerequisites
 - Python 2.7 (Python 3 is not supported) 
 - Java 1.7 or higher
 - Either Linux or Mac OS (for Windows users Cygwin might be an option, though it has not been tested)

## Installation

1. Clone this repository.
3. Install dependencies: `pip install -r requirements.txt`.

Run Evaluation
==============

**TLDR; Just go to the respective tool, look for the dataset, fill the `predict_sense_ids` "by hand" and see the example invocation.**

Two evaluation tools exist:

- one for the **SemEval 2013 Task 13 dataset** 
- and the other for the **TWSI 2.0 dataset**. 
 
Both tools handle sense alignment of the sense inventories between the provided datasets and your system senses differently. Please see the section of each tool for more information.

The schema of the datasets for both evaluation tools is however the same: a tab seperated CSV file, with no quoting and no escape character and the following headers:

```
context_id	target	target_pos	target_position	gold_sense_ids	predict_sense_ids	golden_related	predict_related	context
```

## General invocation procedure

The general procedure for both tools is to fill in the respective dataset the column `predict_sense_ids` "by hand" with the predicted sense identifiers of your system. And than provide this modified dataset to each tool.

_TODO: Mention multiple ids and wether order has any relevance._

Also note that if your system cannot confidently classify some contexts, you can, by setting the `predict_sense_ids` to `-1`, implement a "reject option".

## SemEval 2013 Evaluation Tool
This tool is based on the SemEval 2013 Task 13 **dataset**, which is located at `data/Dataset-SemEval-2013-13.csv`.

Sense alignment is done by the usage of cluster grouping between your sense IDs and the SemEval dataset senses. Hence the tool does not need any additional information about your senses.

For a more detailed introduction to the SemEval 2013 Task 13 evaluation and dataset refer to: http://www.aclweb.org/website/old_anthology/S/S13/S13-2.pdf#page=326

_TODO: mention [Cluster comparision tools](https://code.google.com/p/cluster-comparison-tools/) reference_
### Invocation procedure

1. Fill the dataset with your sense IDs (see General invocation procedure )
   _Note: you must delete the headers!_
2. Provide the dataset to the script `semeval_eval.sh` and also the SemEval sense keys (see example below)
4. Results are printed to stdout

#### Example invocation

_Note: The files used here actually exist in the repository and you can use them for reference._

```bash
./semeval_eval.sh semeval_2013_13/keys/gold/all.key data/Dataset-SemEval-2013-13-adagram-ukwac-wacky-raw.csv
```

Here the first argument `semeval_2013_13/keys/gold/all.key` however is a list of all gold senses and will always need to be exactly this file. The second argument `data/Dataset-SemEval-2013-13-adagram-ukwac-wacky-raw.csv` is the dataset filled with sense IDs and you should change it to your modified dataset.

## TWSI 2.0 Evaluation Tool

This tool is based on the TWSI 2.0 **dataset**, which is located at `data/Dataset-SemEval-2013-13.csv`.

For sense alignment it requires you to provide a word sense inventory with related terms. The alignment is calculated as the maximum overlap between related terms from your word sense inventory to sense substitutions provided by TWSI.

For an in depth introduction to the TWSI word senses and their contextualized word substitutions refer to https://www.lt.informatik.tu-darmstadt.de/de/data/twsi-turk-bootstrap-word-sense-inventory/

### Invocation procedure

1. Fill the dataset with your sense IDs (see General invocation procedure) and gzip it.
2. Create a file containing the whole sense inventory your system uses of the following format: a tab separated CSV file, with no quoting and no escape character and the following columns: `word`, `sense_id`, `related_words`. The `related_words` column is a comma separated word list, where each word can have a colon separated weight, i.e. `list:5,of:3,related:1,words:1`. And the `sense_id` must be numeric. 
    _Note: the file must be given without the header!_
3. Provide both files from the previous two steps to the python script `twsi_eval.py`.
    _Note: in case your dataset file has no header, you can provide the `--no-header` flag!_
4. Results of the evaluations are printed to stdout. Most essential metrics are also printed to stderr.

#### Example invocation

_Note: The files used here actually exist in the repository and you can use them for reference._

```bash
python twsi_eval.py data/Inventory-TWSI-2.csv data/Dataset-TWSI-2-GOLD.csv.gz
```

Here the first argument `data/Inventory-TWSI-2.csv` should contain the sense inventory of your system as described in the 2. point above. You need to change it. The second argument `data/Dataset-TWSI-2-GOLD.csv.gz` is the dataset filled with sense IDs and gzipped and of course you also need to change it to your modified dataset. 

License
-----------
TWSI 2.0 is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported license (https://creativecommons.org/licenses/by-sa/3.0/). The combined TWSI 2.0 sentences are shared under the same license.

The code of the evaluation script is under the Apache Software License (ASL) 2.0 (http://www.apache.org/licenses/LICENSE-2.0).


References
-------------
* [Biemann and Nygaard, 2010] C. Biemann and V. Nygaard (2010): Crowdsourcing WordNet.  In Proceedings of the 5th Global WordNet conference, Mumbai, India. 
* [Biemann, 2012] C. Biemann (2012): Turk Bootstrap Word Sense Inventory 2.0:  A Large-Scale Resource for Lexical Substitution. Proceedings of LREC 2012, Istanbul, Turkey.
