package uk.ac.ebi.spot.biosolr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.api.SearchResponse;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06/07/15
 */
@RestController
@RequestMapping("/service")
public class OntologySearchController {
    private final OntologySearch ontologySearch;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    @Autowired
    public OntologySearchController(OntologySearch ontologySearch) {
        this.ontologySearch = ontologySearch;
    }

    @RequestMapping(value = "/ontology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse<OntologyEntryBean> doSearch(@RequestParam("q") String query,
                                                      @RequestParam("start") int start,
                                                      @RequestParam("rows") int rows) {
        SearchResponse<OntologyEntryBean> response;

        try {
            ResultsList<OntologyEntryBean> results = ontologySearch.searchOntology(query, null, start, rows);
            response = new SearchResponse<>(results.getResults(), start, rows, results.getNumResults());
        }
        catch (SearchEngineException e) {
            getLog().error("Exception thrown searching ontologies: {}", e.getMessage());
            response = new SearchResponse<>(e.getMessage());
        }

        return response;
    }

}
