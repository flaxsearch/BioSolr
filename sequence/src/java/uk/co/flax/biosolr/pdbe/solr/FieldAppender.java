package uk.co.flax.biosolr.pdbe.solr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.common.util.NamedList;

/**
 * Class for adding properties from an object to a NamedList.
 */
public class FieldAppender {

	// selected properties (or null for all)
	private Set<String> fieldNames;
	
	/**
	 * Create a FieldAppender for adding the specified properties.
	 */
	public FieldAppender(String fl) {
		fieldNames = new HashSet<>();
	    for (String field : fl.split("[, ]")) {
	    	field = field.trim();
	    	if ("*".equals(field)) {
	    		fieldNames = null;
	    		return;
	    	}
	    	fieldNames.add(field);
	    }
	}
	
	/**
	 * Add a NamedList (with given name) with properties from the given object.
	 * Returns the new NamedList.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public NamedList addNamedList(NamedList target, String name, Object object) {
	    NamedList<Object> list = new NamedList<>();
	    target.add(name, list);
        for (Method method : object.getClass().getMethods()) {
        	String fieldName = NameConverter.getFieldName(method.getName());
        	if (fieldName == null) {
        		continue;
        	}
            if (fieldNames == null || fieldNames.contains(fieldName)) {
	            try {
					list.add(fieldName, method.invoke(object));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
            }
        }
        return list;
	}

}
