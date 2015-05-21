# contextualization-eval
A tool for evaluation of contextualization (WSD) based on TWSI 2.0.

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