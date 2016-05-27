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
package org.kurento.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;
import org.kurento.modulecreator.codegen.Error;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Parses Kurento Media Elements Definition (*.kmd.json) and transforms them
 * into Java source files.
 *
 * @author micael.gallego@gmail.com
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "generate-kurento-client", requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class GenerateKurentoJavaClientMojo extends AbstractMojo {

	private static final String KURENTO_CLIENT_GROUP_ID = "org.kurento";

	private static final String KURENTO_CLIENT_ARTIFACT_ID = "kurento-client";

	private static final String TEMPLATES_FOLDER = "templates";

	/**
	 * The directory where the Kurento Media Element Definition files (
	 * {@code *.kmd.json}) are located.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/kmd")
	private File sourceDirectory;

	/**
	 * Specify output directory where the Java files are generated.
	 */
	@Parameter(readonly = true, defaultValue = "${project.build.directory}/generated-sources/kmd")
	protected File generatedSourceOutputFolder;

	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/kurento", readonly = true)
	private File kmdOutputFolder;

	protected Log log;

	/**
	 * specify kurento media element definition file encoding; e.g., euc-jp
	 */
	@Parameter(property = "project.build.sourceEncoding")
	protected String encoding;

	@Component
	private BuildContext buildContext;

	/**
	 * The current Maven project.
	 */
	@Parameter(property = "project", required = true, readonly = true)
	protected MavenProject project;

	@Parameter
	private List<String> generateCodeForModules = Collections.emptyList();

	protected void addKmdFiles(KurentoModuleCreator krp, Set<File> kmdFiles,
			KurentoDependencyManager manager) {

		log.info("Preparing code generation tool:");

		for (File kmdFile : kmdFiles) {
			getLog().info("  Adding kmd file to generate code: " + kmdFile);
			krp.addKmdFileToGen(kmdFile.toPath());
		}

		for (String moduleToGenerateCode : this.generateCodeForModules) {
			if (manager.getDependency(moduleToGenerateCode) == null) {
				throw new KurentoModuleCreatorException(
						"The module to generate code '" + moduleToGenerateCode
								+ "' doesn't exist in dependencies");
			}
		}

		for (ModuleMavenArtifact dependency : manager.getDependencies()) {

			Path kmdFile = dependency.getKmdFile();
			if (!this.generateCodeForModules.contains(dependency.getName())) {
				getLog().info("  Adding dependency kmd file: " + kmdFile);
				krp.addDependencyKmdFile(kmdFile);
			} else {
				getLog().info("  Adding kmd file to generate code: " + kmdFile);
				krp.addDependencyKmdFileToGen(kmdFile);
			}
		}
	}

	protected void copyKmdFiles(Set<File> kmdFiles, File kurentoOutputFolder)
			throws MojoFailureException {

		try {
			Path outputPath = kurentoOutputFolder.toPath();

			if (Files.exists(outputPath)) {
				PathUtils.deleteRecursive(outputPath);
			}

			if (!kmdFiles.isEmpty()) {
				Files.createDirectories(outputPath);
			}

			for (File kmdFile : kmdFiles) {

				Path kmdPath = kmdFile.toPath();
				String kmdFileName = kmdPath.getFileName().toString();
				Path newFile = outputPath.resolve(kmdFileName);
				Files.copy(kmdPath, newFile,
						StandardCopyOption.REPLACE_EXISTING);
				buildContext.refresh(newFile.toFile());
			}
		} catch (IOException e) {
			throw new MojoFailureException("Exception copying kmd files", e);
		}
	}

	protected void prepareOutputDirectories(File outputDirectory,
			File kurentoOutputFolder) {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		if (!kurentoOutputFolder.exists()) {
			kurentoOutputFolder.mkdirs();
		}
	}

	protected Set<File> loadKmdFiles(File sourceDirectory)
			throws MojoFailureException {

		if (!sourceDirectory.isDirectory()) {
			getLog().info(
					"The folder for Kurento Media Element Definition files (*.kmd.json) is \""
							+ sourceDirectory.getAbsolutePath()
							+ "\", but it doesn't exist");

			return Collections.emptySet();

		} else {

			getLog().info(
					"Searching for kmd files in "
							+ sourceDirectory.getAbsolutePath());
			try {

				// Which files under the source set should we be looking for as
				// kmd
				// files
				SourceMapping mapping = new SuffixMapping("kmd.json",
						Collections.<String> emptySet());

				SourceInclusionScanner scan = new SimpleSourceInclusionScanner(
						Collections.singleton("**/*.kmd.json"),
						Collections.<String> emptySet());

				scan.addSourceMapping(mapping);

				Set<File> kmdFiles = scan.getIncludedSources(sourceDirectory,
						null);

				if (kmdFiles.isEmpty()) {
					getLog().info("No kmd files to process in the project");

				} else {

					for (File kmdFile : kmdFiles) {
						getLog().info(
								"  Found kmd file to generate code: " + kmdFile);
					}
				}

				return kmdFiles;

			} catch (InclusionScanException e) {
				throw new MojoFailureException("Exception loading kmd files", e);
			}
		}
	}

	private Path loadTemplatesPath(MavenProject project)
			throws MojoExecutionException, IOException {

		log.info("Searching for kurento dependencies:");

		if (KURENTO_CLIENT_ARTIFACT_ID.equals(project.getArtifactId())
				&& KURENTO_CLIENT_GROUP_ID.equals(project.getGroupId())) {
			return loadTemplatesPathFromKurentoClient(project.getFile());
		}

		for (Object artObj : project.getArtifacts()) {

			Artifact artifact = (Artifact) artObj;

			log.debug("Exploring dependency: " + artifact);

			if (KURENTO_CLIENT_ARTIFACT_ID.equals(artifact.getArtifactId())
					&& KURENTO_CLIENT_GROUP_ID.equals(artifact.getGroupId())) {

				return loadTemplatesPathFromKurentoClient(artifact.getFile());
			}
		}

		return null;
	}

	private Path loadTemplatesPathFromKurentoClient(File artifactFile)
			throws IOException {

		if ("pom.xml".equals(artifactFile.getName())) {

			return artifactFile.toPath().getParent().resolve("src")
					.resolve("main").resolve("resources")
					.resolve(TEMPLATES_FOLDER);

		} else if (artifactFile.isFile()) {

			return FileSystems.newFileSystem(artifactFile.toPath(), null)
					.getPath("/" + TEMPLATES_FOLDER);

		} else {

			return artifactFile.toPath().resolve(TEMPLATES_FOLDER);
		}
	}

	protected void executeKurentoMavenPlugin(File sourceDirectory,
			File generatedSourceOutputFolder, File kmdOutputFolder)
			throws MojoFailureException, MojoExecutionException {

		log = getLog();

		Set<File> kmdFiles = loadKmdFiles(sourceDirectory);

		try {

			KurentoDependencyManager manager = new KurentoDependencyManager(log);

			manager.loadDependencies(project);

			KurentoModuleCreator krp = new KurentoModuleCreator();
			addKmdFiles(krp, kmdFiles, manager);
			krp.loadModulesFromKmdFiles();

			if (krp.hasToGenerateCode()) {

				krp.setDeleteGenDir(true);
				krp.setVerbose(false);
				Path templatesPath = loadTemplatesPath(project);

				log.info("Templates path: " + templatesPath);

				krp.setTemplatesDir(templatesPath);
				krp.setCodeGenDir(generatedSourceOutputFolder.toPath());
				krp.setListGeneratedFiles(false);

				prepareOutputDirectories(generatedSourceOutputFolder,
						kmdOutputFolder);

				Result result = krp.generateCode();

				if (result.isSuccess()) {
					getLog().info("Generation success");
				} else {
					getLog().error("Generation failed");

					getLog().error("Errors:");
					for (Error error : result.getErrors()) {
						getLog().error(error.toString());
					}

					throw new MojoExecutionException(
							"Kurento Rom Processor found errors: "
									+ result.getErrors());
				}

				project.addCompileSourceRoot(generatedSourceOutputFolder
						.getPath());
			}

		} catch (MojoExecutionException e) {
			throw e;

		} catch (Exception e) {
			log.error(
					"Exception "
							+ e.getClass().getName()
							+ ":"
							+ e.getMessage()
							+ " in code generation from kmd files. See exception report for details",
					e);
			throw new MojoFailureException(
					"Exception in code generation from kmd files. See exception report for details",
					e);
		}

		copyKmdFiles(kmdFiles, kmdOutputFolder);
	}

	/**
	 * The main entry point for this Mojo, it is responsible for converting
	 * Kurento Module Descriptions (kmd) into the Java code used by
	 * kurento-client users.
	 *
	 * @exception MojoExecutionException
	 *                if a configuration or definition error causes the code
	 *                generation process to fail
	 * @exception MojoFailureException
	 *                if an instance of the Kurento Maven Plugin cannot be
	 *                created
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		executeKurentoMavenPlugin(sourceDirectory, generatedSourceOutputFolder,
				kmdOutputFolder);
	}
}