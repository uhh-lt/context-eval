import argparse
from os.path import join
import ntpath
from os.path import splitext
from pandas import read_csv


lexsample_clasified_fpath = "/home/panchenko/spark/o6"


def evaluate(lexsample_clasified_fpath):
    names=["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids",
           "golden_related","predict_related","context","word_features","holing_features","target_holing_features",
           "conf","norm_conf","used_features","all_features"]
    df = read_csv(lexsample_clasified_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False,
                  header=None, names=names, low_memory=False)

    print "Accuracy: %.3f" % (sum(df.gold_sense_ids == df.predict_sense_ids) / float(len(df)))


def main():
    parser = argparse.ArgumentParser(description='Evaluate directly i.e. without sense mapping.')
    parser.add_argument('lexsample', help='Lexical sample file with features and predictions (15 columns, no header)')
    args = parser.parse_args()

    print "Lexical sample file:", args.lexsample
    print ""

    evaluate(args.lexsample)


if __name__ == '__main__':
    main()

