import argparse
from pandas import read_csv
from plumbum.cmd import cut, sed


##############################################
# to be imported from:
# from twsi_evaluation import get_best_id
from collections import Counter
from traceback import format_exc

def get_best_id(predict_sense_ids, sep=","):
    """ Converts a string '1:0.9, 2:0.1' to '1', or just keeps the simple format the same e.g. '1' -> '1'. """

    try:
        ids = predict_sense_ids.split(sep)
        scores = Counter()
        for s in ids:
            p = s.split(":")
            label = p[0]
            conf = float(p[1]) if len(p) == 2 else 1.0
            scores[label] = conf
        major_label = scores.most_common(1)[0][0]

        return major_label
    except:
        print predict_sense_ids
        print format_exc()
        return "-1"
######################################


def many2nine(csv_fpath, output_fpath):
    cut_9cols = (cut["-f", "1-9"] > output_fpath)
    cut_9cols(csv_fpath)
    print "file with 9 columns:", output_fpath


def doublespace2comma(csv_fpath, output_fpath):
    replace = (sed["s/  /,/g"] > output_fpath)
    replace(csv_fpath)
    print "comma-separated file with 9 columns:", output_fpath


def evaluate(lexsample_clasified_fpath):
    names=["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids","golden_related","predict_related","context"]

    df = read_csv(lexsample_clasified_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False,
                  header=None, names=names, low_memory=False)

    df["predict_sense_ids_best"] = df.predict_sense_ids.apply(lambda x: get_best_id(x, sep="  "))
    df.gold_sense_ids = df.gold_sense_ids.astype(unicode)
    df.predict_sense_ids_best = df.predict_sense_ids_best.astype(unicode)

    print "Accuracy: %.3f" % (sum(df.gold_sense_ids == df.predict_sense_ids_best) / float(len(df)))
    correct = float(sum(df.predict_sense_ids_best == df.gold_sense_ids))
    total = float(sum(df.predict_sense_ids_best != "-1"))
    print "Precision: %.3f == %d/%d" % (correct/total, correct, total)
    print "Recall: %.3f == %d/%d" % (correct/len(df), correct, len(df))
    print "Wrong:", sum(df.gold_sense_ids != df.predict_sense_ids_best)
    for i, row in  df.iterrows():
        if row.gold_sense_ids != row.predict_sense_ids_best:
            print "\n>>>", row.context_id, row.target, row.gold_sense_ids, row.predict_sense_ids_best, row.context


def main():
    parser = argparse.ArgumentParser(description='Evaluate directly i.e. without sense mapping.')
    parser.add_argument('lexsample', help='Lexical sample file with features and predictions (9 columns or more, no header)')
    args = parser.parse_args()

    print "Lexical sample file:", args.lexsample
    print ""

    nine_cols_fpath = args.lexsample + "-9cols"
    many2nine(args.lexsample, nine_cols_fpath)
    doublespace2comma(nine_cols_fpath)
    evaluate(nine_cols_fpath)


if __name__ == '__main__':
    main()

