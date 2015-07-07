package uk.ac.ebi.spot.biosolr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.biosolr.config.SpringJenaConfiguration;
import uk.ac.ebi.spot.biosolr.config.SpringSolrConfiguration;
import uk.co.flax.biosolr.ontology.config.JenaConfiguration;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.jena.JenaOntologySearch;
import uk.co.flax.biosolr.ontology.search.solr.SolrDocumentSearch;
import uk.co.flax.biosolr.ontology.search.solr.SolrOntologySearch;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 26/06/15
 */
@Component
public class WebConfiguration {
    private final SolrConfiguration solrConfiguration;

    private final JenaConfiguration jenaConfiguration;

    @Autowired
    public WebConfiguration(SolrConfiguration solrConfiguration,
                            JenaConfiguration jenaConfiguration) {
        this.solrConfiguration = solrConfiguration;
        this.jenaConfiguration = jenaConfiguration;
    }

    @Bean OntologySearch ontologySearch() {
        return new SolrOntologySearch(solrConfiguration);
    }

    @Bean DocumentSearch documentSearch() {
        return new SolrDocumentSearch(solrConfiguration);
    }

    @Bean JenaOntologySearch jenaOntologySearch() {
        return new JenaOntologySearch(jenaConfiguration, solrConfiguration);
    }
}
