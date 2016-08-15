import twsi_eval
import argparse
from pandas import read_csv
from twsi_eval import TWSI_INVENTORY, map_sense_inventories, calculate_evaluation_scores


TWSI_DATASET = 'data/Dataset-TWSI-2.csv'


def evaluate_uppper_bound(twsi_dataset_fath, user2twsi):
    print 'Estimating upper bound performance: ', twsi_dataset_fath
    correct = 0
    checked = set()
    predictions = read_csv(twsi_dataset_fath, sep='\t', encoding='utf8')
    i = -1

    for i, row in predictions.iterrows():
        context_id = row.context_id
        gold_sense_ids = unicode(row.gold_sense_ids)
        key = unicode(context_id) + row.target

        if key not in checked:
            checked.add(key)
            if gold_sense_ids in user2twsi[row.target].values():
                correct += 1

    return correct, i+1


def main():
    parser = argparse.ArgumentParser(description='Estimation of the upper bound performance given the custom Word Sense Inventory.')
    parser.add_argument('user_inventory', metavar='sense-inventory', help='word sense inventory file, format:\n word_senseID <tab> list,of,words')
    parser.add_argument('-predictions', help='word sense disambiguation predictions in the 9 column lexical sample format. By default use full Dataset-TWSI-2 set.', default=TWSI_DATASET)
    args = parser.parse_args()

    user2twsi = map_sense_inventories(TWSI_INVENTORY, args.user_inventory)
    correct, count = evaluate_uppper_bound(args.predictions, user2twsi)

    print "\nUpper Bound Results:"
    print "Correct, retrieved, nr_sentences"
    print correct, "\t", count
    precision, recall, fscore = calculate_evaluation_scores(correct=correct, retrieved=correct, itemcount=count)
    print "Precision:", precision, "\tRecall:", recall, "\tF1:", fscore


if __name__ == '__main__':
    main()
