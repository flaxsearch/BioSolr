from flask import Flask
from index import read
import json
import random
import sys

app = Flask(__name__)


@app.route('/')
def main():
  return json.dumps({ 'info': 'product offers API' })


@app.route('/products')
def products():
  offer = lambda doc: { 'id': doc['id'], 'discountPct': random.randint(1, 80) }
  return json.dumps([offer(doc) for doc in random.sample(app.docs, 64)])


@app.route('/manufacturers')
def manufacturer():
  manufacturers = set(doc['manufacturer'] for doc in app.docs if 'manufacturer' in doc)
  deal = lambda m: { 'id': m, 'discountPct': random.randint(1, 10) * 5 }
  return json.dumps([deal(m) for m in random.sample(manufacturers, 3)])


if __name__ == "__main__":
  app.docs = list(read(sys.argv[1]))

  app.run(port=8000, debug=True)
