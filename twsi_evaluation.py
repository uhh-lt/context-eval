#!/usr/bin/env python

import argparse
from os.path import split
from math import sqrt
import re
from pandas import read_csv
from morph import is_stopword, tokenize
import codecs

DEBUG = False
LIST_SEP = ','
SCORE_SEP = ':'
TWSI_ASSIGNED_SENSES = "data/AssignedSenses-TWSI-2.csv"
TWSI_INVENTORY = "data/Inventory-TWSI-2.csv"
SPLIT_MWE=False

class TWSI:
    """ A class to store sense inventories """

    def __init__(self, word):
        # target word
        self.word = word
        # related terms (with count) by sense id
        self.terms = {}
        # substitution counts by sense id
        self.scores = {}

    def __eq__(self, other):
        return self.word == other

    def __hash__(self):
        return hash(self.word)


    def get_cluster(self, num):
        """ get substitutions for sense id """

        return self.terms[num]

    # get scores by sense id
    def getScores(self, num):
        return self.scores[num]

    def _add_term(self, num, term, count):
        """ add a new substiution with score """

        if num not in self.terms:
            self.terms[num] = {}
            self.scores[num] = 0
        if term in self.terms[num]:
            self.terms[num][term] = int(self.terms[num][term]) + int(count)
        else:
            self.terms[num][term] = int(count)
        self.scores[num] += int(count)

    def add_terms(self, sense_id, cluster):
        """ add a list of substitutions with score from string  """

        subs_l = cluster.split(LIST_SEP)
        for s in subs_l:
            s = s.strip()
            term, score = s.split(SCORE_SEP)
            self._add_term(sense_id, term, score)

            if SPLIT_MWE:
                words = term.split(" ")
                if len(words) == 1: continue
                for w in words:
                    if not is_stopword(w): self._add_term(sense_id, w, score)

                for w in tokenize(term, remove_stopwords=True):
                    self._add_term(sense_id, w, score)

    # list all sense ids
    def getSenseIds(self):
        return self.terms.keys()

    # does this sense id exist?
    def hasSenseId(self, num):
        return num in self.scores


def load_assigned_senses(assigned_senses_fpath):
    """ loads all the senses which were assigned in TWSI 2.0
     assigned senses are stored in provided file
     list of senses is used to remove all other senses, since they are impossible substitutions for the TWSI task """

    print "Loading assigned TWSI senses..."
    assigned_senses = set(line.strip() for line in open(assigned_senses_fpath))

    return assigned_senses


def load_twsi_senses(twsi_inventory_fpath, twsi_assigned_fpath=TWSI_ASSIGNED_SENSES):
    """ loads all TWSI 2.0 senses, filters senses by removing senses which do not occur in the TWSI data """

    assigned_senses = load_assigned_senses(twsi_assigned_fpath)

    twsi_senses = {}

    print "Loading TWSI sense inventory..."
    # otherwise a warning was shown, that c engine cannot be used because c engine cannot work with pattern as separators (or smth like this)
    substitutions = read_csv(twsi_inventory_fpath, sep="\t", encoding='utf8', header=None, names=["word","sense_id","cluster"])
    substitutions.sense_id = substitutions.sense_id.astype(unicode)

    for i, row in substitutions.iterrows():
        sense = twsi_senses.get(row.word)
        if sense is None: sense = TWSI(row.word)

        twsi_sense_id = row.word + "@@" + row.sense_id
        if twsi_sense_id not in assigned_senses:
            if DEBUG: print "\nomitting TWSI sense " + twsi_sense_id + " as it did not occur in the sentences"
            continue
        sense.add_terms(row.sense_id, row.cluster)
        twsi_senses[row.word] = sense

    return twsi_senses


def map_sense_inventories(twsi_inventory_fpath, user_inventory_fpath):
    """ loads custom sense inventory performs alignment using cosine similarity """

    twsi_senses = load_twsi_senses(twsi_inventory_fpath)

    user2twsi_mapping = {}

    print "Loading provided Sense Inventory " + user_inventory_fpath + "..."
    mapping_fpath = "data/Mapping_" + split(TWSI_INVENTORY)[1] + "_" + split(user_inventory_fpath)[1]
    with codecs.open(mapping_fpath, "w", "utf-8") as mapping_file:
        user_inventory = read_csv(user_inventory_fpath, sep="\t", encoding='utf8', header=None, names=["word","sense_id","cluster"])
        user_inventory.sense_id = user_inventory.sense_id.astype(unicode)

        for _, row in user_inventory.iterrows():
            if row.word in twsi_senses:
                print >> mapping_file, "\n%s\n%s#%s: %s\n" % ("="*50, row.word, row.sense_id, row.cluster)
                if DEBUG:
                    print "\nSENSE: " + row.word + " " + row.sense_id
                twsi = twsi_senses.get(row.word)

                user_cluster = {}
                for cluster_word_entry in set(row.cluster.split(',')):
                    try:
                        user_word, user_score = cluster_word_entry.strip().rsplit(SCORE_SEP, 1)
                        user_word = user_word.lower()
                        if not re.match('\D+', user_score):
                            if user_word in user_cluster: user_cluster[user_word] += float(user_score)
                            else: user_cluster[user_word] = float(user_score)
                        else:
                            user_cluster[user_word] = 1.0
                    except:
                        print "Warning: wrong cluster word", cluster_word_entry

                # matching terms to TWSI sense ids
                scores = {}
                for twsi_sense_id in twsi.getSenseIds():
                    twsi_cluster = twsi.get_cluster(twsi_sense_id)
                    scores[twsi_sense_id] = calculate_cosine(twsi_cluster, user_cluster)
                    print >> mapping_file, "%s#%s (%.3f):\t" % (row.word, twsi_sense_id, scores[twsi_sense_id]),
                    for key in twsi_cluster.keys():
                        mapping_file.write(key + ":" + str(twsi_cluster[key]) + ", ")
                    print >> mapping_file, "\n"

                # assignment
                assigned_twsi_sense_id = get_max_score(scores)
                user2twsi_mapping[row.word + row.sense_id] = assigned_twsi_sense_id
                print >> mapping_file, row.word + "#" + unicode(assigned_twsi_sense_id), "\n"
            else:
                print "Warning: skipping word not present in TWSI vocabulary:", row.word

    print "Mapping:", mapping_fpath

    return user2twsi_mapping


def evaluate_predicted_labels(user2twsi_mapping, lexsub_dataset_fpath, has_header=True):
    """ loads and evaluates the results """

    print "Evaluating Predicted Labels " + lexsub_dataset_fpath + "..."
    correct = 0
    retrieved = 0
    checked = set()

    if has_header:
        lexsub_dataset = read_csv(lexsub_dataset_fpath, sep='\t', encoding='utf8')
    else:
        lexsub_dataset = read_csv(lexsub_dataset_fpath, sep='\t', encoding='utf8', header=None,
            names=["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids",
                   "golden_related","predict_related","context"])

    i = -1
    for i, row in lexsub_dataset.iterrows():
        context_id = row.context_id
        gold_sense_ids = unicode(row.gold_sense_ids)
        if unicode(row.predict_sense_ids) == 'nan':
            print "Sentence " + unicode(context_id) + ": Key '" + row.target + "' without sense assignment"
            predicted_sense_ids = "-1"
        else:
            predicted_sense_ids = unicode(int(row.predict_sense_ids))
        key = unicode(context_id) + row.target

        if key not in checked:
            checked.add(key)
            if row.target + predicted_sense_ids in user2twsi_mapping and gold_sense_ids == user2twsi_mapping[row.target + predicted_sense_ids]:
                correct += 1
            if int(float(predicted_sense_ids)) > -1:
                retrieved += 1
            if DEBUG:
                if row.target + predicted_sense_ids in user2twsi_mapping:
                    print "Sentence: " + key + "\tPrediction: " + predicted_sense_ids + \
                          "\tGold: " + key + \
                          "\tPredicted_TWSI_sense: " + str(user2twsi_mapping[row.target + predicted_sense_ids]) + \
                          "\tMatch:" + str(gold_sense_ids == user2twsi_mapping[row.target + predicted_sense_ids])
                else:
                    print "Sentence: " + key + "\tPrediction: " + predicted_sense_ids + \
                          "\tGold: " + key + \
                          "\tPredicted_TWSI_sense: " + "none" + \
                          "\tMatch: False"

        elif DEBUG:
            print "Sentence not in gold data: " + key + " ... Skipping sentence for evaluation."
    return correct, retrieved, i + 1


def get_max_score(scores):
    """ gets maximum score from a dictionary """

    max_value = 0
    max_id = -1
    for i in scores.keys():
        if scores[i] > max_value:
            max_value = scores[i]
            max_id = i
    return max_id


def calculate_evaluation_scores(correct, retrieved, itemcount, eval_retrieved=False):
    """ computes precision, recall and fscore """

    if eval_retrieved:
        itemcount = retrieved
    precision = 0
    recall = 0
    fscore = 0
    if retrieved == 0:
        print "No predictions were retrieved!"
    else:
        precision = float(correct) / retrieved

    if itemcount == 0:
        print "No Gold labels, check TWSI path!"
    else:
        recall = float(correct) / itemcount

    if precision > 0 and recall > 0:
        fscore = 2 * precision * recall / (precision + recall)
    return precision, recall, fscore


def calculate_cosine(v1, v2):
    """ computes cosine similarity between two vectors """

    score = 0
    len1 = 0
    len2 = 0
    for w in v1.keys():
        if w in v2.keys():
            if DEBUG:
                print "Element:", w, v1[w], v2[w]
            score += v1[w] * v2[w]
        len1 += pow(v1[w], 2)
    for w in v2.keys():
        len2 += pow(v2[w], 2)
    l1 = sqrt(len1)
    l2 = sqrt(len2)
    if l1 > 0 and l2 > 0:
        return score / (l1 * l2)
    return 0


def main():
    parser = argparse.ArgumentParser(description='Evaluation script for contextualizations with a custom Word Sense Inventory.')
    parser.add_argument('user_inventory', metavar='inventory', help='word sense inventory file, format: "word<TAB>senseID<TAB>cluster", where cluster is a list of "word:score" separeted by ","')
    parser.add_argument('predictions', help='word sense disambiguation predictions in the 9 column lexical sample format.')
    parser.add_argument('--verbose', action='store_true', help='Display detailed information. Default -- false.')
    parser.add_argument('--no_header', action='store_true', help='No headers. Default -- false.')
    args = parser.parse_args()

    global DEBUG
    if args.verbose: DEBUG = args.verbose

    print "Sense inventory:", args.user_inventory
    print "Lexical sample dataset:", args.predictions
    print "No header:", args.no_header
    print "Verbose:", args.verbose
    print ""

    user2twsi_mapping = map_sense_inventories(TWSI_INVENTORY, args.user_inventory)
    correct, retrieved, count = evaluate_predicted_labels(user2twsi_mapping, args.predictions, has_header=(not args.no_header))

    print "\nEvaluation Results:"
    print "Correct, retrieved, nr_sentences"
    print correct, "\t", retrieved, "\t", count
    precision, recall, fscore = calculate_evaluation_scores(correct, retrieved, count)
    print "Precision:", precision, "\tRecall:", recall, "\tF1:", fscore
    print "Coverage: ", float(retrieved) / count


if __name__ == '__main__':
    main()
