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

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.config.JenaConfiguration;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Dataset ds;
		
		LOGGER.info("Construct a dataset backed by Solr");
		Dataset jenaData = buildBaseDataset();

		if (StringUtils.isNotBlank(jenaConfig.getAssemblerFile())) {
			// The assembler file contains the full mapping detail
			ds = jenaData;
		} else {
			// Define the index mapping
			EntityDefinition entDef = buildEntityDefinition();
			// Define the Solr server
			SolrClient server = new HttpSolrClient(solrConfig.getOntologyUrl());
			// Join together into a dataset
			ds = TextDatasetFactory.create(jenaData, new TextIndexSolr5(server, entDef));
		}
		
		return ds;
	}
	
	private EntityDefinition buildEntityDefinition() {
		// Build the base definition
		EntityDefinition entity = new EntityDefinition(jenaConfig.getEntityField(), jenaConfig.getPrimaryField());
		
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
		
		if (StringUtils.isNotBlank(jenaConfig.getAssemblerFile())) {
			LOGGER.debug("Building dataset from assembler file {}", jenaConfig.getAssemblerFile());
			jenaData = DatasetFactory.assemble(jenaConfig.getAssemblerFile(), jenaConfig.getAssemblerDataset());
		} else if (StringUtils.isNotBlank(jenaConfig.getTdbPath())) {
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
