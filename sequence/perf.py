import requests
import random

r = requests.get('http://localhost:8983/solr/pdb_entity_db1/select?q=*:*&fl=entry_entity&rows=6000&wt=json')
j = r.json()
ids = [ d['entry_entity'] for d in j['response']['docs'] ]

for _ in xrange(10):
  test_ids = random.sample(ids, 5000)

  r = requests.post('http://localhost:8983/solr/pdb_entity_db1/test?wt=json', { 'xjoin_test.external.values': ','.join(test_ids) })
  j = r.json()
  print j['response']['numFound'], j['responseHeader']['QTime']

print
print "Weighted"

for _ in xrange(10):
  test_ids = random.sample(ids, 5000)
  
  r = requests.post('http://localhost:8983/solr/pdb_entity_db1/test?wt=json&bf=test(xjoin_test)', { 'xjoin_test.external.values': ','.join(test_ids) })
  j = r.json()
  print j['response']['numFound'], j['responseHeader']['QTime']

print
print "Intersections"

for _ in xrange(10):
  ids_1 = random.sample(ids, 5000)
  ids_2 = random.sample(ids, 5000)
  
  params = { 'xjoin_test.external.values': ','.join(ids_1), 'xjoin_test2.external.values': ','.join(ids_2) }
  r = requests.post('http://localhost:8983/solr/pdb_entity_db1/intersect?wt=json', params)
  j = r.json()
  print j['response']['numFound'], j['responseHeader']['QTime']
