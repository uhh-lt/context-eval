import codecs
from pandas import read_csv
import argparse


def convert_dataset2semevalkey(dataset_fpath, output_fpath):
    with codecs.open(output_fpath, "w", encoding="utf-8") as output:
        df = read_csv(dataset_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False)
        
        for i, row in df.iterrows():
            predicted_senses = " ".join(unicode(row.predict_sense_ids).split(","))
            print >> output, "%s %s %s" % (row.target + "." + row.target_pos, row.context_id, predicted_senses)

    print "Key file:", output_fpath


def main():
    parser = argparse.ArgumentParser(description='Convert dataset to key.')
    parser.add_argument('input', help='Path to a file with input file.')
    parser.add_argument('output', help='Output file.')
    args = parser.parse_args()
    print "Input: ", args.input
    print "Output: ", args.output
    convert_dataset2semevalkey(args.input, args.output)


if __name__ == '__main__':
    main()
