package org.apache.solr.util;

import org.xml.sax.SAXException;

// horrible hack to allow the use of TestHarness.validateUpdate() on an alternate core
public class TestHarnessWrapper {

  private TestHarness h;
  private String coreName;
  
  public TestHarnessWrapper(TestHarness h, String coreName) {
    this.h = h;
    this.coreName = coreName;
  }
  
  public String validateUpdate(String xml) throws SAXException {
    final String originalCoreName = h.coreName;
    try {
      h.coreName = coreName;
      return h.validateUpdate(xml);
    } finally {
      h.coreName = originalCoreName;
    }
  }
  
}
