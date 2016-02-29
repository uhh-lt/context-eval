import twsi_evaluation
import argparse
from pandas import read_csv
from twsi_evaluation import *

TWSI_DATASET = 'data/Dataset-TWSI-2.csv'

def evaluate_uppper_bound(filename, mapped_twsi_senses):

    print 'Estimating upper bound performance: ' + filename + '...'
    correct = 0
    retrieved = 0
    checked = set()
    predictions = read_csv(filename, sep='\t', encoding='utf8')
    i = -1
    for i, row in predictions.iterrows():
        context_id = row.context_id
        gold_sense_ids = unicode(row.gold_sense_ids)

        key = unicode(context_id) + row.target

        if key not in checked:
            checked.add(key)
            if row.target + gold_sense_ids in mapped_twsi_senses:
                correct += 1
                retrieved += 1
    return correct, retrieved, i+1



def main():
    parser = argparse.ArgumentParser(
        description='Estimation of the upper bound performance given the custom Word Sense Inventory.')
    parser.add_argument('sense_file', metavar='sense-inventory',
                        help='word sense inventory file, format:\n word_senseID <tab> list,of,words')
    args = parser.parse_args()

    twsi_evaluation._assigned_senses = twsi_evaluation.load_assigned_senses(twsi_evaluation.TWSI_ASSIGNED_SENSES)
    twsi_evaluation._twsi_subst = twsi_evaluation.load_twsi_senses(twsi_evaluation.TWSI_INVENTORY)

    twsi_evaluation.load_sense_inventory(args.sense_file)
    correct, retrieved, count = evaluate_uppper_bound(TWSI_DATASET, twsi_evaluation._mapped_twsi_senses)


    print "\nUpper Bound Results:"
    print "Correct, retrieved, nr_sentences"
    print correct, "\t", retrieved, "\t", count
    precision, recall, fscore = twsi_evaluation.calculate_evaluation_scores(correct, retrieved, count)
    print "Precision:", precision, "\tRecall:", recall, "\tF1:", fscore


if __name__ == '__main__':
    main()