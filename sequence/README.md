XJoin
-----

The new "xjoin" SOLR contrib allows external results to be joined with SOLR results in a query and the SOLR result set
to be filtered by the results of an external query. Values from the external results are made available in the SOLR
results and may also be used to boost the scores of corresponding documents during the search. The contrib consists of
the Java classes XJoinSearchComponent, XJoinValueSourceParser, and XJoinQParserPlugin (and associated classes) which
must be configured in solrconfig.xml, and the interfaces XJoinResultsFactory and XJoinResults, which are implemented
by the user to provide the link between SOLR and the external results source. External results and SOLR documents are
matched via a single configurable attribute (the "join field"). The JAR xjoin.jar (attached) contains these classes
and interfaces and is included in SOLR's class path in solrconfig.xml, as should a JAR containing the user
implementations of the previously mentioned interfaces.

For example:

```
<config>
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

### XJoinResultsFactory

The user implementation of this interface is responsible for connecting to an external source to perform a query (or
otherwise collect results). Parameters with prefix "<component name>.external." are passed from the SOLR query URL to
pararameterise the search. The interface has the following methods:

  * void init(NamedList args) - this is called during SOLR initialisation, and passed parameters from the search
                                component configuration (see below)
                              
  * XJoinResults getResults(SolrParams params) - this is called during a SOLR search to generate external results, and
                                                 is passed parameters from the SOLR query URL (as above)

For example, the implementation might perform queries of an external source based on the 'q' SOLR query URL parameter
(in full, <component name>.external.q).

### XJoinResults

A user implementation of this interface is returned by the getResults() method of the XJoinResultsFactory
implementation. It has methods:

  * Object getResult(String joinId) - this should return a particular result given the value of the join attribute

  * Iterable<String> getJoinIds() - this should return the join attribute values for results of the external search

### XJoinSearchComponent

This is the central Java class of the contrib. It is a SOLR search component, configured in solrconfig.xml and
included in one or more SOLR request handlers. There is one XJoin search component per external source, and each has two main responsibilities:

  * Before the SOLR search, it connects to the external source and retrieves results, storing them in the SOLR
    request context

  * After the SOLR search, it matches SOLR document in the results set and external results via the join field, adding
    attributes from the external results to documents in the SOLR results set

It takes the following initialisation parameters:

  * factoryClass - this specifies the user-supplied class implementing XJoinResultsFactory, used to generate external
                   results
                   
  * joinField - this specifies the attribute on which to join between SOLR documents and external results

  * external - this parameter set is passed to configure the XJoinResultsFactory implementation

For example, in solrconfig.xml:

```
<searchComponent name="xjoin_test" class="org.apache.solr.search.xjoin.XJoinSearchComponent">
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

  * results - a comma-separated list of attributes from the XJoinResults implementation (created by the factory at
              search time) to be included in the SOLR results
              
  * fl - a comma-separated list of attributes from results objects (contained in an XJoinResults implementation) to
         be included in the SOLR results
         
For example:

```
<requestHandler name="/xjoin" class="solr.SearchHandler" startup="lazy">
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

### XJoinQParserPlugin

This query parser plugin constructs a query from the results of the external searches, and is based on the TermsQParserPlugin. It takes the following local parameters:

  * method - as the TermsQParserPlugin, this specifies how to build the Lucene query based on the join ids contained in external results; one of termsFilter, booleanQuery, automaton, or docValuesTermsFilter (defaults to termsFilter)

  * v (or as usual with query parsers, specified via the query) - a Boolean combination of XJoin search component names. Supported operators are OR, AND, XOR, and AND NOT

The query is a Boolean expression whose terms are XJoin search component names. The resulting set of join ids (obtained from the respective XJoin search components) are formed into a Lucene query. Note that the join field of all the referenced XJoin search components must be identical. Of course, the expression can be a single XJoin search component name in the simplest situation. For example:

```
q={!xjoin}xjoin_test
q={!xjoin v=xjoin_test}
fq={!xjoin method=automaton}xjoin_test1 AND NOT xjoin_test2
```

### XJoinValueSourceParser

This class provides a SOLR function that may be used, for example, in a boost function to weight the result score from external values. The function returns an attribute value from the external result with matching join attribute. There are two ways of using the function. Either the XJoin component name is specified in the configuration parameters and the external result attribute is the argument of the function in the query, or vice versa, the attribute is specified in the configuration parameters and the component name is the function argument.

The parameters for configuration in solrconfig.xml are:

  * xJoinSearchComponent - the name of an XJoin search component containing external results
  * attribute - the attribute to use from external results
  * defaultValue - if the external result has no such attribute, then this value is returned

Normally, only one of xJoinSearchComponent and attribute is configured, but it is possible to specify both (but you must specify at least one).

For example:

```
<valueSourceParser name="test_fn" class="org.apache.solr.search.xjoin.XJoinValueSourceParser">
  <str name="xJoinSearchComponent">xjoin_test</str>
  <double name="defaultValue">1.0</double>
</valueSourceParser>
```

Alternatively:
```
<valueSourceParser name="test_fn" class="org.apache.solr.search.xjoin.XJoinValueSourceParser">
  <str name="attribute">value</str>
  <double name="defaultValue">1.0</double>
</valueSourceParser>
```

Mapping between attributes and Java methods
-------------------------------------------

Java method names are converted into attribute (field) names by stripping the initial "get" or "is" and converting
the remainder from CamelCase to lowercase-with-underscores, and vice versa.

For example, getScore() <-> score or getFooBar() <-> foo_bar.

The field list parameter of XJoinSearchComponent (fl) can be given as *, in which case all methods beginning 'get'
or 'is' are converted into fields in the SOLR result for the document.


Putting it together - the SOLR query URL
----------------------------------------

Here is an example SOLR query URL to perform an xjoin:

```
http://solrserver:8983/solr/collection1/xjoin?defType=edismax&q=*:*&xjoin_test.external.q=foobar&fl=id,score&fq={!terms+f=id+v=$xx}&bf=test_fn(value)
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

Joining results from multiple external sources
----------------------------------------------

There are (at least) 3 different ways XJoin can be used in conjunction with other SOLR features to combine results from more than one external source.

### Multiple filter queries

Multiple filter queries are ANDed together by SOLR, so if this is the desired combination for external result join ids, this is a simple approach. (Note the implications for filter caching.) In this case, the external join fields do not have to be the same.

For example (assuming two configured XJoin components, xjoin_test and xjoin_other):
```
http://solrserver:8983/solr/collection1/xjoin?q=*:*&xjoin_test.external.q=foobar&xjoin_other.external.q=barfoo&fq={!xjoin}xjoin_test&fq={!xjoin}xjoin_other
```
### Nested queries in the standard SOLR Query Parser

The nested query syntax of the standard SOLR query parser (see https://wiki.apache.org/solr/SolrQuerySyntax) can be used for more complicated combinations, allowing for "should", "must" etc. Lucene queries to be built from external join id sets. The external join fields do not have to be the same.

For example (again, assuming two configured XJoin components, xjoin_test and xjoin_other):
```
http://solrserver:8983/solr/collection1/xjoin?q=*:*&xjoin_test.external.q=foobar&xjoin_other.external.q=barfoo&fq=_query_:"{!xjoin}xjoin_test" -_query_:"{!xjoin}xjoin_other"
```
### Boolean expressions with the XJoin Query Parser

To combine external join id sets directly using a Boolean expression, one can use the XJoinQParserPlugin as detailed above. This allows arbitrary Boolean expressions using the operators AND, OR, XOR and AND NOT.

For example (again, assuming two configured XJoin components, xjoin_test and xjoin_other):
```
http://solrserver:8983/solr/collection1/xjoin?q=*:*&xjoin_test.external.q=foobar&xjoin_other.external.q=barfoo&fq={!xjoin}xjoin_test XOR xjoin_other
```

