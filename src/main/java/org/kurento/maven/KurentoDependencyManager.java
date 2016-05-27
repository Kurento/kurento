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

				log.debug("Exploring dependency: " + artifact);

				ModuleMavenArtifact kurentoArtifact = new ModuleMavenArtifact(
						log, artifact);

				if (kurentoArtifact.isKurentoModule()) {

					log.info("  Found kurento dependency in artifact: "
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
