package org.apache.solr.search.xjoin;

/**
 * Utility methods for converting between method names and field names.
 */
public class NameConverter {
  
  /**
   * Given a method name, generate a field name if it's a getXxx or isXxx method.
   * (Otherwise, return null.) The name is lower cased and _ seperated, so e.g.
   * getFooBar => foo_bar.
   */
  public static String getFieldName(String methodName) {
    int i;
        if (methodName.startsWith("get")) {
          i = 3;
        } else if (methodName.startsWith("is")) {
          i = 2;
        } else {
          return null;
        }
        StringBuilder fieldName = new StringBuilder();
        for (; i < methodName.length(); ++i) {
          char c = methodName.charAt(i);
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
  
  /**
   * Given a field name, generate a method name. The name is CamelCased, so
   * e.g. foo_bar => getFooBar.
   */
  public static String getMethodName(String fieldName) {
    StringBuilder methodName = new StringBuilder("get");
    for (String bit : fieldName.split("_")) {
      methodName.append(bit.substring(0, 1).toUpperCase());
      methodName.append(bit.substring(1));
    }
    return methodName.toString();
  }

}
