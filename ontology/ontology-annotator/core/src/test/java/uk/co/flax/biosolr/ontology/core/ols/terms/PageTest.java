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
package uk.co.flax.biosolr.ontology.core.ols.terms;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the Page class.
 *
 * <p>Created by Matt Pearce on 24/11/15.</p>
 * @author Matt Pearce
 */
public class PageTest {

	@Test
	public void compareTo_nullComparison() {
		final int pageNum = 0;
		final Page page = new Page(10, 10, 10, pageNum);

		assertTrue(page.compareTo(null) > 0);
	}

	@Test
	public void compareTo_lessThan() {
		final int pageNum = 0;
		final Page page = new Page(10, 10, 10, pageNum);
		final int cmpPageNum = 1;
		final Page cmpPage = new Page(10, 10, 10, cmpPageNum);

		assertTrue(page.compareTo(cmpPage) < 0);
	}

	@Test
	public void compareTo_moreThan() {
		final int pageNum = 1;
		final Page page = new Page(10, 10, 10, pageNum);
		final int cmpPageNum = 0;
		final Page cmpPage = new Page(10, 10, 10, cmpPageNum);

		assertTrue(page.compareTo(cmpPage) > 0);
	}

	@Test
	public void compareTo_equal() {
		final int pageNum = 1;
		final Page page = new Page(10, 10, 10, pageNum);
		final int cmpPageNum = 1;
		final Page cmpPage = new Page(10, 10, 10, cmpPageNum);

		assertTrue(page.compareTo(cmpPage) == 0);
	}

}
