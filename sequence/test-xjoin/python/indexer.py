import sys
import csv
import json
import requests

def value(k, v):
    return k, v.strip() if k != 'price' else float(v.split()[0])

def read(path):
    with open(path, encoding='iso-8859-1') as f:
        reader = csv.DictReader(f)
        for doc in reader:
            yield dict(value(k, v) for k, v in doc.items()
                       if len(v.strip()) > 0)

def index(url, docs):
    print("Sending {0} documents to {1}".format(len(docs), url))
    data = json.dumps(docs)
    headers = { 'content-type': 'application/json' }
    r = requests.post(url, data=data, headers=headers)
    if r.status_code != 200:
      raise IOError(r.text)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: {0} <Solr update URL> <CSV file>".format(sys.argv[0]))
        sys.exit(1)

    docs = list(read(sys.argv[2]))
    index(sys.argv[1], docs)
