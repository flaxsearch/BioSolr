/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core.ols;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.ext.ContextResolver;

/**
 * Context resolver implementation for OLS mappings.
 *
 * <p>
 * This specifically disables failing on unknown properties when
 * deserializing objects from OLS.
 * </p>
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 *
 * @author Matt Pearce
 */
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

	private final ObjectMapper defaultMapper;

	public ObjectMapperResolver() {
		defaultMapper = createDefaultMapper();
	}

	private static ObjectMapper createDefaultMapper() {
		ObjectMapper defaultMapper = new ObjectMapper();
		defaultMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		defaultMapper.disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS);
		return defaultMapper;
	}

	@Override
	public ObjectMapper getContext(Class<?> aClass) {
		return defaultMapper;
	}
}
