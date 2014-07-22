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

	private Map<String, KurentoModuleArtifact> dependencies = new HashMap<>();

	private Log log;

	public KurentoDependencyManager(Log log) {
		this.log = log;
	}

	public Collection<KurentoModuleArtifact> getDependencies() {
		return this.dependencies.values();
	}

	public KurentoModuleArtifact getDependency(String name) {
		return this.dependencies.get(name);
	}

	public void loadDependencies(MavenProject project)
			throws MojoExecutionException {

		log.info("Searching for kurento dependencies:");

		for (Object artObj : project.getArtifacts()) {

			try {

				Artifact artifact = (Artifact) artObj;

				log.info("Exploring dependency: " + artifact);

				KurentoModuleArtifact kurentoArtifact = new KurentoModuleArtifact(log,
						artifact);

				if (kurentoArtifact.isKurentoModule()) {

					log.info("  Found kurento dependency "
							+ kurentoArtifact.getArtifactCoordinate());

					addKurentoModuleArtifact(kurentoArtifact);
				}

			} catch (IOException e) {
				throw new MojoExecutionException(
						"Exception accessing to dependencies", e);
			}
		}
	}

	private void addKurentoModuleArtifact(KurentoModuleArtifact kurentoArtifact) {

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
