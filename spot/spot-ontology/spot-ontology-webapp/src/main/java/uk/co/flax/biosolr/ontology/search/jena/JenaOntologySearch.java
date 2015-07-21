/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.search.jena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.JenaConfiguration;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;
import uk.co.flax.biosolr.ontology.search.solr.SolrOntologySearch;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Search engine providing SPARQL search through Apache Jena, using
 * Solr as a back-end to search labels.
 * 
 * @author Matt Pearce
 */
public class JenaOntologySearch {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JenaOntologySearch.class);
	
	private final JenaConfiguration jenaConfig;
	private final SolrConfiguration solrConfig;

	private final Dataset dataset;

	public JenaOntologySearch(JenaConfiguration jenaConfig, SolrConfiguration solrConfig) {
		this.jenaConfig = jenaConfig;
		this.solrConfig = solrConfig;
		this.dataset = buildDataSet();
	}
	
	private Dataset buildDataSet() {
		LOGGER.info("Construct a dataset backed by Solr");
		Dataset jenaData = buildBaseDataset();

		// Define the index mapping
		EntityDefinition entDef = buildEntityDefinition();
		// Define the Solr server
		SolrServer server = new HttpSolrServer(solrConfig.getOntologyUrl());
		// Join together into a dataset
		Dataset ds = TextDatasetFactory.createSolrIndex(jenaData, server, entDef);
		return ds;
	}
	
	private EntityDefinition buildEntityDefinition() {
		// Build the base definition
		EntityDefinition entity = new EntityDefinition(SolrOntologySearch.URI_FIELD, jenaConfig.getPrimaryField());
		
		// And add the field mappings
		for (String field : jenaConfig.getFieldMappings().keySet()) {
			for (String mapping : jenaConfig.getFieldMappings().get(field)) {
				entity.set(field, ResourceFactory.createProperty(mapping).asNode());
			}
		}
		
		return entity;
	}
	
	private Dataset buildBaseDataset() {
		Dataset jenaData;
		
		if (StringUtils.isNotBlank(jenaConfig.getTdbPath())) {
			LOGGER.debug("Building dataset from TDB data at {}", jenaConfig.getTdbPath());
			jenaData = TDBFactory.createDataset(jenaConfig.getTdbPath());
		} else {
			LOGGER.debug("Building dataset from ontology URI {}", jenaConfig.getOntologyUri());
			FileManager fileManager = FileManager.get();
			Model model = fileManager.loadModel(jenaConfig.getOntologyUri());
			
			// Build the base dataset backed by the model loaded from the URI
			jenaData = DatasetFactory.create(model);
		}
		
		return jenaData;
	}

	public ResultsList<Map<String, String>> searchOntology(String prefix, String query, int rows) throws SearchEngineException {
		ResultsList<Map<String, String>> results = null;
		
		dataset.begin(ReadWrite.READ);
		try {
			List<Map<String, String>> resultsList = new ArrayList<>();
			
			Query q = QueryFactory.create(prefix + "\n" + query);
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet rs = qexec.execSelect();
			List<String> vars = rs.getResultVars();
			while (rs.hasNext()) {
				Map<String, String> resultMap = extractResultMap(rs.next(), vars);
				if (!resultMap.isEmpty()) {
					resultsList.add(resultMap);
				}
			}
			
			results = new ResultsList<>(resultsList, rows, 0, rows);
		} catch (QueryException e) {
			throw new SearchEngineException(e);
		} finally {
			dataset.end();
		}
		
		return results;
	}
	
	private Map<String, String> extractResultMap(QuerySolution qs, List<String> vars) {
		Map<String, String> resultMap = new HashMap<>();

		for (String var : vars) {
			RDFNode node = qs.get(var);
			if (node != null) {
				if (node instanceof Literal) {
					resultMap.put(var, ((Literal)node).getLexicalForm());
				} else {
					resultMap.put(var, node.toString());
				}
			}
		}
		
		return resultMap;
	}

}
