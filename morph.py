from spacy.en import English
from pandas import read_csv


STOPWORDS = "data/stopwords.csv"

def load_stoplist(fpath):
    word_df = read_csv(fpath, sep="\t", quotechar=u"\0",doublequote=False,  encoding='utf8', error_bad_lines=False)
    voc = set(row["word"] for i, row in word_df.iterrows())
    print "loaded %d stopwords: %s" % (len(voc), fpath)
    return voc


print "Loading spacy model..."
_stopwords = load_stoplist(STOPWORDS)
_spacy = English()


def is_stopword(word):
    return word in _stopwords


def get_stoplist():
    return _stopwords


def tokenize(text, lowercase=False, remove_stopwords=False):
    """ Return lemmatized text """

    lemmas = []
    for t in _spacy(text, tag=True, parse=False, entity=True):
        if t.lemma_ != t.orth_:
            lemmas.append(t.lemma_)

    if remove_stopwords: lemmas = [l for l in lemmas if l not in _stopwords and len(l) > 1 and "-" not in l]
    if lowercase: lemmas = [l.lower() for l in lemmas]

    return lemmas


def analyze_word(word, lowercase=True):
    tokens = _spacy(word, tag=True, parse=False, entity=False)
    lemma = tokens[0].lemma_
    if lowercase: lemma = lemma.lower()
    return lemma, tokens[0].pos_
