package uk.ac.ebi.spot.biosolr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.api.FacetStyle;
import uk.co.flax.biosolr.ontology.api.SearchResponse;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06/07/15
 */
@RestController
@RequestMapping("/service")
public class SearchController {
    private final DocumentSearch documentSearch;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    @Autowired
    public SearchController(DocumentSearch documentSearch) {
        this.documentSearch = documentSearch;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse<Document> doSearch(@RequestParam("q") String query,
                                             @RequestParam(value = "start", required = false, defaultValue = "0") int start,
                                             @RequestParam(value = "rows", required = false, defaultValue = "0") int rows,
                                             @RequestParam(value = "additionalFields", required = false) List<String> additionalFields,
                                             @RequestParam(value = "fq", required = false) List<String> filters,
                                             @RequestParam("facetStyle") FacetStyle facetStyle) {
        SearchResponse<Document> response;

        // Default rows value if not set
        if (rows == 0) {
            rows = 10;
        }

        try {
            ResultsList<Document> results =
                    documentSearch.searchDocuments(query, start, rows, additionalFields, filters,
                                                   facetStyle);
            response = new SearchResponse<>(results.getResults(),
                                            start,
                                            rows,
                                            results.getNumResults(),
                                            results.getFacets());
        }
        catch (SearchEngineException e) {
            getLog().error("Exception thrown during search: {}", e);
            response = new SearchResponse<>(e.getMessage());
        }

        return response;
    }

}
