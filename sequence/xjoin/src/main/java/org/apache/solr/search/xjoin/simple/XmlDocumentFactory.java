package org.apache.solr.search.xjoin.simple;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
