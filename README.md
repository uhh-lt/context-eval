# contextualization-eval
A tool for evaluation of contextualization (WSD) based on TWSI 2.0.

Overview
---------------------

The tool evaluates Word Sense Disambiguation performance of a custom WSD system utilizing sentences and word senses (consisting of contextualized word substitutions) from the Turk Bootstrap Word Sense Inventory (TWSI 2.0; https://www.lt.informatik.tu-darmstadt.de/de/data/twsi-turk-bootstrap-word-sense-inventory/)

First, it aligns the provided word sense inventory to TWSI senses. The alignment is calculated as the maximum overlap between related terms from a provided word sense to the substitutions from TWSI.
After alignment, it calculates the precision, recall and F-score of the WSD system.



Prerequesites
------------------------

You need to download and extract the TWSI2 dataset: https://www.lt.informatik.tu-darmstadt.de/de/data/twsi-turk-bootstrap-word-sense-inventory/



Running evaluation script 
--------------------

This are instructions are for Ubuntu Linux, but the script should work well on Mac OSX and Windows as well (just install required dependencies). 


1. Download the TWSI2 dataset from https://www.lt.informatik.tu-darmstadt.de/de/data/twsi-turk-bootstrap-word-sense-inventory/

2. Download the evaluation script:

   - Direct download from GitHub: https://github.com/tudarmstadt-lt/contextualization-eval/raw/master/twsi-evaluation.py
   
   - Clone the git repository:
   
       ```
       git clone https://github.com/tudarmstadt-lt/contextualization-eval.git
       ```

3. Install required components

    ```
    sudo apt-get install python-numpy python-scipy python-pandas
    ```

4. Assign sense IDs to sentences in *file*

5. Run the evaluation:

    ```
    # check parameters
    python twsi-evaluation.py -h

    # evaluate your predictions, based on your word sense inventory
    python twsi-evaluation.py word_sense_inventory predictions
    ```
    
    For evaluation, you need to provide the path to the TWSI2.0 dataset, if it is not in the same directory as the script.
    You can set it using the '-t' parameter:
    
    ```
    python twsi-evaluation.py -t path-to/TWSI2_complete word_sense_inventory predictions

    ```
    

Results of the evaluations are printed to stdout. Most essential metrics are also printed to stderr. You should see something like this:


```
Evaluation:
Correct, retrieved, nr_sentences
    2	    13		143594
Precision: 0.153846153846 	Recall: 0.39576 	F1: 0.221567

```

Data Format
---------------

As the system is directly working on the TWSI 2.0 data, it extracts the TWSI sense inventory and the evaluation data automatically. The uses only needs to provide a sense inventory and the predictions of his system.

###Word Sense Inventory

The sense inventory should be in two columns. The first column contains the sense identifier, consisting of the word and the sense ID, separated by a separator (default: '_').

```
Word'SEP'SenseID     list,of,related,words
```
####Example
```
mouse_0     mammalian,murine,Drosophila,human,vertebrate
mouse_1     rat,mice,frog,sloth,rodent,koala,rabbit,lizard,cat
mouse_2     joystick,keyboard,monitor,simulation,networks,hardware,cursor,graphics,worm,lab
...
```

###Predictions

The predictions contain two columns, first the sentence ID from TWSI, second the sense identifier (same identifier as in the word sense inventory).
If the system was not able to assign a word sense id, this column can be empty.

```
SentenceID     Word'SEP'SenseID
```
####Example
```
17367113	   type_1
17948117	   type_0
19032445	   type_0
19179157	   type_0
19651374	   
22028271	   type_0
22585018	   type_1
```
