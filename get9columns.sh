f=$1; cat $f/p* | cut -f 1-9 | grep -v "^context" > $f.csv; wc -l $f.csv
