package uk.co.flax.biosolr.elasticsearch; /**
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

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.plugins.Plugin;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.OntologyMapper;

import java.util.Collection;
import java.util.Collections;

/**
 * Plugin class defining the ontology annotator.
 *
 * @author mlp
 */
public class OntologyUpdatePlugin extends Plugin {

	public static final String NAME = "ontology-update";
	public static final String DESCRIPTION = "Ontology Update plugin";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public Collection<Module> indexModules(Settings indexSettings) {
		return Collections.<Module>singletonList(new OntologyUpdateModule());
	}

	public void onModule(IndicesModule indicesModule) {
		indicesModule.registerMapper(OntologyMapper.CONTENT_TYPE, new OntologyMapper.TypeParser());
	}

}
