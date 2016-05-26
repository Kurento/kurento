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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class JsonFusioner {

  private static final Logger log = LoggerFactory.getLogger(JsonFusioner.class);

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
      .create();

  private final Path generatedJson;
  private final Path customizerJson;
  private final Path outputFile;

  private final Set<String> addChildrenTags;
  private final Set<String> replaceChildrenTags;

  public JsonFusioner(Path generatedJson, Path customizerJson, Path outputFile) {
    this(generatedJson, customizerJson, outputFile, null, null);
  }

  public JsonFusioner(Path generatedJson, Path customizerJson, Path outputFile,
      String[] addChildrenTags, String[] replaceChildrenTags) {
    super();
    this.generatedJson = generatedJson;
    this.customizerJson = customizerJson;
    this.outputFile = outputFile;
    this.addChildrenTags = new HashSet<String>(Arrays.asList(addChildrenTags));
    this.replaceChildrenTags = new HashSet<String>(Arrays.asList(replaceChildrenTags));
  }

  public void fusionJsons() throws IOException {

    try {
      JsonObject generatedJsonDoc = loadJson(generatedJson);
      JsonObject customizedJsonDoc = loadJson(customizerJson);

      merge(generatedJsonDoc, customizedJsonDoc, new ArrayList<String>());

      writeJson(generatedJsonDoc);
    } catch (IOException e) {
      log.warn("Error while merging '" + generatedJson + "' with '" + customizerJson + "': "
          + e.getMessage());
    }
  }

  private void merge(JsonObject genNode, JsonObject custNode, List<String> genPath) {

    for (Entry<String, JsonElement> entry : custNode.entrySet()) {

      JsonElement custChildNode = entry.getValue();

      JsonElement genChildNode = genNode.get(entry.getKey());

      if (genChildNode != null) {

        String nodePath = getPath(genPath, entry.getKey());

        if (replaceChildrenTags.contains(nodePath)) {

          if (custChildNode instanceof JsonObject && genChildNode instanceof JsonObject) {

            List<String> newPath = new ArrayList<String>(genPath);
            newPath.add(entry.getKey());

            merge((JsonObject) genChildNode, (JsonObject) custChildNode, newPath);
          }

        } else if (addChildrenTags.contains(nodePath)) {

          addChildren(custChildNode, genChildNode);

        } else if (includedInReplaceOrAdd(nodePath)) {

          if (custChildNode instanceof JsonObject && genChildNode instanceof JsonObject) {

            List<String> newPath = new ArrayList<String>(genPath);
            newPath.add(entry.getKey());

            merge((JsonObject) genChildNode, (JsonObject) custChildNode, newPath);
          }

        } else {

          // Replace entire node
          genNode.add(entry.getKey(), custChildNode);
        }

      } else {

        // Add new node
        genNode.add(entry.getKey(), custChildNode);
      }
    }
  }

  private void addChildren(JsonElement fromElement, JsonElement toElement) {

    if (fromElement instanceof JsonObject && toElement instanceof JsonObject) {

      JsonObject fromObject = (JsonObject) fromElement;
      JsonObject toObject = (JsonObject) toElement;

      for (Entry<String, JsonElement> entry : fromObject.entrySet()) {
        toObject.add(entry.getKey(), entry.getValue());
      }

    } else if (fromElement instanceof JsonArray && toElement instanceof JsonArray) {

      JsonArray fromArray = (JsonArray) fromElement;
      JsonArray toArray = (JsonArray) toElement;

      toArray.addAll(fromArray);
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

  private String getPath(List<String> path, String propName) {

    StringBuilder sb = new StringBuilder("/");
    for (String prop : path) {
      sb.append(prop).append("/");
    }
    sb.append(propName);
    return sb.toString();
  }

  private JsonObject loadJson(Path jsonPath)
      throws JsonSyntaxException, JsonIOException, IOException {

    return (JsonObject) gson.fromJson(Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8),
        JsonElement.class);
  }

  private void writeJson(JsonObject doc) throws IOException {

    String json = gson.toJson(doc);
    try (Writer os = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
      os.write(json);
    }
  }
}
