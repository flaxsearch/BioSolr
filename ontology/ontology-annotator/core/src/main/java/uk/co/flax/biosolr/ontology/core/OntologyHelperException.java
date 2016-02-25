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
package uk.co.flax.biosolr.ontology.core;

/**
 * Exception indicating issues with the OntologyHelper.
 *
 * <p>Created by Matt Pearce on 20/10/15.</p>
 * @author Matt Pearce
 */
public class OntologyHelperException extends Exception {

	private static final long serialVersionUID = 1L;

	public OntologyHelperException() {
		super();
	}

	public OntologyHelperException(String msg) {
		super(msg);
	}

	public OntologyHelperException(Throwable t) {
		super(t);
	}

	public OntologyHelperException(String msg, Throwable t) {
		super(msg, t);
	}

}
