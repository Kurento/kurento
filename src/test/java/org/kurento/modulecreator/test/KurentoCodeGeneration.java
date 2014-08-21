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

	private static final Logger log = LoggerFactory
			.getLogger(KurentoCodeGeneration.class);

	public static void main(String[] args) throws JsonIOException, IOException {

		// updateGitProjects();

		generateCodeFor("kms-core");
		generateCodeFor("kms-elements");
		generateCodeFor("kms-filters");

		mvnInstall("/home/mica/Data/Kurento/git/kurento-module-creator");
		mvnInstall("/home/mica/Data/Kurento/git/kurento-maven-plugin");

		mvnInstall("/home/mica/Data/Kurento/git/kurento-java/kurento-client");

		generateCodeFor("kms-platedetector");
		generateCodeFor("kms-pointerdetector");
		generateCodeFor("kms-crowddetector");
		generateCodeFor("kms-chroma");
		generateCodeFor("kms-example");

	}

	private static void updateGitProjects() throws IOException {
		updateGitProject("kms-core");
		updateGitProject("kms-elements");
		updateGitProject("kms-filters");
		updateGitProject("kms-platedetector");
		updateGitProject("kms-pointerdetector");
		updateGitProject("kms-crowddetector");
		updateGitProject("kms-chroma");
		updateGitProject("kms-example");
	}

	private static void updateGitProject(String project) throws IOException {

		execAndGetResult("git fetch origin", getProjectPath(project));
		execAndGetResult("git reset --hard origin/develop",
				getProjectPath(project));

	}

	private static String getProjectPath(String project) {
		return KMS_PROJECTS_PATH + "/" + project;
	}

	public static void generateCodeFor(String project) throws JsonIOException,
			IOException {

		log.info("----------------------------------------------------");
		log.info("  Generate code for " + project);
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
				new String[] { getProjectPath(project)
						+ "/src/server/interface" }, "*.kmd.json"));

		modCreator.setDependencyKmdFiles(PathUtils.searchFiles(
				Paths.get(GLOBAL_FUSIONED_KMDS_PATH), "*.kmd.json"));

		modCreator.setInternalTemplates("maven");

		modCreator.setCodeGenDir(Paths.get(mavenProjectFolder));

		modCreator.generateCode();

		if (modCreator.hasToGenerateCode()) {

			modCreator.setInternalTemplates("java");

			modCreator.setCodeGenDir(Paths.get(mavenProjectFolder
					+ "/src/main/java"));

			modCreator.generateCode();

		} else {

			// Copy fusioned kmd file to /META-INF/kurento/
			Path fusionedKmdFile = Paths.get(mavenProjectFolder
					+ "/src/main/resources/META-INF/kurento/");

			modCreator.setOutputFile(fusionedKmdFile);

			modCreator.generateCode();

			// Copy fusioned kmd file to /tmp/kurento-module-creator/kmds
			Path kmdsPath = Paths.get(GLOBAL_FUSIONED_KMDS_PATH);

			if (!Files.exists(kmdsPath)) {
				Files.createDirectories(kmdsPath);
			}

			try (DirectoryStream<Path> stream = Files
					.newDirectoryStream(fusionedKmdFile)) {

				for (Path kmdFile : stream) {

					Files.copy(kmdFile,
							kmdsPath.resolve(kmdFile.getFileName()),
							java.nio.file.StandardCopyOption.REPLACE_EXISTING,
							java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
							java.nio.file.LinkOption.NOFOLLOW_LINKS);
				}
			}
		}
	}

	public static String execAndGetResult(final String command)
			throws IOException {
		return execAndGetResult(command, null);
	}

	public static String execAndGetResult(final String command, String workDir)
			throws IOException {

		log.debug("Running command on the shell: {} in {}", command, workDir);

		Process p;

		String[] execCommand = { "sh", "-c", command };

		ProcessBuilder processBuilder = new ProcessBuilder(execCommand)
				.redirectErrorStream(true);

		if (workDir != null) {
			processBuilder.directory(new File(workDir));
		}

		p = processBuilder.start();

		String output = null;
		try (Scanner scanner = new Scanner(p.getInputStream(),
				StandardCharsets.UTF_8.name())) {
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
