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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.kurento.modulecreator.KurentoModuleCreatorException;

public class ModuleMavenArtifact implements Closeable {

	private static final int EXTENSION_LENGTH = ".kmd.json".length();

	private static final String META_INF_KURENTO_FOLDER = "META-INF/kurento";

	private Log log;

	private Artifact artifact;
	private JarFile jarFile;
	private File artifactFile;
	private FileSystem jarFS;

	private boolean kurentoArtifact = false;
	private Path kmdFile;
	private String moduleName;

	public ModuleMavenArtifact(Log log, Artifact artifact) throws IOException {

		this.log = log;
		this.artifact = artifact;
		this.artifactFile = artifact.getFile();

		if (artifactFile.isFile()) {
			this.jarFile = new JarFile(artifactFile);
			JarEntry entry = jarFile.getJarEntry(META_INF_KURENTO_FOLDER);
			kurentoArtifact = (entry != null);
		} else {
			File kurentoFolder = new File(artifactFile, META_INF_KURENTO_FOLDER);
			kurentoArtifact = kurentoFolder.exists();
		}

		if (kurentoArtifact) {
			loadKmdFiles();
		}
	}

	private void loadKmdFiles() throws IOException {

		Path kmdFolder = null;
		if (artifactFile.isDirectory()) {
			kmdFolder = artifactFile.toPath().resolve(META_INF_KURENTO_FOLDER);
		} else {
			jarFS = FileSystems.newFileSystem(artifactFile.toPath(), null);
			kmdFolder = jarFS.getPath("/" + META_INF_KURENTO_FOLDER);
		}

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				kmdFolder, "*.kmd.json")) {

			log.debug("Exploring dir " + META_INF_KURENTO_FOLDER
					+ " with filter *.kmd.json");

			for (Path kmdFile : directoryStream) {

				if (this.kmdFile == null) {
					this.kmdFile = kmdFile;
				} else {
					throw new KurentoModuleCreatorException(
							"Found two or more kmd files in dependency "
									+ this.getArtifactCoordinate());
				}
			}
		}

		if (kmdFile == null) {
			throw new KurentoModuleCreatorException(
					"Found /META-INF/kurento folder in dependency "
							+ getArtifactCoordinate()
							+ " without .kmd.json file");
		}

		moduleName = removeExtension(kmdFile.getFileName().toString());
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public boolean isKurentoModule() {
		return kurentoArtifact;
	}

	public Path getKmdFile() {
		return kmdFile;
	}

	public String getArtifactCoordinate() {
		return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
				+ artifact.getVersion();
	}

	public String getName() {
		return moduleName;
	}

	private String removeExtension(String dependencyId) {
		return dependencyId.substring(0, dependencyId.length()
				- EXTENSION_LENGTH);
	}

	@Override
	public void close() throws IOException {
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} finally {
			if (jarFS != null) {
				jarFS.close();
			}
		}
	}
}
