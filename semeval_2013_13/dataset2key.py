import codecs
from pandas import read_csv
import argparse
import numpy as np
import codecs
import os

FIELD_NAMES = ["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids", "golden_related","predict_related","context"]
FIELD_TYPES = {"context_id":np.dtype(str),"target":np.dtype(str),"target_pos":np.dtype(str),"target_position":np.dtype(str),"gold_sense_ids":np.dtype(str),"predict_sense_ids":np.dtype(str), "golden_related":np.dtype(str),"predict_related":np.dtype(str),"context":np.dtype(str)}


def cut_9_first(dataset_fpath, dataset_9_fpath):
    """ Cuts first 9 columns of the dataset file to make it openable with read_csv. """
    
    with codecs.open(dataset_fpath, "r", "utf-8") as in_dataset, codecs.open(dataset_9_fpath, "w", "utf-8") as out_dataset:
        for line in in_dataset: print >> out_dataset, "\t".join(line.split("\t")[:9])


def convert_dataset2semevalkey(dataset_fpath, output_fpath, no_header=False):
    with codecs.open(output_fpath, "w", encoding="utf-8") as output:

        if no_header:
            df = read_csv(dataset_fpath, sep='\t', encoding='utf8', header=None, names=FIELD_NAMES,
                    dtype=FIELD_TYPES, doublequote=False, quotechar='\0')
            df.target = df.target.astype(str)
        else:
            df = read_csv(dataset_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False, 
                    doublequote=False, quotechar='\0')

        for i, row in df.iterrows():
            predicted_senses = " ".join(unicode(row.predict_sense_ids).split(","))
            print >> output, "%s %s %s" % (row.target + "." + row.target_pos, row.context_id, predicted_senses)

    print "Key file:", output_fpath


def main():
    parser = argparse.ArgumentParser(description='Convert lexical sample dataset to SemEval 2013 key format.')
    parser.add_argument('input', help='Path to a file with input file.')
    parser.add_argument('output', help='Output file.')
    parser.add_argument('--no_header', action='store_true', help='No headers. Default -- false.')
    args = parser.parse_args()
    print "Input: ", args.input
    print "Output: ", args.output
    print "No header:", args.no_header

    tmp_fpath = args.input + "-9-columns.csv"
    cut_9_first(args.input, tmp_fpath) 
    convert_dataset2semevalkey(tmp_fpath, args.output, args.no_header)
    #os.remove(tmp_fpath)


if __name__ == '__main__':
    main()
