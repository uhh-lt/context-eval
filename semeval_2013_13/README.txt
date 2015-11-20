===============================================================================

SemEval-2013 Task 13: Word Sense Induction for Graded and Non-Graded Senses

David A. Jurgens and Ioannis P. Klapaftis.  September 1, 2013

This README.txt file describes the test data for SemEval-2013 Task 13. For
information about SemEval-2013 Task 13, see the task website
http://www.cs.york.ac.uk/semeval-2013/task13/.


TASK OVERVIEW:

Previous SemEval tasks on word senses have largely assumed that each usage of a
word is best labeled by a single sense.  In contrast, Task 13 proposes that
usages should be labeled by all senses that apply, with weights indicating the
degree of applicability.  This multi-sense labeling effectively captures both
cases where related senses from a fine-grain sense inventory apply and where
contextual ambiguity enables alternate interpretations.  We illustrate this with
three example sentences:

 1. The student loaded paper into the printer
 2. The student submitted her paper by email.
 3. The student handed her paper to the teacher at the beginning of class

according to the first two senses of paper in WordNet 3.1:

 1) paper - a material made of cellulose pulp derived mainly from wood or rags
    or certain grasses
 2) paper - an essay, especially one written as an assignment

The first sentence refers to the material sense of paper, while the second
sentence refers to the essay sense of paper.  In contrast, both senses are
possible interpretations in the third sentence, though with different degrees;
here, the usage evokes separate properties of the concept of its form (a
cellulose material) and purpose (an assignment), which are themselves distinct
senses of paper.  Similar multi-label conditions may also be constructed for
word uses where a reader perceives multiple, unrelated interpretations due to
contextual ambiguity.  While most previous work on WSD makes a best guess as to
which interpretation is correct, Task 13 opts to make explicit the ambiguity
explicit in the multi-sense labeling.


TASK

Task 13 evaluates Word Sense Induction (WSI) and Unsupervised WSD systems in two
settings (1) a WSD task for Unsupervised WSD and WSI systems, (2) a clustering
comparison setting that evaluates the similarity of the sense inventories for
WSI systems.  Participants are presented examples contexts of each word and
asked to label each usage with as many senses as they think are applicable,
along with numeric weights denoting the relative levels of applicability.


TRAINING DATA:

Because the task is focused on unsupervised approaches for both WSI and WSD, no
training data is provided.  However, WSI systems will use a common corpus, the
ukWaC, to build their sense inventories.  The ukWaC is a 2-billion word
web-gathered corpus, which has also been released in POS-tagged and dependency
parsed formats. The corpus is available for download from the WaCky group here:

http://wacky.sslmit.unibo.it/

Participants may select their data from some or all of the ukWaC.  Furthermore,
unlike in previous WSI tasks, we will allow participants to use additional
training contexts not found in the ukWaC under the condition that they submit
systems for both using only the ukWaC and with their augmented corpora. This
option is designed for evaluating the impact of specialized corpora for
improving the quality of the induced senses.


TESTING DATA:

Testing data is provided in two formats, both containing identical data.

XML FORMAT:

Each of the contexts for a test term is contained in separate XML file.  The XML
are formatted as follows:

<instances lemma="win" partOfSpeech="v">
  <instance id="win.v.1" lemma="win" partOfSpeech="v" token="win" tokenEnd="21" tokenStart="18">instance text<</instance>
  ...
</instances>

Instance attributes are defined as follows:

-  "id" is the particular ID associated with that test and is used
  to report the senses
- "lemma" is the lemmatized target term
- "pos" is the part of speech of the target term
- "token" is the lexical form of the target term as it appears in the text itself
- "tokenStart" and "tokenEnd" indicate the sentence position of the token for
  the target term in the sentence.

Instances are drawn from both written, spoken, and web-based text, and therefore
may include a variety of sentence structures (or even be fragments).  

SENSEVAL2 FORMAT:

In keeping with previous tasks, the test data is also provided with all contexts
in a single XML file.  Target terms are represented as <lexelt> nodes that have
child nodes for each of their instances.  One token in the context is marked
with the <head> element, indicating the word should be disambiguated.  An
example is as follows:

<corpus lang="english">
  <lexelt item="become.v">
    <instance id="become.v.1">
      <context>It's no wonder that Wolf has  <head>become</head>  a well-paid political consultant.</context>
    </instance>


TESTING:

Using either the WordNet 3.1 sense inventory or an induced sense inventory,
participants must annotate each instance of a target word with one or more of
their senses, and optionally with those senses' applicabilities.

The annotation key will use the traditional key format used in prior Senseval
and SemEval WSD tasks (details here: http://www.senseval.org/senseval3/scoring).
Each line is the annotation for a particular instance, formatted as:

lemma.partOfSpeech instance-id sense-name/applicability-rating

For example, a rating might appear as:

win.v win.v.instance.1 win.v.1/1.0 win.v.2/4.7

Unsupervised systems should use the WordNet numbering convention for their
senses such that the first reported sense for a lemma is labeled as sense 1,
e.g., win.v.1 or win.v#1 (either is acceptable).

Sense induction systems may use a naming convention of their choice for their
senses so long as each sense has a unique label that does not contain the '/'
character.

The sense applicability ratings may be any positive value.  All ratings will be
normalized so the maximum value is 1, indicating completely applicable.  Senses
without ratings are assumed to have maximum applicability.


TEST DATA:

The Task 13 was based on a sample of instances from the Open American National
Corpus (http://www.americannationalcorpus.org/OANC/index.html). Target lemmas
were first identified and then instances were sampled from both the written and
spoken portions of the corpus. Due to the frequency with which the lemmas were
used, obtaining a fully balanced corpus between spoken and written was not
possible; however, all lemmas have at least four instances from the spoken
format.

The dataset initially distributed with the SemEval test data contained a small
minority of instances that were removed due to being invalid examples of the
lemma. A manual analysis of all instances revealed instances that were invalid
due being the wrong part of speech, being a part of collocation, or not having
any applicable WordNet 3.1 sense. These instances were removed from the data
distribution after the task, but the instance numbering remains the same, which
results in occasional gaps in the numbers.

The Task 13 test set contains annotations for 4664 instances. Of those, 517 were
annotated with two senses (11%) and 25 were annotated with three sense
(0.5%). This low percentage of multiple-annotations is in stark contrast with
the trial data from the GWS dataset of Erk et al. (2009), which featured
multiple annotations on every instance. A re-analysis of their dataset by
trained lexicographers revealed that annotators were often mistaken regarding
the specific application of senses and were therefore more likely to rate in
applicable senses as applicable. The Task 13 test data adopted a conservative
sense annotation approach that involved making sense applicability judgments
based on all available information and examples in WordNet 3.1, such as sentence
frames, coordinate terms, antonyms, etc., which were not available to the
annotators for the trial data.


EVALUATION:

Task 13 uses two types of evaluation metrics:

  (1) a traditional WSD evaluation involving direct comparison between WordNet
      3.1 sense labels and

  (2) cluster-based evaluations for comparing induced sense inventories to the
      WordNet 3.1 inventories

All code and scripts for performing the evaluation are released open source at
https://code.google.com/p/cluster-comparison-tools/


The WSD evaluation uses three different scoring metrics:

 - the Jaccard Index, which measures the agreement in which sense labels are
   used on an instance

 - positionally-weighted Kendall's tau, which measures the agreement in ranking
   an instance's sense labels according to their applicability

 - a weighted variant of Normalized Discounted Cumulative Gain, which measures
   agreement in sense applicability ratings

Each metric has a separate evaluation script.  Because WSI approaches use a
different sense inventory, the scoring scripts will perform a remapping
procedure that converts a sense labeling in the induced inventory to one for
WordNet 3.1.  The process follows previous SemEval tasks and uses 80% of the
instances to learn the sense-mapping and then tests the agreement on the
remaining 20%.  This 80/20 test/train process is repeated to provide a score for
the entire data set. 

Unsupervised WSD approaches that directly use WordNet 3.1 should provide the
"--no-remapping" flag to the evaluation scripts to indicate that such an
evaluation is not necessary.


Cluster-based evaluations use two metrics:

 - Fuzzy Normalized Mutual Information, which measures agreement between the two
   sense clusters at the sense level
 
 - Fuzzy B-Cubed, which measures agreement at the instance level.

Cluster-based evaluation are designed to measure the agreement between the
induced sense inventory and the WordNet 3.1 inventory and therefore the measures
are only officially reported for WSI systems.


BASELINES:

Task 13 includes six total baselines: (1) three solutions that directly use the
WordNet 3.1 sense inventory and (2) three solutions that use induced senses.

 - Most Frequent Sense (mfs.wn.key) labels each instance with a single sense, using the sense
   that is most frequently seen for that word in the text (regardless of what
   applicability rating it was given)

 - All Senses, Equally Weighted (all-senses.wn.key) labels each instance with
   all senses

 - All Senses, Average Weighted (all-senses.avg-rating.wn.key) labels each
   instance with all senses, rating each sense with its average applicability
   rating from the gold standard labeling

 - 1 of 2 random senses (random.2-senses.induced.key) labels each instance one
   of two random induced senses, which are then mapped to WordNet 3.1 senses
   using the Task's sense mapping procedure.

 - 1 of 3 random senses (random.3-senses.induced.key) labels each instance one
   of three random induced senses, which are then mapped to WordNet 3.1 senses
   using the Task's sense mapping procedure.

 - 1 of n random senses (random.n-senses.induced.key) labels each instance one
   of n random induced senses, where n is the true number of senses for that
   word. These induced senses are then mapped to WordNet 3.1 senses using the
   Task's sense mapping procedure.

When evaluating these and other baselines, sense keys that are already use the
WordNet 3.1 sense inventory (denoted above with ".wn." in the file name) should
use the "--no-remapping" flag with the supervised command line programs.


DIRECTORY LAYOUT AND FILE DESCRIPTIONS:

contexts/ - A directory containing .xml files for each of the target lemmas.
            Each file contains a set of instances to be annotated.  Note that in
            the 2.0 release, some instances in the XML files are excluded from
            the key files (see note above)

keys/gold/ - The directory with gold standard keys

keys/gold/all.key - The sense annotation key with all instances.  This is the
                    key used for all official SemEval-2013 scoring

keys/gold/{adjectives,nouns,verbs}.key - Three separate keys for each of the
                                         parts of speech used in Task 13

keys/gold/{spoken,written}.key - Two separate keys for separating whether the
                                 instances were obtained from spoken or written
                                 text.

keys/baselines/ - The directory for all official baseline solutions

scoring/ - The directory containing executable .jar files for each evaluation


CONTACT:

For questions, comments, or bug reports, please contact the SemEval-2013 Google
groups: semeval-2013-task-13@groups.google.com

For specific questions, please contact the organizers

David Jurgens - jurgens@di.uniroma1.it
Ioannis P. Klapaftis - klapaftis@outlook.com


VERSIONS:

1.0 - Initial release of test data and README
2.0 - Release of solutions and baselines
2.1 - Release of senseval2 format

===============================================================================
