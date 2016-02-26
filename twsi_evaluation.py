#!/usr/bin/env python

import argparse
from os.path import split
from math import sqrt
import re
from pandas import read_csv

DEBUG = False
SCORE_SEP = ':'
TWSI_ASSIGNED_SENSES = "data/AssignedSenses-TWSI-2.csv"
TWSI_INVENTORY = "data/Inventory-TWSI-2.csv"


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

    # get substitutions for sense id
    def getSubst(self, num):
        return self.terms[num]

    # get scores by sense id
    def getScores(self, num):
        return self.scores[num]

    # add a new substiution with score
    def addTerm(self, num, term, count):
        if num not in self.terms:
            self.terms[num] = {}
            self.scores[num] = 0
        if term in self.terms[num]:
            self.terms[num][term] = int(self.terms[num][term]) + int(count)
        else:
            self.terms[num][term] = int(count)
        self.scores[num] += int(count)

    # add a list of substitutions with score from string
    def addTerms(self, num, subs):
        subs_l = subs.split(',')
        for s in subs_l:
            s = s.strip()
            key, val = s.split(SCORE_SEP)
            self.addTerm(num, key, val)

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


def load_twsi_senses():
    """ loads all TWSI 2.0 senses from the TWSI dataset folder
    filters senses by removing senses which do not occur in the TWSI data """

    assigned_senses = load_assigned_senses(TWSI_ASSIGNED_SENSES)

    twsi_subst = {}

    print "Loading TWSI sense inventory..."
    # otherwise a warning was shown, that c engine cannot be used because c engine cannot work with pattern as separators (or smth like this)
    substitutions = read_csv(TWSI_INVENTORY, '/\t+/', encoding='utf8', header=None, engine="python")
    for i, s in substitutions.iterrows():
        # create new TwsiSubst for the given word
        word, t_id, subs = s[0].split('\t')
        t_s = twsi_subst.get(word)
        if t_s is None:
            t_s = TWSI(word)
        twsi_sense = word + "@@" + t_id
        if twsi_sense not in assigned_senses:
            if DEBUG:
                print "\nomitting TWSI sense " + twsi_sense + " as it did not occur in the sentences"
            continue
        t_s.addTerms(t_id, subs)
        twsi_subst[word] = t_s

    return twsi_subst


def map_sense_inventories(user_inventory_fpath):
    """ loads custom sense inventory performs alignment using cosine similarity """

    twsi_senses = load_twsi_senses()

    sense_mappings = {}

    print "Loading provided Sense Inventory " + user_inventory_fpath + "..."
    mapping_fpath = "data/Mapping_" + split(TWSI_INVENTORY)[1] + "_" + split(user_inventory_fpath)[1]
    mapping_file = open(mapping_fpath, 'w')
    user_inventory = read_csv(user_inventory_fpath, '/\t+/', encoding='utf8', header=None, engine="python")
    for _, row in user_inventory.iterrows():
        word, user_sense_id, cluster = row[0].split('\t')  # this is baaad :-)
        if word in twsi_senses:
            print >> mapping_file, "\n%s\n%s#%s: %s\n" % ("="*50, word, user_sense_id, cluster)
            if DEBUG:
                print "\nSENSE: " + word + " " + user_sense_id
            twsi = twsi_senses.get(word)
            word_vec = {}

            for cluster_word_entry in set(cluster.split(',')):
                try:
                    word2, score2 = cluster_word_entry.strip().rsplit(SCORE_SEP, 1)
                    if not re.match('\D+', score2):
                        if word2 in word_vec:
                            word_vec[word2] += float(score2)
                        else:
                            word_vec[word2] = float(score2)
                    else:
                        word_vec[word2] = 1.0
                except:
                    print "Warning: wrong cluster word", cluster_word_entry

            # matching terms to TWSI sense ids
            scores = {}
            for twsi_sense_id in twsi.getSenseIds():
                twsi_sense = twsi.getSubst(twsi_sense_id)
                scores[twsi_sense_id] = calculate_cosine(twsi_sense, word_vec)
                print >> mapping_file, "%s#%s (%.3f):\t" % (word, twsi_sense_id, scores[twsi_sense_id]),
                for key in twsi_sense.keys():
                    mapping_file.write(key + ":" + str(twsi_sense[key]) + ", ")
                print >> mapping_file, "\n"

            # assignment
            assigned_id = get_max_score(scores)
            sense_mappings[word + user_sense_id] = assigned_id
            print >> mapping_file, word + "#" + unicode(assigned_id), "\n"



    print "Mapping:" + mapping_fpath

    return sense_mappings


def evaluate_predicted_labels(_sense_mappings, lexsub_dataset_fpath, has_header=True):
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
            if row.target + predicted_sense_ids in _sense_mappings \
                    and gold_sense_ids == _sense_mappings[row.target + predicted_sense_ids]:
                correct += 1
            if int(float(predicted_sense_ids)) > -1:
                retrieved += 1
            if DEBUG:
                if row.target + predicted_sense_ids in _sense_mappings:
                    print "Sentence: " + key + "\tPrediction: " + predicted_sense_ids + \
                          "\tGold: " + key + \
                          "\tPredicted_TWSI_sense: " + str(_sense_mappings[row.target + predicted_sense_ids]) + \
                          "\tMatch:" + str(gold_sense_ids == _sense_mappings[row.target + predicted_sense_ids])
                else:
                    print "Sentence: " + key + "\tPrediction: " + predicted_sense_ids + \
                          "\tGold: " + key + \
                          "\tPredicted_TWSI_sense: " + "none" + \
                          "\tMatch: False"

        elif DEBUG:
            print "Sentence not in gold data: " + key + " ... Skipping sentence for evaluation."
    return correct, retrieved, i+1


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
    parser.add_argument('sense_file', metavar='inventory', help='word sense inventory file, format: "word<TAB>senseID<TAB>cluster", where cluster is a list of "word:score" separeted by ","')
    parser.add_argument('predictions', help='word sense disambiguation predictions in the 9 column lexical sample format.')
    parser.add_argument('--verbose', action='store_true', help='Display detailed information. Default -- false.')
    parser.add_argument('--no_header', action='store_true', help='No headers. Default -- false.')
    args = parser.parse_args()

    global DEBUG
    if args.verbose: DEBUG = args.verbose

    print "Sense inventory:", args.sense_file
    print "Lexical sample dataset:", args.predictions
    print "No header:", args.no_header
    print "Verbose:", args.verbose
    print ""

    sense_mappings = map_sense_inventories(args.sense_file)
    correct, retrieved, count = evaluate_predicted_labels(sense_mappings, args.predictions, has_header=(not args.no_header))

    print "\nEvaluation Results:"
    print "Correct, retrieved, nr_sentences"
    print correct, "\t", retrieved, "\t", count
    precision, recall, fscore = calculate_evaluation_scores(correct, retrieved, count)
    print "Precision:", precision, "\tRecall:", recall, "\tF1:", fscore
    print "Coverage: ", float(retrieved) / count


if __name__ == '__main__':
    main()
