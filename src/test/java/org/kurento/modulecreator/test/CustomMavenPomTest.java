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
package org.kurento.modulecreator.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;
import org.kurento.modulecreator.codegen.XmlFusioner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomMavenPomTest {

  @Test
  public void xmlFusionerTest() throws Exception {

    Path fusionedXml = Files.createTempFile("pom", ".xml");

    String[] addTags = { "/dependencies", "/build/plugins" };
    String[] replaceTags = { "/properties" };

    XmlFusioner fusioner = new XmlFusioner(
        PathUtils.getPathInClasspath("/custommaven/generated.xml"),
        PathUtils.getPathInClasspath("/custommaven/customizer.xml"), fusionedXml, addTags,
        replaceTags);

    fusioner.fusionXmls();

    printFile(fusionedXml);

    Document doc = loadXmlFile(fusionedXml);

    // Add tags
    assertTagValue(doc, "/project/newTag", "newValue");
    assertTagValue(doc, "/project/url", "http://moduleUrl");

    // Replace tags
    assertTagValue(doc, "/project/packaging", "war");

    // Add properties
    assertTagValue(doc, "/project/properties/custom_prop", "custom_value");
    assertTagValue(doc, "/project/properties/project.build.sourceEncoding", "UTF-8");

    // Replace properties
    assertTagValue(doc, "/project/properties/maven.compiler.target", "1.8");

    // Add dependencies
    assertTagValue(doc, "/project/dependencies/dependency/groupId", "fake_dependency");

    // Original dependencies
    assertTagValue(doc, "/project/build/plugins/plugin/groupId", "fake_plugin");

    // Add plugins
    assertTagValue(doc, "/project/dependencies/dependency/groupId", "org.kurento.module");

    // Original plugins
    assertTagValue(doc, "/project/build/plugins/plugin/groupId", "org.kurento");

    // Original tags
    assertTagValue(doc, "/project/groupId", "org.kurento.module");
    assertTagValue(doc, "/project/artifactId", "moduleA");
    assertTagValue(doc, "/project/version", "1.0.0-SNAPSHOT");
    assertTagValue(doc, "/project/name", "moduleA");
  }

  @Test
  public void test() throws Exception {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/custommaven/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    modCreator.loadModulesFromKmdFiles();

    Path codeGenDir = Files.createTempDirectory("maven");

    modCreator.setCodeGenDir(codeGenDir);
    modCreator.setGenerateMavenPom(true);

    Result result = modCreator.generateCode();

    assertThat("Compilation error: " + result.getErrors(), result.isSuccess(), is(true));

    Path pomFile = codeGenDir.resolve("pom.xml");

    assertThat("The pom.xml should exist", Files.exists(pomFile), is(true));

    printFile(pomFile);

    Document doc = loadXmlFile(pomFile);

    // Add tags
    assertTagValue(doc, "/project/newTag", "newValue");
    assertTagValue(doc, "/project/url", "http://moduleUrl");

    // Replace tags
    assertTagValue(doc, "/project/packaging", "war");

    // Add properties
    assertTagValue(doc, "/project/properties/custom_prop", "custom_value");
    assertTagValue(doc, "/project/properties/project.build.sourceEncoding", "UTF-8");

    // Replace properties
    assertTagValue(doc, "/project/properties/maven.compiler.target", "1.8");

    // Add dependencies
    assertTagValue(doc, "/project/dependencies/dependency/groupId", "fake_dependency");

    // Original dependencies
    assertTagValue(doc, "/project/build/plugins/plugin/groupId", "fake_plugin");

    // Add plugins
    assertTagValue(doc, "/project/dependencies/dependency/groupId", "org.kurento.module");

    // Original plugins
    assertTagValue(doc, "/project/build/plugins/plugin/groupId", "org.kurento");

    // Original tags
    assertTagValue(doc, "/project/groupId", "org.kurento.module");
    assertTagValue(doc, "/project/artifactId", "moduleA");
    assertTagValue(doc, "/project/version", "1.0.0-SNAPSHOT");
    assertTagValue(doc, "/project/name", "moduleA");
  }

  private void assertTagValue(Document doc, String tagName, String tagValue)
      throws XPathExpressionException {

    NodeList list;

    if (tagName.startsWith("/")) {

      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();
      XPathExpression expr = xpath.compile(tagName);

      list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

    } else {

      list = doc.getElementsByTagName(tagName);

      if (list.getLength() == 0) {
        fail("Tag '" + tagName + "' not found in document");
      }
    }

    for (int i = 0; i < list.getLength(); i++) {
      Node node = list.item(i);
      if (node.getTextContent().equals(tagValue)) {
        return;
      }
    }

    fail("There is no tag '" + tagName + "' with value '" + tagValue + "'");

  }

  private Document loadXmlFile(Path pomFile)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(pomFile.toString());
    return doc;
  }

  private void printFile(Path pomFile) throws IOException {
    System.out.println(new String(Files.readAllBytes(pomFile), StandardCharsets.UTF_8));
  }

}
