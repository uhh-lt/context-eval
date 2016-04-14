from pandas import read_csv
import argparse
import codecs
from eval_lib import get_best_id, format_lexsample, FIELD_NAMES, FIELD_TYPES, LIST_SEP


SEMEVAL_SEP = " "

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
            predicted_senses = get_best_id(unicode(row.predict_sense_ids))
            print >> output, "%s %s %s" % (row.target + "." + row.target_pos, row.context_id, predicted_senses)

    print "Key file:", output_fpath


def main():
    parser = argparse.ArgumentParser(description='Convert lexical sample dataset to SemEval 2013 key format.')
    parser.add_argument('input', help='Path to a file with input lexical sample CSV file (9 columns or more).')
    parser.add_argument('output', help='Output file: a SemEval key file with the sense predictions.')
    parser.add_argument('--no_header', action='store_true', help='No headers. Default -- false.')
    args = parser.parse_args()
    print "Input: ", args.input
    print "Output: ", args.output
    print "No header:", args.no_header

    lexsample_9cols_fpath = format_lexsample(args.input)
    convert_dataset2semevalkey(lexsample_9cols_fpath, args.output, args.no_header)


if __name__ == '__main__':
    main()
