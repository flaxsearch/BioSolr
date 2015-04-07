package org.apache.solr.search.xjoin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.solr.search.xjoin.NameConverter;
import org.junit.Test;

public class TestNameConverter {

	@Test
	public void fieldToMethod() {
		assertEquals("getFooBar", NameConverter.getMethodName("foo_bar"));
		assertEquals("getAProperty", NameConverter.getMethodName("a_property"));
	}
	
	@Test
	public void methodToField() {
		assertEquals("foo_bar", NameConverter.getFieldName("isFooBar"));
		assertEquals("a_property", NameConverter.getFieldName("getAProperty"));
	}
	
	@Test
	public void nulls() {
		assertNull(NameConverter.getFieldName("doSomething"));
	}
	
}
