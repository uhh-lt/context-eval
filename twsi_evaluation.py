#!/usr/bin/env python

import argparse
from os import listdir
from os.path import isfile, join, walk
from math import sqrt
import re
from pandas import read_csv


d = False
sense_file = "sense_inventory"
prediction_file = "predictions"

SEP = "_"
VALSEP = ':'

TWSI_PATH = "./TWSI2_complete/"
SUBST_PATH = "substitutions/raw_data/all-substitutions/"
SENT_PATH = "contexts/"
ASSIGNED_SENSES_FILE = "./data/Senses-TWSI-2.csv"

twsi_subst = dict()
sense_mappings = dict()
gold_labels = dict()
predictions = dict()
assigned_senses = dict()


class TwsiSubst:
    def __init__(self,word):
    	# target word
        self.word = word
        # related terms (with count) by sense id
        self.terms = {}
        # substitution counts by sense id
        self.scores = dict()
    
    def __eq__(self,other):
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
    	if not self.terms.has_key(num):
    	    self.terms[num] = dict()
    	    self.scores[num] = 0
    	if self.terms[num].has_key(term):
    	    self.terms[num][term] = int(self.terms[num][term]) + int(count)
    	else:
    	    self.terms[num][term] = int(count)
        self.scores[num] += int(count)
        
    # list all sense ids
    def getSenseIds(self):
        return self.terms.keys()
    
    # does this sense id exist?
    def hasSenseId(self, num):
        return (num in self.scores)


""" 	loads all the senses which were assigned in TWSI 2.0
	assigned senses are stored in provided file
	list of senses is used to remove all other senses, since they are impossible substitutions for the TWSI task 
"""
def load_assigned_senses():
    print "Loading assigned TWSI senses..."
    global assigned_senses
    assigned_senses = set(line.strip() for line in open(ASSIGNED_SENSES_FILE))
    print "Loading done\n"


"""	loads all TWSI 2.0 senses from the TWSI dataset folder
	filters senses by removing senses which do not occur in the TWSI data
"""
def load_twsi_senses():
    print "Loading TWSI sense inventory..."
    twsi_subst_p = join(TWSI_PATH,SUBST_PATH)
    files = [ f for f in listdir(twsi_subst_p) if isfile(join(twsi_subst_p,f)) ]
    files_s = files.sort()
    omitted = set()
    for f in files:
    	word = f.split('.')[0]
    	if d:
    	    print (word+'...'),
        substitutions = read_csv(join(twsi_subst_p,f), '/\t+/', encoding='utf8', header=None)
        # create new TwsiSubst for the given word
        t_s = TwsiSubst(word)
        for i,s in substitutions.iterrows():
            twsi_sense, w, subst, count = s[0].split('\t')
            if twsi_sense not in assigned_senses:
            	if d and not re.match('.*\-$', twsi_sense) and twsi_sense not in omitted:
            	    print "\nomitting TWSI sense "+twsi_sense+" as it did not occur in the sentences"
            	    omitted.add(twsi_sense)
                continue
            num = twsi_sense.split('@@')[1]
            if num == '-' or re.match('\-\-', subst) or re.match('^\-', subst):
                continue
            t_s.addTerm(num,subst,count)
        # add TWSI subst to dictionary
        twsi_subst[word] = t_s
    print "\nLoading done\n"
    

""" 	loads custom sense inventory
	performs alignment using cosine similarity
"""
def load_sense_inventory(filename):
    print "Loading provided Sense Inventory "+filename+"..."
    inventory = read_csv(filename, '/\t+/', encoding='utf8', header=None)
    for r,inv in inventory.iterrows():
        word, ident, terms = inv[0].split('\t')
        if word in twsi_subst:
            if d:
                print "\nSENSE: "+word+" "+ident
            twsi = twsi_subst.get(word)
            word_vec = dict()
            # split sense cluster into elements
            for el in set(terms.split(',')):
            	el = el.strip()
            	# split element into word and score
            	el_split = el.rsplit(VALSEP, 1)
            	word = el_split[0]
            	if len(el_split) > 1 and not re.match('\D+', el_split[1]):
            	    if word in word_vec:
            	        word_vec[word] += float(el_split[1])
            	    else:
            	        word_vec[word] = float(el_split[1])
            	else:
            	    word_vec[word] = 1.0
            	    
            # matching terms to TWSI sense ids
            scores = dict()
            for i in twsi.getSenseIds():
                scores[i] = calculate_cosine(twsi.getSubst(i), word_vec)
                if d:
                    print "Score for ",i,":", scores[i]
               
            # assignment
            assigned_id = get_max_score(scores)
            sense_mappings[ident] = assigned_id
            if d:
                print "SCORES: "+str(scores)
                print "ASSIGNED ID: "+word+" "+ident+"\t"+str(assigned_id)
    print "\nLoading done\n"


def load_gold_labels():
    print "Loading Gold Labels..."
    sent_files = []
    sent_p = join(TWSI_PATH,SENT_PATH)
    files = [ f for f in listdir(sent_p) if isfile(join(sent_p,f)) and not "DS" in f]
    files.sort()
    omitted = set()
    for f in files:
    	if d:
    	    print (f.split('.')[0]+'...'),
        sentences = read_csv(join(sent_p,f), '/\t+/', encoding='utf8', header=None)
        word = f.split('.')[0]        
        for i,s in sentences.iterrows():
            sen = s[0].split('\t')
            twsi_sense = sen[0]
            twsi_num = twsi_sense.split('@@')[1]
            word = sen[1]
            if not twsi_subst.get(word).hasSenseId(twsi_num):
                if d and not twsi_sense in omitted:
            	    print "\nNo TWSI sense id '" + twsi_sense + "' in the inventory... Skipping sentences."            	    
            	    omitted.add(twsi_sense)
                continue
            ident = str(sen[3]) + str(sen[1])
            gold_labels[word+ident] = twsi_sense
            
    print "\nLoading done\n"

def evaluate_predicted_labels(filename):
    print "Evaluating Predicted Labels "+filename+"..."
    correct = 0
    retrieved = 0
    checked = set()
    predictions = read_csv(filename, '/\t+/', encoding='utf8', header=None)
    for i,p in predictions.iterrows():
         pred = p[0].split('\t')
         key = str(pred[0]) + str(pred[1])
         oracle = pred[2]
         if not SEP  in oracle:
             print "Sentence "+pred[0]+": Key '"+oracle+"' does not contain separator "+SEP + ". Skipping\n"
             continue
         oracle_p = oracle.split(SEP)[1]
         if oracle_p == "":
             print "Sentence "+pred[0]+": Key '"+oracle+"' without sense assignment\n"
             oracle_p = -1
         if gold_labels.has_key(key) and key not in checked:
             twsi_id = gold_labels[key].split('@@')[1]
             if oracle in sense_mappings and twsi_id == sense_mappings[oracle]:
                 correct += 1
             if int(oracle_p) > -1: 
                 retrieved += 1
             if d:  
             	 if oracle in sense_mappings:
             	     print "Sentence: "+key+"\tPrediction: "+oracle+"\tGold: "+gold_labels[key]+"\tPredicted_TWSI_sense: "+str(sense_mappings[oracle])+"\tMatch:"+str(gold_labels[key].split('@@')[1] == sense_mappings[oracle])
             	 else:
             	     print "Sentence: "+key+"\tPrediction: "+oracle+"\tGold: "+gold_labels[key]+"\tPredicted_TWSI_sense: "+"none"+"\tMatch: False"
             checked.add(key)   	     
         elif d:
             print "Sentence not in gold data: "+key+" ... Skipping sentence for evaluation."
    print "\nEvaluation done\n"
    return correct, retrieved



def get_max_score(scores):
    max_value = 0
    max_id = -1
    for i in scores.keys():
        if scores[i] > max_value:
            max_value = scores[i]
            max_id = i
    return max_id


def calculate_evaluation_scores(correct, retrieved, eval_retrieved = False):
    if eval_retrieved:
        nr_sentences = retrieved
    else:
        nr_sentences = len(gold_labels)
    precision = 0
    recall = 0
    if retrieved == 0:
        print "No predictions were retrieved!"
    else:
        precision = float(correct) / retrieved
    
    if nr_sentences == 0:
        print "No Gold labels, check TWSI path!"
    else:
        recall = float(correct) / nr_sentences
    fscore = 2 * precision * recall / (precision + recall)
    return precision, recall, fscore
    
        

""" computes cosine similarity between two vectors
"""
def calculate_cosine(v1, v2):
    score = 0
    len1 = 0
    len2 = 0
    for w in v1.keys():
        if w in v2.keys():
            if d:
                print "Element:",w, v1[w], v2[w]
            score += v1[w] * v2[w]
        len1 += pow(v1[w],2)
    for w in v2.keys():
        len2 += pow(v2[w],2)
    l1 = sqrt(len1)
    l2 = sqrt(len2)
    if l1 > 0 and l2 > 0:
        return score / (l1 * l2)
    return 0
    
    
""" computes the purity of a clustering
"""
def calculate_purity_clustering(c1, c2):
    # for clustering c1:
    	# get cluster
    	# calcucate purity(v1, v2)
    return 0
    	
def calculate_purity(v1, v2):
    #
    return 0
    



def main():
    global SEP, TWSI_PATH, d
    parser = argparse.ArgumentParser(description='Evaluation script for contextualizations with a custom Word Sense Inventory.')	
    parser.add_argument('sense_file', metavar='inventory', help='word sense inventory file, format:\n word_senseID <tab> list,of,words')
    parser.add_argument('predictions', help='word sense disambiguation predictions, format:\n sentenceID <tab> predicted-word_senseID')
    settings = parser.add_argument_group('Settings')
    settings.add_argument('-t', dest='TWSI_PATH', help='path to the TWSI2.0 folder (default: ./TWSI2_complete/)', required=False)
    settings.add_argument('-d', dest='debug', help='display debug output (default: False)', required=False)
    settings.add_argument('-sep', dest='sep', help='separator between word and senseID in the inventory file (default: "_")', required=False)
    args = parser.parse_args()
    
    if args.sep:
        SEP = args.sep
    if args.debug:
        d = args.debug
    if args.TWSI_PATH:               
        TWSI_PATH = args.TWSI_PATH	    
    
    load_assigned_senses()
    load_twsi_senses()
    load_sense_inventory(args.sense_file)
    load_gold_labels()
    correct, retrieved = evaluate_predicted_labels(args.predictions)
    
    print "\nEvaluation Results:"
    print "Correct, retrieved, nr_sentences"
    print correct, "\t", retrieved, "\t", len(gold_labels)
    precision, recall, fscore = calculate_evaluation_scores(correct, retrieved)
    print "Precision:",precision, "\tRecall:", recall, "\tF1:", fscore
    print "Coverage: ", float(retrieved)/len(gold_labels)
    

if __name__ == '__main__':
    main()

