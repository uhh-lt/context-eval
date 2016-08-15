from collections import Counter
from traceback import format_exc
from plumbum.cmd import cut, sed
import numpy as np
import codecs
import os.path


LIST_SEP = ','
SCORE_SEP = ':'
FIELD_NAMES = ["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids", "golden_related","predict_related","context"]
FIELD_TYPES = {"context_id":np.dtype(str),"target":np.dtype(str),"target_pos":np.dtype(str),"target_position":np.dtype(str),"gold_sense_ids":np.dtype(str),"predict_sense_ids":np.dtype(str), "golden_related":np.dtype(str),"predict_related":np.dtype(str),"context":np.dtype(str)}


def format_lexsample(lexsample_fpath):
    extension = os.path.splitext(lexsample_fpath)[1]
    if extension == ".csv" or extension == "csv":
        lexsample_9cols_fpath = lexsample_fpath + "-9cols.csv"
        tmp_fpath = lexsample_9cols_fpath + ".tmp"
        many2nine(lexsample_fpath, tmp_fpath)
        doublespace2comma(tmp_fpath, lexsample_9cols_fpath)
    else:
        # if .gz do not format formatting
        lexsample_9cols_fpath = lexsample_fpath

    return lexsample_9cols_fpath


def many2nine(csv_fpath, output_fpath):
    cut_9cols = (cut["-f", "1-9"] > output_fpath)
    cut_9cols(csv_fpath)
    print "file with 9 columns:", output_fpath


def many2nine_alternative(dataset_fpath, dataset_9_fpath):
    """ Cuts first 9 columns of the dataset file to make it openable with read_csv. """

    with codecs.open(dataset_fpath, "r", "utf-8") as in_dataset, codecs.open(dataset_9_fpath, "w", "utf-8") as out_dataset:
        for line in in_dataset: print >> out_dataset, "\t".join(line.split("\t")[:9])


def doublespace2comma(csv_fpath, output_fpath):
    replace = (sed["s/  /,/g"] > output_fpath)
    replace(csv_fpath)
    print "comma-separated file with 9 columns:", output_fpath


def get_best_id(predict_sense_ids, sep=LIST_SEP, output_score=False):
    """ Converts a string '1:0.9, 2:0.1' to '1', or just keeps the simple format the same e.g. '1' -> '1'. """

    try:
        ids = predict_sense_ids.split(sep)
        scores = Counter()
        for s in ids:
            p = s.split(SCORE_SEP)
            label = p[0]
            conf = float(p[1]) if len(p) == 2 else 1.0
            scores[label] = conf
        major_label = scores.most_common(1)[0][0]
        if output_score:
            return major_label + SCORE_SEP + unicode(scores[major_label])
        else:
            return major_label
    except:
        print predict_sense_ids
        print format_exc()
        return "-1"

