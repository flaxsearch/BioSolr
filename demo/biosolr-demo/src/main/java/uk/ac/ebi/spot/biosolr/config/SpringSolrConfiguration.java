package uk.ac.ebi.spot.biosolr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06/07/15
 */
@Component
@ConfigurationProperties(prefix = "solr")
public class SpringSolrConfiguration extends SolrConfiguration {
}
