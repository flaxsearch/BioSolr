# Sequence joins

This directory contains code for joining Solr searches with external sequence databases.

### XJoin

Base classes for Solr joins with external data sources.

### Fasta

Implementation for joins with the EBI FASTA database.

### Phmmer

Implementation for joins with the EBI Phmmer sequence model.

## Building the libraries

All the above are written in Java and packaged as JAR files. You will need a Java 8 compiler and
Apache Maven to build them.

Compile XJoin first:
```
    cd xjoin
    mvn clean install
```

This will compile and create the JAR file and install it in your local Maven repository. Then build
the Phmmer component:
```
    cd phmmer
    mvn clean package
```

FASTA has a dependency on a JAR file supplied by EBI which implements the glue code for FASTA lookup
(the Java package is `uk.ac.ebi.webservices.axis1.stubs.fasta`.) To install this in your local Maven
repository, use the following command:
```
    mvn install:install-file \
     -Dfile=<path-to-JAR> \
     -DgroupId=uk.ac.ebi.webservices \
     -DartifactId=axis1 \
     -Dversion=2017.2.5 \
     -Dpackaging=jar
     -DgeneratePom=true
```

(Note that the version string here is arbitrary as I did not know the exact version. You may want
to change this and the version in `fasta/pom.xml` to a more appropriate string.)

Then build the FASTA component:
```
    cd fasta
    mvn clean package
```

## Using the components in Solr

As well as the three JAR files created above and the one supplied by EBI, you will need the following
libraries:

  * org.apache.commons.commons-discovery (I used version 0.5)
  * org.apache.commons.commons-httpclient (I used version 3.1)
  * WSDL4J (I used version 1.6.2)
  * org.apache.axis (I used version 1.4)
  * org.glassfish.javax.json (I used version 1.0.4)
  * org.glassfish.javax.xml.rpc (I used version 10.0-b28)

These can be downloaded from the Maven repository (https://mvnrepository.com/ .) Copy all ten JARs
to a suitable directory, and add the following configuration to your `solrconfig.xml`:
```
    <lib path="[LIB DIR]/commons-discovery-0.5.jar" />
    <lib path="[LIB DIR]/commons-httpclient-3.1.jar" />
    <lib path="[LIB DIR]/wsdl4j-1.6.2.jar" />
    <lib path="[LIB DIR]/axis-1.4.jar" />
    <lib path="[LIB DIR]/axis1-2017.2.5.jar" />
    <lib path="[LIB DIR]/javax.json-1.0.4.jar" />
    <lib path="[LIB DIR]/javax.xml.rpc-10.0-b28.jar" />
    <lib path="[LIB DIR]/xjoin-7.2.1-0.1.jar" />
    <lib path="[LIB DIR]/xjoin-fasta-7.2.1-0.1.jar" />
    <lib path="[LIB DIR]/xjoin-phmmer-7.2.1-0.1.jar" />
 ```
(replace in [LIB DIR] with the appropriate path.)

To configure the FASTA and Phmmer components, add the following to `solrconfig.xml`:
```
  <queryParser name="xjoin" class="org.apache.solr.search.xjoin.XJoinQParserPlugin" />

  <valueSourceParser name="fasta" class="org.apache.solr.search.xjoin.XJoinValueSourceParser">
    <str name="xJoinSearchComponent">xjoin_fasta</str>
    <double name="defaultValue">1.0</double>
  </valueSourceParser>

  <valueSourceParser name="phmmer" class="org.apache.solr.search.xjoin.XJoinValueSourceParser">
    <str name="xJoinSearchComponent">xjoin_phmmer</str>
    <double name="defaultValue">1.0</double>
  </valueSourceParser>

  <searchComponent name="xjoin_fasta" class="org.apache.solr.search.xjoin.XJoinSearchComponent">
    <str name="factoryClass">uk.co.flax.biosolr.pdbe.fasta.FastaXJoinResultsFactory</str>
    <str name="joinField">pdb_id</str>
    <lst name="external">
      <str name="email">[FASTA EMAIL]</str>
      <str name="program">[FASTA PROGRAM]</str>
      <str name="database">[FASTA DB]</str>
      <str name="stype">[FASTA STYPE]</str>
    </lst>
  </searchComponent>

  <searchComponent name="xjoin_phmmer" class="org.apache.solr.search.xjoin.XJoinSearchComponent">
    <str name="factoryClass">uk.co.flax.biosolr.pdbe.phmmer.PhmmerXJoinResultsFactory</str>
    <str name="joinField">pdb_id</str>
    <lst name="external">
      <str name="url">[PHMMER URL]</str>
      <str name="database">[PHMMER DB]</str>
    </lst>
  </searchComponent>
```
replacing [FASTA EMAIL], [FASTA PROGRAM], [FASTA DB], [FASTA STYPE], [PHMMER URL], and
[PHMMER DB] with the appropriate values. To make these components available to a search request
handler, add the following fragment to the handler in `solrconfig.xml`:
```
    <arr name="first-components">
      <str>xjoin_fasta</str>
      <str>xjoin_phmmer</str>
    </arr>
    <arr name="last-components">
      <str>xjoin_fasta</str>
      <str>xjoin_phmmer</str>
    </arr>
```

An example `solrconfig.xml` is provided in `sequence/solr-conf`.

To enable logging of these components, add the following line to your Solr `log4j.properties` file:
```
    log4j.logger.uk.co.flax.biosolr=DEBUG
```
(replacing DEBUG with the desired logging level.)
