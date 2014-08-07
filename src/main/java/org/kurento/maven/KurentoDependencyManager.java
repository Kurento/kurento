package org.kurento.maven;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.kurento.modulecreator.KurentoModuleCreatorException;

public class KurentoDependencyManager {

	private Map<String, ModuleMavenArtifact> dependencies = new HashMap<>();

	private Log log;

	public KurentoDependencyManager(Log log) {
		this.log = log;
	}

	public Collection<ModuleMavenArtifact> getDependencies() {
		return this.dependencies.values();
	}

	public ModuleMavenArtifact getDependency(String name) {
		return this.dependencies.get(name);
	}

	public void loadDependencies(MavenProject project)
			throws MojoExecutionException {

		log.info("Searching for kurento dependencies:");

		for (Object artObj : project.getArtifacts()) {

			try {

				Artifact artifact = (Artifact) artObj;

				log.info("Exploring dependency: " + artifact);

				ModuleMavenArtifact kurentoArtifact = new ModuleMavenArtifact(
						log, artifact);

				if (kurentoArtifact.isKurentoModule()) {

					log.info("  Found kurento dependency "
							+ kurentoArtifact.getArtifactCoordinate());

					addModuleMavenArtifact(kurentoArtifact);
				}

			} catch (IOException e) {
				throw new MojoExecutionException(
						"Exception accessing to dependencies", e);
			}
		}
	}

	private void addModuleMavenArtifact(ModuleMavenArtifact moduleMavenArtifact) {

		String moduleName = moduleMavenArtifact.getName();

		if (!dependencies.containsKey(moduleName)) {
			dependencies.put(moduleName, moduleMavenArtifact);
		} else {
			throw new KurentoModuleCreatorException("Dependency "
					+ moduleMavenArtifact.getArtifactCoordinate()
					+ " has the same module '" + moduleName
					+ "' that other dependency");
		}
	}
}
