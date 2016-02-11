import argparse
from os.path import join
from jnt.common import ensure_dir
import ntpath
from os.path import splitext
from pandas import read_csv


names=["context_id","target","target_pos","target_position","gold_sense_ids","predict_sense_ids",
       "golden_related","predict_related","context","word_features","holing_features","target_holing_features",
       "conf","norm_conf","used_features"]

def breakdown(lexsample_fpath, output_dir): 
    ensure_dir(output_dir)
    df = read_csv(lexsample_fpath, encoding='utf-8', delimiter="\t", error_bad_lines=False,
                  header=None, names=names, low_memory=False)

    n = 0
    q_levels = [0.1*x for x in range(9, 0, -1)]
    q_values = [df.norm_conf.quantile(l) for l in q_levels]
    q = zip(q_levels, q_values)

    q_cur = q.pop()

    df = df.sort(["norm_conf"], ascending=1)
    name = splitext(ntpath.basename(lexsample_fpath))[0]

    for i, row in df.iterrows():
        if row.norm_conf >= q_cur[1]:
            output_fpath = join(output_dir, name + "-normconf" + unicode(int(100*(1.0-q_cur[0]))) + ".csv")
            print "Saving: %.3f %.3f %d %s" % (q_cur[0], q_cur[1], n, output_fpath)
            df.to_csv(output_fpath, sep="\t", encoding="utf-8", float_format='%.0f', index=False, header=False)
            
            if len(q) > 0: q_cur = q.pop()
            else: break
      
        if row.norm_conf < q_cur[1]:
           df.loc[i,"predict_sense_ids"] = -1
        
        if n % 5000 == 0: print n
        n += 1


def main():
    parser = argparse.ArgumentParser(description='Breakdown by confidence a lexical sample file.')
    parser.add_argument('lexsample', help='Lexical sample file with features and predictions (15 columns, no header)')
    parser.add_argument('output_dir', help='Output directory with the lexical sample files breked down w.r.t. confidence levels')
    args = parser.parse_args()

    print "Lexical sample file:", args.lexsample
    print "Output directory:", args.output_dir
    print ""

    breakdown(args.lexsample, args.output_dir)


if __name__ == '__main__':
    main()
