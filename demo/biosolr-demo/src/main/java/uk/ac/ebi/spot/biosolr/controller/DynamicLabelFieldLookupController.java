package uk.ac.ebi.spot.biosolr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06/07/15
 */
@RestController
@RequestMapping("/service")
public class DynamicLabelFieldLookupController {
    private static final String LABEL_FIELD_REGEX = ".*_rel_labels$";

    private final DocumentSearch documentSearch;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    @Autowired
    public DynamicLabelFieldLookupController(DocumentSearch documentSearch) {
        this.documentSearch = documentSearch;
    }

    @RequestMapping(value = "/dynamicLabelFields",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> doSearch() {
        List<String> retList = new ArrayList<>();

        try {
            List<String> fieldList = documentSearch.getDynamicFieldNames();
            for (String fieldName : fieldList) {
                if (fieldName.matches(LABEL_FIELD_REGEX)) {
                    retList.add(fieldName);
                }
            }
        }
        catch (SearchEngineException e) {
            getLog().error("Error thrown finding dynamic document fields: {}", e.getMessage());
        }

        return retList;
    }
}
