curl http://localhost:8983/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/xml' -d '<delete><query>*:*</query></delete>'
curl http://localhost:8983/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/json' -d @server-1/docs
curl http://localhost:8984/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/xml' -d '<delete><query>*:*</query></delete>'
curl http://localhost:8984/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/json' -d @server-2/docs
curl http://localhost:8985/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/xml' -d '<delete><query>*:*</query></delete>'
curl http://localhost:8985/solr/pdb_entity_db1/update?commit=true -H 'Content-type:application/json' -d @server-3/docs
