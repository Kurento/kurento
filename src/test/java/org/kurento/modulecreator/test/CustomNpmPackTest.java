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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;
import org.kurento.modulecreator.codegen.JsonFusioner;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CustomNpmPackTest {

  @Test
  public void jsonFusionerTest() throws Exception {

    Path fusionedJson = Files.createTempFile("package", ".json");

    String[] addTags = { "/keywords", "/dependencies", "/peerDependencies" };
    String[] replaceTags = { "/repository", "/bugs" };

    JsonFusioner fusioner = new JsonFusioner(
        PathUtils.getPathInClasspath("/customnpm/generated.json"),
        PathUtils.getPathInClasspath("/customnpm/customizer.json"), fusionedJson, addTags,
        replaceTags);

    fusioner.fusionJsons();

    printFile(fusionedJson);

    JsonObject doc = loadJsonFile(fusionedJson);

    // Original properties
    assertTagValue(doc, "/name", "npmName");
    assertTagValue(doc, "/version", "1.0.0");
    assertTagValue(doc, "/description", "xxxxxxx");

    // Add properties
    assertTagValue(doc, "/newProp", "newValue");

    // Replace properties
    assertTagValue(doc, "/homepage", "http://new.home.page");

    // Add dependencies
    assertTagValue(doc, "/dependencies/custom", "2222");

    // Original dependencies
    assertTagValue(doc, "/dependencies/inherits", "^2.0.1");

    // Add dependencies
    assertTagValue(doc, "/peerDependencies/customPeer", "1111");

    // Original dependencies
    assertTagValue(doc, "/peerDependencies/kurento-client", "^5.0.0");

    // Add keywords
    assertTagValue(doc, "/keywords", "NewKeyword");

    // Original keywords
    assertTagValue(doc, "/keywords", "Kurento");

  }

  @Test
  public void test() throws Exception {

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.addKmdFileToGen(PathUtils.getPathInClasspath("/customnpm/moduleA.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakecore.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakeelements.kmd.json"));

    modCreator.addDependencyKmdFile(PathUtils.getPathInClasspath("/fakefilters.kmd.json"));

    modCreator.loadModulesFromKmdFiles();

    Path codeGenDir = Files.createTempDirectory("npm");

    modCreator.setCodeGenDir(codeGenDir);
    modCreator.setGenerateNpmPackage(true);

    Result result = modCreator.generateCode();

    assertThat("Compilation error: " + result.getErrors(), result.isSuccess(), is(true));

    Path packageFile = codeGenDir.resolve("package.json");

    assertThat("The package.json should exist", Files.exists(packageFile), is(true));

    printFile(packageFile);

    JsonObject doc = loadJsonFile(packageFile);

    // Original properties
    assertTagValue(doc, "/name", "kurento-module-moduleA");
    assertTagValue(doc, "/version", "1.0.0-dev");
    assertTagValue(doc, "/description", "");

    // Add properties
    assertTagValue(doc, "/newProp", "newValue");

    // Replace properties
    assertTagValue(doc, "/homepage", "http://new.home.page");

    // Add dependencies
    assertTagValue(doc, "/dependencies/custom", "2222");

    // Original dependencies
    assertTagValue(doc, "/dependencies/inherits", "^2.0.1");

    // Add dependencies
    assertTagValue(doc, "/peerDependencies/customPeer", "1111");

    // Add keywords
    assertTagValue(doc, "/keywords", "NewKeyword");

    // Original keywords
    assertTagValue(doc, "/keywords", "Kurento");
  }

  private void findJsonElementsPath(JsonObject node, String[] propertyName,
      List<JsonElement> list) {

    for (Entry<String, JsonElement> entry : node.entrySet()) {
      if (entry.getKey().equals(propertyName[0])) {

        if (propertyName.length == 1) {
          list.add(entry.getValue());
        } else {
          JsonElement elem = entry.getValue();
          if (elem instanceof JsonObject) {
            findJsonElementsPath((JsonObject) elem,
                Arrays.copyOfRange(propertyName, 1, propertyName.length), list);
          }
        }
      }
    }
  }

  private void findJsonElementsProp(JsonObject node, String propertyName, List<JsonElement> list) {

    for (Entry<String, JsonElement> entry : node.entrySet()) {
      if (entry.getKey().equals(propertyName)) {
        list.add(entry.getValue());
      } else {
        JsonElement elem = entry.getValue();
        if (elem instanceof JsonObject) {
          findJsonElementsProp((JsonObject) elem, propertyName, list);
        }
      }
    }
  }

  private void assertTagValue(JsonObject node, String propertyName, String value)
      throws XPathExpressionException {

    List<JsonElement> list = new ArrayList<>();

    if (propertyName.startsWith("/")) {
      findJsonElementsPath(node, propertyName.substring(1).split("/"), list);
    } else {
      findJsonElementsProp(node, propertyName, list);
    }

    if (list.isEmpty()) {
      fail("Property '" + propertyName + "' no found in the document");

    } else {

      for (JsonElement elem : list) {

        if (elem instanceof JsonPrimitive) {

          if (value.equals(((JsonPrimitive) elem).getAsString())) {
            return;
          }

        } else if (elem instanceof JsonArray) {

          JsonArray elemArray = (JsonArray) elem;
          for (JsonElement arrayElem : elemArray) {

            if (arrayElem instanceof JsonPrimitive) {

              if (value.equals(((JsonPrimitive) arrayElem).getAsString())) {
                return;
              }
            }
          }
        }
      }

      fail("There is no tag '" + propertyName + "' with value '" + value + "'");
    }
  }

  private JsonObject loadJsonFile(Path jsonFile)
      throws ParserConfigurationException, SAXException, IOException {

    Gson gson = new GsonBuilder().create();

    return (JsonObject) gson.fromJson(Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8),
        JsonElement.class);
  }

  private void printFile(Path file) throws IOException {
    System.out.println(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
  }

}
