import codecs
from pandas import read_csv
import argparse


def convert_dataset2semevalkey(dataset_fpath, output_fpath, no_header=False):
    with codecs.open(output_fpath, "w", encoding="utf-8") as output:

        if no_header:
            df = read_csv(dataset_fpath, sep='\t', encoding='utf8', header=None,
            names=["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids",
                   "golden_related","predict_related","context"])
        else:
            df = read_csv(dataset_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False)

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

    convert_dataset2semevalkey(args.input, args.output, args.no_header)


if __name__ == '__main__':
    main()
