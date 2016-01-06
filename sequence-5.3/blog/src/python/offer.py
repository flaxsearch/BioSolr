from flask import Flask
from index import read
import json
import random
import sys


app = Flask(__name__)


@app.route('/')
def main():
  return json.dumps({ 'info': 'product offers API' })


@app.route('/offers')
def offers():
  offer = lambda doc: { 'id': doc['id'], 'price': round(doc['price'] / 2, 2) }
  products = [offer(doc) for doc in random.sample(app.docs, 64)]

  manufacturers = set(doc['manufacturer'] for doc in app.docs if 'manufacturer' in doc)
  deal = lambda m: { 'manufacturer': m, 'discount': random.randint(1, 10) * 5 }
  discounts = [deal(m) for m in random.sample(manufacturers, 3)]

  return json.dumps({ 'products': products, 'discounts': discounts })


if __name__ == "__main__":
  app.docs = list(read(sys.argv[1]))

  app.run(port=8000, debug=True)
