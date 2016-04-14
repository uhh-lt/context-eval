import argparse
from pandas import read_csv
from eval_lib import get_best_id, many2nine, doublespace2comma


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

