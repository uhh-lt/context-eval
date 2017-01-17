
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
The sentences are tokenized ~~and contain a '\<b\>' tag around the target word~~. Additionally, the target word and its surface form are listed in separate columns.

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
