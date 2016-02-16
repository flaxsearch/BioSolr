/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockserver.matchers.BooleanMatcher;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Created by mlp on 26/01/16.
 * @author mlp
 */
public class OntologySettingsTest {

	@Test
	public void getFieldMappings_includeIndirect() {
		OntologySettings settings = new OntologySettings();
		settings.setIncludeIndirect(true);

		List<FieldMappings> mappings = settings.getFieldMappings();
		assertThat(mappings.size(), equalTo(FieldMappings.values().length));
	}

	@Test
	public void getFieldMappings_notIncludeIndirect() {
		OntologySettings settings = new OntologySettings();
		settings.setIncludeIndirect(false);

		List<FieldMappings> mappings = settings.getFieldMappings();
		for (FieldMappings fm : mappings) {
			assertFalse(fm.isIndirect());
		}
	}

}
