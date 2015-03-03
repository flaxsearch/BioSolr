package org.apache.solr.xjoin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.xjoin.FieldAppender;
import org.junit.Test;

public class TestFieldAppender {

	@Test
	public void fieldList() {
		FieldAppender fa = new FieldAppender(" foo  bar,  ,  baz, foo ");
		String[] expected = new String[] { "foo", "bar", "baz" };
		assertEquals(new HashSet<>(Arrays.asList(expected)), fa.getFieldNames());
	}

	@Test
	public void fieldListAll() {
		FieldAppender fa = new FieldAppender("foo, * bar");
		assertNull(fa.getFieldNames());
	}
	
	@Test
	public void addFieldNames() {
		FieldAppender fa = new FieldAppender();
		fa.getFieldNames().add("foo");
		fa.getFieldNames().add("bar");
		String[] expected = new String[] { "foo", "bar" };
		assertEquals(new HashSet<>(Arrays.asList(expected)), fa.getFieldNames());
		fa.appendAllFields();
		assertNull(fa.getFieldNames());
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void addNamedList() {
		FieldAppender fa = new FieldAppender(true);
		NamedList root = new NamedList();
		NamedList added = fa.addNamedList(root, "list", new Object() {
			@SuppressWarnings("unused")
			public String getFoo() {
				return "foo";
			}
			
			@SuppressWarnings("unused")
			public int getBar() {
				return 123;
			}
			
			@SuppressWarnings("unused")
			public boolean isBaz() {
				return true;
			}
		});
		assertEquals(added, root.get("list"));
		assertEquals("foo", added.get("foo"));
		assertEquals(123, added.get("bar"));
		assertTrue((boolean)added.get("baz"));
	}
	
}
