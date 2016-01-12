from flask import Flask, request, g
import json
import sqlite3 as sql

# flask application context attribute for caching database connection
DB_APP_KEY = '_database'

# default weight for storing against queries
DEFAULT_WEIGHT = 1.0

app = Flask(__name__)

def get_db():
  """ Obtain a (cached) DB connection and return a cursor for it.
  """
  db = getattr(g, DB_APP_KEY, None)
  if db is None:
    db = sql.connect('click.db')
    setattr(g, DB_APP_KEY, db)
    c = db.cursor()
    c.execute("CREATE VIRTUAL TABLE IF NOT EXISTS click USING fts4 ("
                "id VARCHAR(256),"
                "q VARCHAR(256),"
                "weight FLOAT"
              ")")
    c.close()
  return db

@app.teardown_appcontext
def teardown_db(exception):
  db = getattr(g, DB_APP_KEY, None)
  if db is not None:
    db.close()

@app.route('/')
def main():
  return 'click-through API'

@app.route('/click/<id>', methods=["PUT"])
def click(id):
  # validate request
  if 'q' not in request.args:
    return 'Missing q parameter', 400
  q = request.args['q']
  try:
    w = float(request.args.get('w', DEFAULT_WEIGHT))
  except ValueError:
    return 'Could not parse weight', 400

  # do the DB insert
  db = get_db()
  try:
    c = db.cursor()
    c.execute("INSERT INTO click (id, q, weight) VALUES (?, ?, ?)", (id, q, w))
    db.commit()
    return 'OK'
  finally:
    c.close()

@app.route('/ids')
def ids():
  if 'q' not in request.args:
    return 'Missing q parameter', 400
  try:
    c = get_db().cursor()
    c.execute("SELECT id, SUM(weight) FROM click WHERE q MATCH ? GROUP BY id", (request.args['q'], ))
    return json.dumps([ x for x in c ])
  finally:
    c.close()


if __name__ == "__main__":
  app.run(port=5001, debug=True)
