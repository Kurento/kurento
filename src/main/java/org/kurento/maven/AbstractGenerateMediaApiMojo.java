package org.kurento.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.kurento.modulecreator.codegen.Error;
import com.kurento.modulecreator.codegen.KurentoRomProcessor;
import com.kurento.modulecreator.codegen.KurentoRomProcessorException;
import com.kurento.modulecreator.codegen.PathUtils;
import com.kurento.modulecreator.codegen.Result;

public abstract class AbstractGenerateMediaApiMojo extends AbstractMojo {

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

	protected void addKmdFiles(KurentoRomProcessor krp, Set<File> kmdFiles,
			KurentoDependencyManager manager) {

		log.info("Preparing code generation tool:");

		for (File kmdFile : kmdFiles) {
			getLog().info("  Adding kmd file to generate code: " + kmdFile);
			krp.addKmdFileToGen(kmdFile.toPath());
		}

		for (String moduleToGenerateCode : this.generateCodeForModules) {
			if (manager.getDependency(moduleToGenerateCode) == null) {
				throw new KurentoRomProcessorException(
						"The module to generate code '" + moduleToGenerateCode
								+ "' doesn't exist in dependencies");
			}
		}

		for (KurentoModuleArtifact dependency : manager.getDependencies()) {

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

	protected void executeKurentoMavenPlugin(String templates,
			File sourceDirectory, File generatedSourceOutputFolder,
			File kmdOutputFolder) throws MojoFailureException,
			MojoExecutionException {

		log = getLog();

		Set<File> kmdFiles = loadKmdFiles(sourceDirectory);

		try {

			KurentoDependencyManager manager = new KurentoDependencyManager(log);

			manager.loadDependencies(project);

			KurentoRomProcessor krp = new KurentoRomProcessor();
			addKmdFiles(krp, kmdFiles, manager);
			krp.loadModulesFromKmdFiles();

			if (krp.hasToGenerateCode()) {

				krp.setDeleteGenDir(true);
				krp.setVerbose(false);
				krp.setInternalTemplates(templates);
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

}