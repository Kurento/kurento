/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.kurento.modulecreator.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class XmlFusioner {

  private Path generatedXml;
  private Path customizerXml;
  private Path outputFile;

  private Set<String> addChildrenTags;
  private Set<String> replaceChildrenTags;

  public XmlFusioner(Path generatedXml, Path customizerXml, Path outputFile) {
    this(generatedXml, customizerXml, outputFile, null, null);
  }

  public XmlFusioner(Path generatedXml, Path customizerXml, Path outputFile,
      String[] addChildrenTags, String[] replaceChildrenTags) {
    super();
    this.generatedXml = generatedXml;
    this.customizerXml = customizerXml;
    this.outputFile = outputFile;
    this.addChildrenTags = new HashSet<String>(Arrays.asList(addChildrenTags));
    this.replaceChildrenTags = new HashSet<String>(Arrays.asList(replaceChildrenTags));
  }

  public void fusionXmls()
      throws ParserConfigurationException, SAXException, IOException, TransformerException {

    Document generatedXmlDoc = loadXml(generatedXml);
    Document customizedXmlDoc = loadXml(customizerXml);

    merge(generatedXmlDoc.getFirstChild(), customizedXmlDoc.getFirstChild());

    writeXml(generatedXmlDoc);
  }

  private void merge(Node genNode, Node custNode) {

    NodeList list = custNode.getChildNodes();

    for (int i = 0; i < list.getLength(); i++) {

      Node custChildNode = list.item(i);

      if (custChildNode instanceof Text) {
        continue;
      }

      Node genChildNode = getNode(genNode, custChildNode.getNodeName());

      if (genChildNode != null) {

        String nodePath = getPath(genChildNode);

        if (replaceChildrenTags.contains(nodePath)) {

          merge(genChildNode, custChildNode);

        } else if (addChildrenTags.contains(nodePath)) {

          addChilds(custChildNode, genChildNode);

        } else if (includedInReplaceOrAdd(nodePath)) {

          merge(genChildNode, custChildNode);

        } else {

          // Replace entire node
          Node newNode = genNode.getOwnerDocument().importNode(custChildNode, true);
          genNode.replaceChild(newNode, genChildNode);
        }

      } else {

        // Add new node
        Node newNode = genNode.getOwnerDocument().importNode(custChildNode, true);
        genNode.appendChild(newNode);

      }
    }
  }

  private boolean includedInReplaceOrAdd(String nodePath) {

    for (String path : replaceChildrenTags) {
      if (path.startsWith(nodePath)) {
        return true;
      }
    }

    for (String path : addChildrenTags) {
      if (path.startsWith(nodePath)) {
        return true;
      }
    }

    return false;
  }

  private void addChilds(Node custChildNode, Node genChildNode) {
    NodeList list = custChildNode.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node node = list.item(i);
      Node newNode = genChildNode.getOwnerDocument().importNode(node, true);
      genChildNode.appendChild(newNode);
    }
  }

  private String getPath(Node node) {
    StringBuilder sb = new StringBuilder();
    while (node.getParentNode() != null && !(node.getParentNode() instanceof Document)) {
      sb.insert(0, "/" + node.getNodeName());
      node = node.getParentNode();
    }
    return sb.toString();
  }

  private Node getNode(Node baseNode, String tagName) {
    NodeList list = baseNode.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      if (list.item(i).getNodeName().equals(tagName)) {
        return list.item(i);
      }
    }
    return null;
  }

  private Document loadXml(Path xmlPath)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(xmlPath.toString());
    return doc;
  }

  private void writeXml(Document doc) throws TransformerFactoryConfigurationError,
      TransformerConfigurationException, IOException, TransformerException {

    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(Files.newOutputStream(outputFile));
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(source, result);
  }

}
