package org.kurento.ktool.maven;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.kurento.ktool.rom.processor.codegen.KurentoRomProcessorException;

public class KurentoDependencyManager {

	private Map<String, KurentoArtifact> dependencies = new HashMap<>();

	private Log log;

	public KurentoDependencyManager(Log log) {
		this.log = log;
	}

	public Collection<KurentoArtifact> getDependencies() {
		return this.dependencies.values();
	}

	public KurentoArtifact getDependency(String name) {
		return this.dependencies.get(name);
	}

	public void loadDependencies(MavenProject project)
			throws MojoExecutionException {

		log.info("Searching for kurento dependencies:");

		for (Object artObj : project.getArtifacts()) {

			try {

				Artifact artifact = (Artifact) artObj;

				log.info("Exploring dependency: " + artifact);

				KurentoArtifact kurentoArtifact = new KurentoArtifact(log,
						artifact);

				if (kurentoArtifact.isKurentoArtifact()) {

					log.info("  Found kurento dependency "
							+ kurentoArtifact.getArtifactCoordinate());

					addKurentoArtifact(kurentoArtifact);
				}

			} catch (IOException e) {
				throw new MojoExecutionException(
						"Exception accessing to dependencies", e);
			}
		}
	}

	private void addKurentoArtifact(KurentoArtifact kurentoArtifact) {

		String moduleName = kurentoArtifact.getName();

		if (!dependencies.containsKey(moduleName)) {
			dependencies.put(moduleName, kurentoArtifact);
		} else {
			throw new KurentoRomProcessorException("Dependency "
					+ kurentoArtifact.getArtifactCoordinate()
					+ " has the same module '" + moduleName
					+ "' that other dependency");
		}
	}
}
