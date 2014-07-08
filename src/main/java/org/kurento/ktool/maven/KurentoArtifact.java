package org.kurento.ktool.maven;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KurentoArtifact implements Closeable {

	private static final String MANIFEST_FILE = "/manifest.json";
	private static final String GENERATED_SOURCES_LIST_PROPERTY = "generated-sources";
	private static final String META_INF_KURENTO_FOLDER = "META-INF/kurento";

	private static Gson gson = new GsonBuilder().create();

	private Log log;

	private Artifact artifact;
	private JarFile jarFile;
	private boolean kurentoArtifact = false;
	private JsonObject manifest;
	private File artifactFile;
	private List<Path> kmdFiles;
	private FileSystem jarFS;
	private List<GeneratedSourcesInfo> kmdGeneratedSourcesList = new ArrayList<>();

	public KurentoArtifact(Log log, Artifact artifact) throws IOException {

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
			loadManifest();
			loadKmdFiles();
		}
	}

	private void loadKmdFiles() throws IOException {

		kmdFiles = new ArrayList<>();

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
				log.debug("Kmd file " + kmdFile + " found");
				kmdFiles.add(kmdFile);
			}
		}
	}

	private void loadManifest() throws IOException {

		if (jarFile != null) {
			JarEntry manifestJE = jarFile.getJarEntry(META_INF_KURENTO_FOLDER
					+ MANIFEST_FILE);

			if (manifestJE != null) {
				manifest = (JsonObject) gson.fromJson(new InputStreamReader(
						jarFile.getInputStream(manifestJE)), JsonElement.class);
			}

		} else {

			File manifestFile = new File(new File(artifactFile,
					META_INF_KURENTO_FOLDER), MANIFEST_FILE);

			if (manifestFile.exists()) {
				manifest = (JsonObject) gson.fromJson(new InputStreamReader(
						new FileInputStream(manifestFile)), JsonElement.class);
			}
		}

		if (manifest == null) {
			manifest = new JsonObject();
		} else {
			log.debug("Found manifest.json: " + manifest);
		}

		if (manifest.has(GENERATED_SOURCES_LIST_PROPERTY)) {

			this.kmdGeneratedSourcesList = new ArrayList<>();

			JsonArray array = manifest
					.getAsJsonArray(GENERATED_SOURCES_LIST_PROPERTY);

			for (JsonElement elem : array) {
				kmdGeneratedSourcesList.add(gson.fromJson(elem,
						GeneratedSourcesInfo.class));
			}
		}
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public JsonObject getManifest() {
		return manifest;
	}

	public boolean isKurentoArtifact() {
		return kurentoArtifact;
	}

	public List<Path> getKmdFiles() {
		return kmdFiles;
	}

	public String getArtifactCoordinate() {
		return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
				+ artifact.getVersion();
	}

	public Collection<GeneratedSourcesInfo> getKmdGeneratedSourcesList() {
		return kmdGeneratedSourcesList;
	}

	public boolean hasCode() {
		return !kmdGeneratedSourcesList.isEmpty();
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
