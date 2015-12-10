/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
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

package uk.co.flax.biosolr.ontology.core.owl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * Unit tests for the OWLOntologyConfiguration class.
 *
 * @author mlp
 */
public class OWLOntologyConfigurationTest {

	@Test
	public void defaultConfiguration() {
		OWLOntologyConfiguration test = OWLOntologyConfiguration.defaultConfiguration();
		assertEquals(Collections.singletonList(OWLOntologyConfiguration.SYNONYM_PROPERTY_URI), test.getSynonymPropertyUris());
		assertEquals(Collections.singletonList(OWLOntologyConfiguration.LABEL_PROPERTY_URI), test.getLabelPropertyUris());
		assertEquals(Collections.singletonList(OWLOntologyConfiguration.DEFINITION_PROPERTY_URI), test.getDefinitionPropertyUris());
		assertTrue(test.getIgnorePropertyUris().isEmpty());
	}

}
