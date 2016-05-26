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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonIOException;

public class KurentoCodeGeneration {

  private static final String GLOBAL_FUSIONED_KMDS_PATH = "/tmp/kurento-module-creator/kmds";
  private static final String KMS_PROJECTS_PATH = "/home/mica/Data/Kurento/git.kms";

  private static final Logger log = LoggerFactory.getLogger(KurentoCodeGeneration.class);

  public static void main(String[] args) throws JsonIOException, IOException {

    // updateGitProjects();

    // generateJavaCode();

    generateJavaScriptCode();

  }

  private static void generateJavaScriptCode() throws JsonIOException, IOException {

    generateJavaScriptCodeFor("kms-core");
    generateJavaScriptCodeFor("kms-elements");
    generateJavaScriptCodeFor("kms-filters");
    generateJavaScriptCodeFor("kms-platedetector");
    generateJavaScriptCodeFor("kms-pointerdetector");
    generateJavaScriptCodeFor("kms-crowddetector");
    generateJavaScriptCodeFor("kms-chroma");
    generateJavaScriptCodeFor("kms-example");
  }

  // private static void generateJavaCode() throws IOException {
  // generateJavaCodeFor("kms-core");
  // generateJavaCodeFor("kms-elements");
  // generateJavaCodeFor("kms-filters");
  //
  // mvnInstall("/home/mica/Data/Kurento/git/kurento-module-creator");
  // mvnInstall("/home/mica/Data/Kurento/git/kurento-maven-plugin");
  //
  // mvnInstall("/home/mica/Data/Kurento/git/kurento-java/kurento-client");
  //
  // generateJavaCodeFor("kms-platedetector");
  // generateJavaCodeFor("kms-pointerdetector");
  // generateJavaCodeFor("kms-crowddetector");
  // generateJavaCodeFor("kms-chroma");
  // generateJavaCodeFor("kms-example");
  // }

  // private static void updateGitProjects() throws IOException {
  // updateGitProject("kms-core");
  // updateGitProject("kms-elements");
  // updateGitProject("kms-filters");
  // updateGitProject("kms-platedetector");
  // updateGitProject("kms-pointerdetector");
  // updateGitProject("kms-crowddetector");
  // updateGitProject("kms-chroma");
  // updateGitProject("kms-example");
  // }

  // private static void updateGitProject(String project) throws IOException {
  //
  // execAndGetResult("git fetch origin", getProjectPath(project));
  // execAndGetResult("git reset --hard origin/develop", getProjectPath(project));
  //
  // }

  private static String getProjectPath(String project) {
    return KMS_PROJECTS_PATH + "/" + project;
  }

  private static void generateJavaScriptCodeFor(String project) throws IOException {
    log.info("----------------------------------------------------");
    log.info("  Start Generating JavaScript code for " + project);
    log.info("----------------------------------------------------");

    generateNpmProject(project);
  }

  private static void generateNpmProject(String project) throws IOException {

    String npmProjectFolder = getProjectPath(project) + "/build/js";

    execAndGetResult("rm -R " + npmProjectFolder + "/package.json");
    execAndGetResult("rm -R " + npmProjectFolder + "/src");

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.setKmdFilesToGen(PathUtils.getPaths(
        new String[] { getProjectPath(project) + "/src/server/interface" }, "*.kmd.json"));

    modCreator.setDependencyKmdFiles(
        PathUtils.searchFiles(Paths.get(GLOBAL_FUSIONED_KMDS_PATH), "*.kmd.json"));

    modCreator.setInternalTemplates("npm");
    modCreator.setCodeGenDir(Paths.get(npmProjectFolder));
    modCreator.generateCode();

    modCreator.setInternalTemplates("js");
    modCreator.setCodeGenDir(Paths.get(npmProjectFolder + "/lib/"));
    modCreator.generateCode();

    // Copy fusioned kmd file to /lib/
    Path fusionedKmdFile = Paths.get(npmProjectFolder + "/lib/");
    modCreator.setOutputFile(fusionedKmdFile);
    modCreator.generateCode();

    // Copy fusioned kmd file to /tmp/kurento-module-creator/kmds
    copyFusionedKmdToKmdsFolder(fusionedKmdFile);
  }

  private static void copyFusionedKmdToKmdsFolder(Path fusionedKmdFile) throws IOException {

    Path kmdsPath = Paths.get(GLOBAL_FUSIONED_KMDS_PATH);

    if (!Files.exists(kmdsPath)) {
      Files.createDirectories(kmdsPath);
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(fusionedKmdFile)) {

      for (Path kmdFile : stream) {

        Files.copy(kmdFile, kmdsPath.resolve(kmdFile.getFileName()),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
            java.nio.file.LinkOption.NOFOLLOW_LINKS);
      }
    }
  }

  public static void generateJavaCodeFor(String project) throws JsonIOException, IOException {

    log.info("----------------------------------------------------");
    log.info("  Start Generating Java code for " + project);
    log.info("----------------------------------------------------");

    generateMavenProject(project);
    mvnInstallProject(project);
  }

  private static void mvnInstallProject(String project) throws IOException {
    mvnInstall(getProjectPath(project) + "/build/java");
  }

  private static void mvnInstall(String path) throws IOException {

    log.info("");
    log.info("  MVN INSTALL " + path);
    log.info("  ========================================================");

    execAndGetResult("mvn install -DskipTests", path);
  }

  private static void generateMavenProject(String project) throws IOException {

    String mavenProjectFolder = getProjectPath(project) + "/build/java";

    execAndGetResult("rm -R " + mavenProjectFolder + "/pom.xml");
    execAndGetResult("rm -R " + mavenProjectFolder + "/src");

    KurentoModuleCreator modCreator = new KurentoModuleCreator();

    modCreator.setKmdFilesToGen(PathUtils.getPaths(
        new String[] { getProjectPath(project) + "/src/server/interface" }, "*.kmd.json"));

    modCreator.setDependencyKmdFiles(
        PathUtils.searchFiles(Paths.get(GLOBAL_FUSIONED_KMDS_PATH), "*.kmd.json"));

    modCreator.setInternalTemplates("maven");
    modCreator.setCodeGenDir(Paths.get(mavenProjectFolder));
    modCreator.generateCode();

    if (modCreator.hasToGenerateCode()) {

      modCreator.setInternalTemplates("java");
      modCreator.setCodeGenDir(Paths.get(mavenProjectFolder + "/src/main/java"));
      modCreator.generateCode();

    } else {

      // Copy fusioned kmd file to /META-INF/kurento/
      Path fusionedKmdFile = Paths
          .get(mavenProjectFolder + "/src/main/resources/META-INF/kurento/");
      modCreator.setOutputFile(fusionedKmdFile);
      modCreator.generateCode();

      copyFusionedKmdToKmdsFolder(fusionedKmdFile);
    }
  }

  public static String execAndGetResult(final String command) throws IOException {
    return execAndGetResult(command, null);
  }

  public static String execAndGetResult(final String command, String workDir) throws IOException {

    log.debug("Running command on the shell: {} in {}", command, workDir);

    Process process;

    String[] execCommand = { "sh", "-c", command };

    ProcessBuilder processBuilder = new ProcessBuilder(execCommand).redirectErrorStream(true);

    if (workDir != null) {
      processBuilder.directory(new File(workDir));
    }

    process = processBuilder.start();

    String output = null;
    try (Scanner scanner = new Scanner(process.getInputStream(), StandardCharsets.UTF_8.name())) {
      try {
        output = scanner.useDelimiter("\\A").next();
      } catch (NoSuchElementException e) {
        output = "";
      }
    }

    log.info(output);

    return output;
  }
}
