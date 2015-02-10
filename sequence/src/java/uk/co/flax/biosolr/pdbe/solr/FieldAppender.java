package uk.co.flax.biosolr.pdbe.solr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.common.util.NamedList;

/**
 * Extract FASTA results into SOLR results.
 */
public class FieldAppender {

	private Set<String> fieldNames;
	
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public NamedList addNamedList(NamedList target, String name, Object object) {
	    NamedList<Object> list = new NamedList<>();
	    target.add(name, list);
        for (Method method : object.getClass().getMethods()) {
        	String fieldName = getFieldName(method);
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
	
	private static String getFieldName(Method method) {
		int i = 0;
        if (method.getName().startsWith("get")) {
        	i = 3;
        } else if (method.getName().startsWith("is")) {
        	i = 2;
        } else {
        	return null;
        }
        StringBuilder fieldName = new StringBuilder();
        for (; i < method.getName().length(); ++i) {
        	char c = method.getName().charAt(i);
        	if (Character.isUpperCase(c)) {
        		if (fieldName.length() > 0) {
        			fieldName.append("_");
        		}
        		fieldName.append(Character.toLowerCase(c));
        	} else {
        		fieldName.append(c);
        	}
        }
        return fieldName.toString();		
	}

}
