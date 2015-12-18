# contextualization-eval
A tool for evaluation of contextualization (WSD) based on TWSI 2.0.

Overview
---------------------

The tool evaluates Word Sense Disambiguation performance of a custom WSD system utilizing sentences and word senses (consisting of contextualized word substitutions) from the [Turk Bootstrap Word Sense Inventory TWSI 2.0](https://www.lt.informatik.tu-darmstadt.de/de/data/twsi-turk-bootstrap-word-sense-inventory/)

First, it aligns the provided word sense inventory to TWSI senses. The alignment is calculated as the maximum overlap between related terms from a provided word sense to the substitutions from TWSI.
After alignment, it calculates the precision, recall and F-score of the WSD system.


Evaluation based on TWSI dataset 
--------------------

1. Clone repository:

   ```
    git clone https://github.com/tudarmstadt-lt/contextualization-eval.git
    ```

2. Install dependencies:
    ```
    pip install numpy scipy pandas
    ```

3. Fill with your program columns ```predict_sense_ids``` and ```predict_related``` in ```data/Dataset-TWSI-2.csv``` and save it (you can use as an example ```data/Dataset-TWSI-2-sample.csv```). The first one contains a list of relevant sense identifiers for a given context and the second contains a list of contextually semantically related words. Check columns ```golden_sense_ids``` and ```golden_related``` for example. Data formats: https://github.com/tudarmstadt-lt/contextualization-eval#input-data-format-datadataset-twsi-20csv.

4. Create your word sense inventory needed for mapping senses to the gold standard word sense inventory. A sample file is available at: ```data/Inventory-sample.csv```

5. Evaluate your predictions, based on your word sense inventory ```, e.g.:

    ```
    python twsi_evaluation.py data/Inventory-sample.csv data/Dataset-TWSI-2-sample.csv
    ```
    
    For evaluation, you need to provide the path to the TWSI 2.0 dataset, if it is not in the same directory as the script.
    You can set it using the '-t' parameter:
    
    ```
    python twsi_evaluation.py -t path-to/TWSI2_complete word_sense_inventory predictions

    ```
    
Results of the evaluations are printed to stdout. Most essential metrics are also printed to stderr. You should see something like this:

```
Evaluation Results:
Correct, retrieved, nr_sentences
25465 	63801 	  142644
Precision: 0.399131675052 	Recall: 0.17852135386 	F1: 0.246700089612
Coverage:  0.447274333305


```

Data Format
---------------



###Input data format: *data/Dataset-TWSI-2.0.csv*
To be able to run different evaluation scripts, the TWSI 2.0 data needs to be converted into a different format. Int the *data/* folder, you will find the transformed TWSI data. 

```
context_id  target-lemma   target_POS  target_position   gold_IDs predicted_IDs  gold_related_words   predicted_related_words context
```
####Example
```
10038908       ability n       160,169 1              aptitude:2, strength:4, talent:11, comprehension:1, function:2, competence:1, faculty:3, capability:33, capacity:29, skill:19             The following year , Harchester United reached the Semi Finals of the FA Cup and were also promoted back to the Premiership thanks to the fantastic goalscoring abilities of Karl Fletcher . 
1418247        ability n       45,54   1              aptitude:2, strength:4, talent:11, comprehension:1, function:2, competence:1, faculty:3, capability:33, capacity:29, skill:19             He has also more than once overestimated his abilities or at times is often too naïve or cocky which usually results in a disadvantage during battle . 
...
```

###Word Sense Inventory: *data/word_sense_inventory.csv*

The sense inventory should be in two columns. The first column contains the word lemma, the second column contains the sense identifier for the lemma.
In the third columns, there is a list of related terms. Each of the related terms can be weighted by a number. These numbers are separated by colons ':'.

```
Word  SenseID  list:5,of:3,related:1,words:1
```
####Example
```
mouse 0        mammalian:50,murine:20,Drosophila:10,human:9
mouse 1        rat:200,mice:150,frog:80,sloth:50,rodent:40
mouse 2        joystick:50,keyboard:33,monitor:25,simulation:15
...
```
## TWSI Input data

### Contexts: data/data/TWSI-2.0-all-contexts.txt

We provide the contents from TWSI 2.0 in their original format (tab separated). In the provided file, we have compiled all the contexts that TWSI 2.0 offers.

Format:

```
TWSI_SenseID   target_word    surface_form     sentenceID   tokenized_sentence   confidence_score
```
The sentences are tokenized and contain a '\<b\>' tag around the target word. Additionally, the target word and its surface form are listed in separate columns.

####Example
```
ability@@1  	ability  	abilities   	10038908	   The following year , Harchester United reached the Semi Finals of the FA Cup and were also promoted back to the Premiership thanks to the fantastic goalscoring <b>abilities</b> of Karl Fletcher . 	   1.0
```

#### Extraction

To extract this data, you can just concatenate all the .context files from TWSI:
```
cat path/to/TWSI2_complete/contexts/*.contexts > data/TWSI-2.0-all-contexts.txt
```

#### Conversion to common format

To convert the TWSI format to the one used in our evaluation, you can use the utils/utils/transform-TWSI.pl script.
It requires the TWSI contexts file as well as the TWSI sense inventory:
```
perl utils/transform-TWSI.pl data/TWSI-2.0-all-contexts.txt data/TWSI-2.0-sense-inventory.txt 
```


### TWSI Sense Inventory: data/TWSI-2.0-sense-inventory.txt 

The TWSI sense inventory follows the format for sense inventories: https://github.com/tudarmstadt-lt/contextualization-eval#word-sense-inventory-dataword_sense_inventorycsv

You can extract the TWSI sense inventory, by running the following command:

####Example

```
academic@@1	scholastic:21, educational:13, scholarly:9, university:5
academic@@2	school:3, educational:2, scholastic:2, school calendar:1
academic@@3	scholar:35, professor:11, academician:10, teacher:9, lecturer:8
```

####Extraction

```
find path/to/TWSI2_complete/substitutions/raw_data/all-substitutions/ -name \*.turk* | sort | xargs perl utils/extract-TWSI-inventory.pl > data/TWSI-2.0-sense-inventory.txt 
```

License
-----------
TWSI 2.0 is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported license (https://creativecommons.org/licenses/by-sa/3.0/). The combined TWSI 2.0 sentences are shared under the same license.

The code of the evaluation script is under the Apache Software License (ASL) 2.0 (http://www.apache.org/licenses/LICENSE-2.0).


References
-------------
* [Biemann and Nygaard, 2010] C. Biemann and V. Nygaard (2010): Crowdsourcing WordNet.  In Proceedings of the 5th Global WordNet conference, Mumbai, India. 
* [Biemann, 2012] C. Biemann (2012): Turk Bootstrap Word Sense Inventory 2.0:  A Large-Scale Resource for Lexical Substitution. Proceedings of LREC 2012, Istanbul, Turkey.
