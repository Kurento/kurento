package org.kurento.ktool.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class KurentoDependencyManager {

	private static final int EXTENSION_LENGTH = ".kmd.json".length();

	public static class KurentoDependency {

		private String dependencyId;
		private String packageName = null;
		private Path kmdFile;

		public KurentoDependency(String dependencyId) {
			this.dependencyId = dependencyId;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public String getId() {
			return dependencyId;
		}

		public boolean isGeneratedSources() {
			return packageName != null;
		}

		public String getPackageName() {
			return packageName;
		}

		public Path getKmdFile() {
			return kmdFile;
		}

		public void setKmdFile(Path kmdFile) {
			this.kmdFile = kmdFile;
		}
	}

	private Map<String, KurentoDependency> dependencies = new HashMap<>();

	private Log log;

	public KurentoDependencyManager(Log log) {
		this.log = log;
	}

	public void addKurentoArtifact(KurentoArtifact kurentoArtifact) {

		for (Path kmdFile : kurentoArtifact.getKmdFiles()) {

			String dependencyId = kmdFile.getFileName().toString();

			dependencyId = removeExtension(dependencyId);

			KurentoDependency info = dependencies.get(dependencyId);
			if (info == null) {
				info = new KurentoDependency(dependencyId);
				dependencies.put(dependencyId, info);
			}
			info.setKmdFile(kmdFile);
		}

		for (GeneratedSourcesInfo generatedSourcesInfo : kurentoArtifact
				.getKmdGeneratedSourcesList()) {

			KurentoDependency info = dependencies.get(generatedSourcesInfo
					.getId());
			if (info == null) {
				info = new KurentoDependency(generatedSourcesInfo.getId());
				dependencies.put(generatedSourcesInfo.getId(), info);
			}

			info.setPackageName(generatedSourcesInfo.getPackageName());
		}
	}

	private String removeExtension(String dependencyId) {
		return dependencyId.substring(0, dependencyId.length()
				- EXTENSION_LENGTH);
	}

	public Collection<KurentoDependency> getKmdDependencyInfos() {
		return this.dependencies.values();
	}

	public void loadDependencies(MavenProject project)
			throws MojoExecutionException {

		log.info("Searching for kurento dependencies:");

		for (Object artObj : project.getArtifacts()) {

			try {

				Artifact artifact = (Artifact) artObj;

				log.debug("Exploring dependency: " + artifact);

				KurentoArtifact kurentoArtifact = new KurentoArtifact(log,
						artifact);

				if (kurentoArtifact.isKurentoArtifact()) {

					log.info("  Found kurento dependency "
							+ kurentoArtifact.getArtifactCoordinate()
							+ (kurentoArtifact.hasCode() ? " (with code)" : ""));

					addKurentoArtifact(kurentoArtifact);
				}

			} catch (IOException e) {
				throw new MojoExecutionException(
						"Exception accessing to dependencies", e);
			}
		}
	}
}
