import sys
import csv
import json
import requests
import codecs


class UTF8Recoder:
  """
  Iterator that reads an encoded stream and reencodes the input to UTF-8
  """
  def __init__(self, f, encoding):
    self.reader = codecs.getreader(encoding)(f)

  def __iter__(self):
    return self

  def next(self):
    return self.reader.next().encode("utf-8")


def read(path):
  with open(path) as f:
    reader = csv.DictReader(UTF8Recoder(f, 'iso-8859-1'))
    for doc in reader:
      doc = dict((k, v.strip() if k != 'price' else float(v.split()[0])) for k, v in doc.iteritems())
      doc = dict((k, v) for k, v in doc.iteritems() if v)
      yield doc


def index(docs):
  print "Sending {0} documents to SOLR".format(len(docs))
  r = requests.post(sys.argv[1], data=json.dumps(docs), headers={ 'content-type': 'application/json' })
  if r.status_code != 200:
    raise IOError("bad batch")


if __name__ == "__main__":
  if len(sys.argv) < 4:
    print "Usage: {0} <solr update URL> <csv file> <batch size>".format(sys.argv[0])
    sys.exit(1)

  docs = []
  for doc in read(sys.argv[2]):
    docs.append(doc)
    if len(docs) == int(sys.argv[3]):
      index(docs)
      docs = []
  index(docs)

