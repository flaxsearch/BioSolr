package org.apache.solr.search.xjoin.simple;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlDocumentFactory {
  
  private final DocumentBuilder builder;
  
  private final XPath xPath;
  
  public XmlDocumentFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    xPath = XPathFactory.newInstance().newXPath();
  }
  
  public PathDocument read(InputStream in) throws SAXException, IOException {
    final Document xml = builder.parse(in);
    
    return new PathDocument() {

      @Override
      public Object getPathValue(String path) {
        try {
          String value = xPath.evaluate(path, xml);
          // try to coerce into various types
          try {
            return Integer.valueOf(value);
          } catch (NumberFormatException e) {
            // do nothing
          }
          try {
            return Double.valueOf(value);
          } catch (NumberFormatException e) {
            // do nothing
          }
          if (value.equalsIgnoreCase("true")) {
            return true;
          }
          if (value.equalsIgnoreCase("false")) {
            return false;
          }
          return value;
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Object[] getPathValues(String path) {
        try {
          NodeList nodes = (NodeList)xPath.evaluate(path, xml, XPathConstants.NODESET);
          String[] values = new String[nodes.getLength()];
          for (int i = 0; i < values.length; ++i) {
            values[i] = nodes.item(i).getNodeValue();
          }
          return values;
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
      
    };
  }

}
