#!/usr/bin/env python

import argparse
from os import listdir
from os.path import isfile, join, walk
import re
from pandas import read_csv


d = False
sense_file = "sense_inventory"
prediction_file = "predictions"

SEP = "_"

TWSI_PATH = "./TWSI2_complete/"
SUBST_PATH = "substitutions/raw_data/all-substitutions/"
SENT_PATH = "contexts/"
ASSIGNED_SENSES_FILE = "./data/TWSI-2.0-assigned-senses.txt"

twsi_subst = dict()
sense_mappings = dict()
gold_labels = dict()
predictions = dict()
assigned_senses = dict()


class TwsiSubst:
    def __init__(self,word):
        self.word = word
        self.terms = {}
        self.scores = dict()
    
    def __eq__(self,other):
        return self.word == other
        
    def __hash__(self):
        return hash(self.word)
    
    def getSubst(self, num):
        return self.terms[num]
    
    def getScores(self, num):
        return self.scores[num]
    
    def addTerm(self, num, term, count):
    	if not self.terms.has_key(num):
    	    self.terms[num] = dict()
    	    self.scores[num] = 0
    	if self.terms[num].has_key(term):
    	    self.terms[num][term] = int(self.terms[num][term]) + int(count)
    	else:
    	    self.terms[num][term] = count
        self.scores[num] += int(count)
        
    def getSenseIds(self):
        return self.terms.keys()
       
    def hasSenseId(self, num):
        return (num in self.scores)



def load_assigned_senses():
    print "Loading assigned TWSI senses..."
    global assigned_senses
    assigned_senses = set(line.strip() for line in open(ASSIGNED_SENSES_FILE))
    print "\nLoading done\n"

def load_twsi_senses():
    print "Loading TWSI sense inventory..."
    subst_files = []
    twsi_subst_p = join(TWSI_PATH,SUBST_PATH)
    files = [ f for f in listdir(twsi_subst_p) if isfile(join(twsi_subst_p,f)) ]
    files_s = files.sort()
    for f in files:
    	print (f.split('.')[0]+'...'),
        substitutions = read_csv(join(twsi_subst_p,f), '/\t+/', encoding='utf8', error_bad_lines=False, warn_bad_lines=True, header=None)
        word = f.split('.')[0]
        t_s = TwsiSubst(word)
        for i,s in substitutions.iterrows():
            twsi_sense, w, subst, count = s[0].split('\t')
            if twsi_sense not in assigned_senses:
            	if d and not re.match('.*\-$', twsi_sense):
            	    print "removing TWSI sense "+twsi_sense+" as it did not occur in the sentences"
                continue
            num = twsi_sense.split('@@')[1]
            if num == '-' or re.match('\-\-', subst) or re.match('^\-', subst):
                continue
            t_s.addTerm(num,subst,count)
        twsi_subst[word] = t_s
    print "\nLoading done\n"
    

def load_sense_inventory(filename):
    print "Loading provided Sense Inventory "+filename+"..."
    mismatch = 0
    match = 0
    inventory = read_csv(filename, '/\t+/', encoding='utf8', error_bad_lines=False, warn_bad_lines=True, header=None)
    for r,inv in inventory.iterrows():
        ident, terms = inv[0].split('\t')
        word = ident.split(SEP)[0]
        if word in twsi_subst:
            if d:
                print "\nSENSE: "+ident
            else:
                print ident+"...",
            twsi = twsi_subst.get(word)
            scores = dict()
            for i in twsi.getSenseIds():
                scores[i] = 0
            for term in set(terms.split(',')):
            	term = term.strip()
                # matching terms to TWSI sense ids
                for i in twsi.getSenseIds():
                    subst = twsi.getSubst(i)
                    if subst.has_key(term):
                        scores[i] += float(subst[term]) / twsi.getScores(i)
                        if d:
                            print "MATCH: "+ident+"\tID: "+i+"\t"+term+"\t"+str(subst[term])+"\t"+str(float(subst[term]) / twsi.getScores(i))
            # assignment
            assigned_id = get_max_score(scores)
            sense_mappings[ident] = assigned_id
            if d:
                print "SCORES: "+str(scores)
                print "ASSIGNED ID: "+ident+"\t"+str(assigned_id)
            t_i = ident.split(SEP)[1]
            if not assigned_id == t_i:
                print "no match for "+ word + " between " + str(t_i) + " and " + str(assigned_id) + "\n"
                mismatch += 1
            else:
                match += 1
    print "\nLoading done\n"
    print "There have been " + str(mismatch) + " mismatches and " + str(match) + " matches!" 
     


def load_gold_labels():
    print "Loading Gold Labels..."
    sent_files = []
    sent_p = join(TWSI_PATH,SENT_PATH)
    files = [ f for f in listdir(sent_p) if isfile(join(sent_p,f)) and not "DS" in f]
    files.sort()
    for f in files:
    	print (f.split('.')[0]+'...'),
        sentences = read_csv(join(sent_p,f), '/\t+/', encoding='utf8', error_bad_lines=False, warn_bad_lines=True, header=None)
        word = f.split('.')[0]        
        for i,s in sentences.iterrows():
            sen = s[0].split('\t')
            twsi_sense = sen[0]
            twsi_num = twsi_sense.split('@@')[1]
            word = sen[1]
            if not twsi_subst.get(word).hasSenseId(twsi_num):
                if d:
            	    print "No TWSI sense id '" + twsi_sense + "' in the inventory... Skipping sentence."
                continue
            ident = str(sen[3]) + str(sen[1])
            gold_labels[ident] = twsi_sense
            
    print "\nLoading done\n"

def evaluate_predicted_labels(filename):
    print "Evaluating Predicted Labels "+filename+"..."
    correct = 0
    retrieved = 0
    checked = set()
    predictions = read_csv(filename, '/\t+/', encoding='utf8', error_bad_lines=False, warn_bad_lines=False, header=None)
    for i,p in predictions.iterrows():
         pred = p[0].split('\t')
         key = str(pred[0]) + str(pred[1])
         oracle = pred[2]
         oracle_p = oracle.split(SEP)[1]
         if gold_labels.has_key(key) and key not in checked:
             twsi_id = gold_labels[key].split('@@')[1]
             if oracle in sense_mappings and twsi_id == sense_mappings[oracle]:
                 correct += 1
             if oracle_p > -1: 
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


def calculate_scores(correct, retrieved):
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
    precision, recall, fscore = calculate_scores(correct, retrieved)
    print "Precision:",precision, "\tRecall:", recall, "\tF1:", fscore
    



if __name__ == '__main__':
    main()

