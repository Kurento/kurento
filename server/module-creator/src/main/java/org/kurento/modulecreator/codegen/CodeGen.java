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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.kurento.modulecreator.codegen.function.CamelToUnderscore;
import org.kurento.modulecreator.codegen.function.CppObjectType;
import org.kurento.modulecreator.codegen.function.EscapeString;
import org.kurento.modulecreator.codegen.function.GenerateKurentoClientJsVersion;
import org.kurento.modulecreator.codegen.function.InitializePropertiesValues;
import org.kurento.modulecreator.codegen.function.IsFirstConstructorParam;
import org.kurento.modulecreator.codegen.function.JavaObjectType;
import org.kurento.modulecreator.codegen.function.JsNamespace;
import org.kurento.modulecreator.codegen.function.JsonCppTypeData;
import org.kurento.modulecreator.codegen.function.OrganizeDependencies;
import org.kurento.modulecreator.codegen.function.PackageToFolder;
import org.kurento.modulecreator.codegen.function.SphinxLinks;
import org.kurento.modulecreator.codegen.function.TypeDependencies;
import org.kurento.modulecreator.codegen.function.TypeHierarchy;
import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.Type;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class CodeGen {

  private Path templatesFolder;
  private Configuration cfg;

  private final Path outputFolder;

  private final boolean listGeneratedFiles;
  private final boolean verbose;
  private final boolean overwrite;
  private final JsonObject config;

  public CodeGen(Path templatesFolder, Path outputFolder, boolean verbose,
      boolean listGeneratedFiles, boolean overwrite, JsonObject config) throws IOException {

    this.verbose = verbose;
    this.listGeneratedFiles = listGeneratedFiles;
    this.overwrite = overwrite;
    this.outputFolder = outputFolder;
    this.config = config;

    if (templatesFolder != null) {
      setTemplatesDir(templatesFolder);
    }
  }

  public void setTemplatesDir(Path templatesFolder) throws IOException {
    this.templatesFolder = templatesFolder;

    cfg = new Configuration();

    // Specify the data source where the template files come from. Here I
    // set a
    // plain directory for it, but non-file-system are possible too:
    cfg.setTemplateLoader(new PathTemplateLoader(templatesFolder));

    // Specify how templates will see the data-model. This is an advanced
    // topic...
    // for now just use this:
    cfg.setObjectWrapper(new DefaultObjectWrapper());

    // Set your preferred charset template files are stored in. UTF-8 is
    // a good choice in most applications:
    cfg.setDefaultEncoding("UTF-8");

    // Sets how errors will appear. Here we assume we are developing HTML
    // pages.
    // For production systems TemplateExceptionHandler.RETHROW_HANDLER is
    // better.
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

    // At least in new projects, specify that you want the fixes that aren't
    // 100% backward compatible too (these are very low-risk changes as far
    // as the
    // 1st and 2nd version number remains):
    // cfg.setIncompatibleImprovements(new Version(2, 3, 19)); // FreeMarker
    // 2.3.19
  }

  public void generateCode(ModuleDefinition module) throws IOException, TemplateException {

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(templatesFolder,
        "*.ftl")) {

      for (Path path : directoryStream) {
        String name = path.getFileName().toString();
        String templateType = name.split("_")[0];

        generateCode(name, module, templateType);
      }
    }
  }

  private void generateCode(String templateName, ModuleDefinition module, String templateType)
      throws TemplateException, IOException {

    Template temp = cfg.getTemplate(templateName);

    List<? extends Type> types;
    if (templateType.equals("remoteClass")) {
      types = module.getRemoteClasses();
    } else if (templateType.equals("complexType")) {
      types = module.getComplexTypes();
    } else if (templateType.equals("event")) {
      types = module.getEvents();
    } else if (templateType.equals("model")) {
      types = null;
    } else {
      throw new RuntimeException("Unknown template type: '" + templateType
          + "'. It should be 'model', 'remoteClass', 'complexType' or 'event'");
    }

    Map<String, Object> root = new HashMap<String, Object>();
    root.put("getJavaObjectType", new JavaObjectType());
    root.put("getCppObjectType", new CppObjectType());
    root.put("getJsonCppTypeData", new JsonCppTypeData());
    root.put("escapeString", new EscapeString());
    root.put("camelToUnderscore", new CamelToUnderscore());
    root.put("typeDependencies", new TypeDependencies());
    root.put("isFirstConstructorParam", new IsFirstConstructorParam());
    root.put("sphinxLinks", new SphinxLinks(module));
    root.put("getJsNamespace", new JsNamespace());
    root.put("packageToFolder", new PackageToFolder());
    root.put("organizeDependencies", new OrganizeDependencies());
    root.put("initializePropertiesValues", new InitializePropertiesValues());
    root.put("generateKurentoClientJsVersion", new GenerateKurentoClientJsVersion());
    root.put("typeHierarchy", new TypeHierarchy());

    root.put("module", module);
    if (this.config != null) {
      JsonObjectAsMap mapper = new JsonObjectAsMap();
      root.put("config", mapper.createMapFromJsonObject(config));
    } else {
      root.put("config", Collections.emptyMap());
    }

    if (types == null) {
      generateFile(temp, root);
    } else {

      for (Type type : types) {

        if (templateType.equals("remoteClass")) {
          root.put("remoteClass", type);
        } else if (templateType.equals("complexType")) {
          root.put("complexType", type);
        } else if (templateType.equals("event")) {
          root.put("event", type);
        }

        generateFile(temp, root);
      }
    }
  }

  private void generateFile(Template temp, Map<String, Object> root)
      throws TemplateException, IOException {

    StringWriter out = new StringWriter();
    temp.process(root, out);
    String tempOutput = out.toString();

    if (tempOutput.isEmpty()) {
      System.out.println(
          "No file generation because applying template '" + temp.getName() + "' is empty");
      return;
    }

    StringTokenizer st = new StringTokenizer(tempOutput);

    String fileName = st.nextToken();

    File outputFile = new File(outputFolder.toFile(), fileName);

    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs();
    }

    String sourceCode = tempOutput.substring(fileName.length() + 1, tempOutput.length());

    boolean generateFile = !outputFile.exists();
    if (outputFile.exists() && overwrite) {
      generateFile = true;
      String oldContent = readFile(outputFile);

      if (oldContent.equals(sourceCode)) {
        generateFile = false;
      }
    }

    if (generateFile) {
      Writer writer = new FileWriter(outputFile);
      writer.write(sourceCode);
      writer.close();
    }

    if (verbose) {
      System.out.println("File: " + fileName);
      System.out.println();
      System.out.println(sourceCode);
      System.out.println("---------------------------------------");
    }

    if (listGeneratedFiles) {
      System.out.print("Processed file:\t" + fileName);
      if (!generateFile) {
        System.out.println("\t(not generated)");
      } else {
        System.out.println();
      }
    }
  }

  public static String readFile(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8.name());
  }

  public void generateMavenPom(ModuleDefinition module, Path templatePomXml) throws IOException,
      TemplateException, ParserConfigurationException, SAXException, TransformerException {

    this.generateCode(module);

    if (templatePomXml != null) {

      String[] addTags = { "/dependencies", "/build/plugins" };
      String[] replaceTags = { "/properties" };

      Path outputPomXml = outputFolder.resolve("pom.xml");

      XmlFusioner fusioner = new XmlFusioner(outputPomXml, templatePomXml, outputPomXml, addTags,
          replaceTags);

      fusioner.fusionXmls();
    }
  }

  public void generateNpmPackage(ModuleDefinition module, Path templatePackJson,
      Path templateBowerJson) throws IOException, TemplateException, ParserConfigurationException,
          SAXException, TransformerException {

    this.generateCode(module);

    if (templatePackJson != null) {

      String[] addTags = { "/keywords", "/dependencies", "/devDependencies", "/peerDependencies" };
      String[] replaceTags = { "/repository", "/bugs" };

      Path outputPackJson = outputFolder.resolve("package.json");

      JsonFusioner fusioner = new JsonFusioner(outputPackJson, templatePackJson, outputPackJson,
          addTags, replaceTags);

      fusioner.fusionJsons();
    }

    if (templateBowerJson != null) {

      String[] addTags = { "/keywords", "/dependencies", "/peerDependencies" };
      String[] replaceTags = { "/repository", "/bugs" };

      Path outputPackJson = outputFolder.resolve("bower.json");

      JsonFusioner fusioner = new JsonFusioner(outputPackJson, templateBowerJson, outputPackJson,
          addTags, replaceTags);

      fusioner.fusionJsons();
    }
  }
}
