from pandas import read_csv
from os.path import splitext
import codecs
import argparse

def ddt2inventory(ddt_fpath, output_fpath):

    df = read_csv(ddt_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False)

    with codecs.open(output_fpath, "w", "utf-8") as output:
        print >> output, "target\tsense_id\trelated"
        for i, row in df.iterrows(): 
            cluster = []
            for word_sense_sim in row.cluster.split(","):
                word_sense, sim = word_sense_sim.split(":")
                word, sense = word_sense.split("#")
                cluster.append("%s:%.3f" % (word, float(sim)))
            print >> output, "%s\t%s\t%s" % (row.word, unicode(row.cid), ",".join(cluster)) 

    
def main():
    parser = argparse.ArgumentParser(description='Convert a DDT to a word sense inventory format "target<TAB>sense_id<TAB>cluster".')
    parser.add_argument('ddt', help='Path to a csv file with a DDT: "word<TAB>cid<TAB>cluster<TAB>isas".')
    parser.add_argument('inv', help='Path to output inventory file')
    args = parser.parse_args()

    print "Input DDT: ", args.ddt
    print "Output word sense inventory:", args.inv

    ddt2inventory(args.ddt, args.inv)

if __name__ == '__main__':
    main()
