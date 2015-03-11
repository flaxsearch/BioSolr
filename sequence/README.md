XJoin
-----

The new "xjoin" SOLR contrib allows external results to be joined with SOLR results in a query and the SOLR result set
to be filtered by the results of an external query. Values from the external results are made available in the SOLR
results and may also be used to boost the scores of corresponding documents during the search. The contrib consists of
the Java classes XJoinSearchComponent and XJoinValueSourceParser, which must be configured in solrconfig.xml, and the
interfaces XJoinResultsFactory and XJoinResults, which are implemented by the user to provide the link between SOLR
and the external results source. External results and SOLR documents are matched via a single configurable attribute
(the "join field"). The JAR xjoin.jar (attached) contains these classes and interfaces and is included in SOLR's class
path in solrconfig.xml, as should a JAR containing the user implementations of the previously mentioned interfaces.

For example:

```<config>
  ..
  <!-- XJoin contrib JAR file -->
  <lib path="/path/to/xjoin.jar" />
  ..
  <!-- user implementations of XJoin interfaces -->
  <lib path="/path/to/xjoin_test.jar" />
  ..
</config>
```


Java classes and interfaces
---------------------------

XJoinResultsFactory:

The user implementation of this interface is responsible for connecting to the external source to perform a query (or
otherwise collect results). Parameters with prefix "<component name>.external." are passed from the SOLR query URL to
pararameterise the search. The interface has the following methods:

  * void init(NamedList args) - this is called during SOLR initialisation, and passed parameters from the search
                                component configuration (see below)
                              
  * XJoinResults getResults(SolrParams params) - this is called during a SOLR search to generate external results, and
                                                 is passed parameters from the SOLR query URL (as above)

For example, the implementation might perform queries of an external source based on the 'q' SOLR query URL parameter
(in full, <component name>.external.q).


XJoinResults:

A user implementation of this interface is returned by the getResults() method of the XJoinResultsFactory
implementation. It has methods:

  * Object getResult(String joinId) - this should return a particular result given the value of the join attribute

  * Iterable<String> getJoinIds() - this should return the join attribute values for results of the external search


XJoinSearchComponent:

This is the central Java class of the contrib. It is a SOLR search component, configured in solrconfig.xml and
included in one or more SOLR request handlers. It has two main responsibilities:

  * Before the SOLR search, it connects to the external source and retrieves results, storing them in the SOLR
    request context

  * After the SOLR search, it matches SOLR document in the results set and external results via the join field, adding
    attributes from the external results to documents in the SOLR results set

Additionally, before the SOLR search, it places a list of join field values in a SOLR request parameter, which may be
referenced from other SOLR parameters. It takes the following initialisation parameters:

  * factoryClass - this specifies the user-supplied class implementing XJoinResultsFactory, used to generate external
                   results
                   
  * joinField - this specifies the attribute on which to join between SOLR documents and external results

  * external - this parameter set is passed to configure the XJoinResultsFactory implementation

For example, in solrconfig.xml:

```<searchComponent name="xjoin_test" class="org.apache.solr.search.xjoin.XJoinSearchComponent">
  <str name="factoryClass">test.TestXJoinResultsFactory</str>
  <str name="joinField">id</str>
  <lst name="external">
    <str name="values">1,2,3</str>
  </lst>
</searchComponent>
```

Here, the search component instantiates a new TextXJoinResultsFactory during initialisation, and passes it the
"values" parameter (1, 2, 3) to configure it. To properly use the XJoinSearchComponent in a request handler, it must
be included at the start and end of the component list, and may be configured with the following query parameters:

  * listParameter - external join field values will be placed in this query parameter for reference by local query
                    parameters

  * results - a comma-separated list of attributes from the XJoinResults implementation (created by the factory at
              search time) to be included in the SOLR results
              
  * fl - a comma-separated list of attributes from results objects (contained in an XJoinResults implementation) to
         be included in the SOLR results
         
For example:

```<requestHandler name="/xjoin" class="solr.SearchHandler" startup="lazy">
  <lst name="defaults">
    ..
    <bool name="xjoin_test">true</bool>
    <str name="xjoin_test.listParameter">xx</str>
    <str name="xjoin_test.results">test_count</str>
    <str name="xjoin_test.fl">id,value</str>
  </lst>
  <arr name="first-components">
    <str>xjoin_test</str>
  </arr>
  <arr name="last-components">
    <str>xjoin_test</str>
  </arr>
</requestHandler>
```


XJoinValueSourceParser:

This class provides a SOLR function that may be used, for example, in a boost function to weight the result score from
external values. The function returns an attribute value from the external result with matching join attribute. The
external attribute returned is specified by the argument of the function specification in the SOLR query URL (see
below). The parameters for configuration in solrconfig.xml are:

  * xJoinSearchComponent - the name of the XJoin search component containing the external results
  
  * defaultValue - if the external result has no such attribute, then this value is returned

For example:

```<valueSourceParser name="test_fn" class="org.apache.solr.search.xjoin.XJoinValueSourceParser">
  <str name="xJoinSearchComponent">xjoin_test</str>
  <double name="defaultValue">1.0</double>
</valueSourceParser>
```


Putting it together - the SOLR query URL
----------------------------------------

Here is an example SOLR query URL to perform an xjoin:

```http://solrserver:8983/solr/collection1/xjoin?defType=edismax&q=*:*&xjoin_test.external.q=foobar&fl=id,score&fq={!terms+f=id+v=$xx}&bf=test_fn(value)
```

This might result in the following SOLR response:

```
<?xml version="1.0" encoding="UTF-8"?>
<response>
  <lst name="responseHeader">
    <int name="status">0</int>
    <int name="QTime">346</int>
    <lst name="params">
      ..
    </lst>
  </lst>
  <result name="response" numFound="2" start="0" maxScore="58.60105">
    <doc>
      <str name="id">document1</str>
      <float name="score">58.60105</float>
    </doc>
    <doc>
      <str name="id">document2</str>
      <float name="score">14.260552</float>
    </doc>
  </result>
  <lst name="xjoin_test">
    <int name="test_count">145</int>
    <lst name="doc">
      <str name="id">document1</str>
      <double name="value">7.4</double>
    </lst>
    <lst name="doc">
      <str name="id">document2</str>
      <double name="value">2.3</double>
    </lst>
  </lst>
</response>
```

Notes:

  * The actual 'join' is specified by the fq parameter. Here, SOLR's in-built TermsQParser is invoked (using local
    parameters) to provide a filter query based on the list of join field values obtained from the external results,
    which are referenced using the $xx notation.
    (See https://cwiki.apache.org/confluence/display/solr/Other+Parsers#OtherParsers-TermsQueryParser).
    
  * The function test_fn is used in the bf score-boost function. Since the argument is value2, that attribute of the
    external results is used as the score boost.
