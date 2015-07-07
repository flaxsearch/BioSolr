package uk.ac.ebi.spot.biosolr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.co.flax.biosolr.ontology.api.JenaRequest;
import uk.co.flax.biosolr.ontology.api.SearchResponse;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;
import uk.co.flax.biosolr.ontology.search.jena.JenaOntologySearch;

import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06/07/15
 */
@RestController
@RequestMapping("/service")
public class JenaSearchController {
    private final JenaOntologySearch jenaOntologySearch;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    @Autowired
    public JenaSearchController(JenaOntologySearch jenaOntologySearch) {
        this.jenaOntologySearch = jenaOntologySearch;
    }

    @RequestMapping(value = "/jenaSearch",
                    method = RequestMethod.POST,
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse<Map<String, String>> doSearch(JenaRequest request) {
        SearchResponse<Map<String, String>> response;

        try {
            ResultsList<Map<String, String>> results = jenaOntologySearch.searchOntology(request.getPrefix(),
                                                                                         request.getQuery(),
                                                                                         request.getRows());
            response =
                    new SearchResponse<>(results.getResults(), 0, results.getPageSize(), results.getResults().size());
        }
        catch (SearchEngineException e) {
            response = new SearchResponse<>(e.getMessage());
        }

        return response;
    }
}
