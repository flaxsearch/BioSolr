from flask import Flask, request

app = Flask(__name__)

@app.route('/')
def main():
  return 'click-through API'

@app.route('/stock/<id>')
def stock(id):
  

if __name__ == "__main__":
  app.run(port=5001, debug=True)
