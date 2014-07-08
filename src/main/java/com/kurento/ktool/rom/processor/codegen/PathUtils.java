package com.kurento.ktool.rom.processor.codegen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PathUtils {

	public static Path getPathInClasspath(URL resource) throws IOException,
			URISyntaxException {

		Objects.requireNonNull(resource, "Resource URL cannot be null");
		URI uri = resource.toURI();

		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			return Paths.get(uri);
		}

		if (!scheme.equals("jar")) {
			throw new IllegalArgumentException("Cannot convert to Path: " + uri);
		}

		String s = uri.toString();
		int separator = s.indexOf("!/");
		String entryName = s.substring(separator + 2);
		URI fileURI = URI.create(s.substring(0, separator));

		FileSystem fs = FileSystems.newFileSystem(fileURI,
				Collections.<String, Object> emptyMap());
		return fs.getPath(entryName);
	}

	public static void delete(Path folder, List<String> noDeleteFiles)
			throws IOException {
		delete(folder, folder, noDeleteFiles);
	}

	public static void delete(Path basePath, Path path, List<String> noDeleteFiles)
			throws IOException {

		Path relativePath = basePath.relativize(path);

		if (noDeleteFiles.contains(relativePath.toString())) {
			return;
		}

		if (Files.isDirectory(path)) {

			try (DirectoryStream<Path> directoryStream = Files
					.newDirectoryStream(path)) {
				for (Path c : directoryStream) {
					delete(basePath, c, noDeleteFiles);
				}
			}

			if (isEmptyDir(path)) {
				Files.delete(path);
			}
		} else {
			Files.delete(path);
		}
	}

	public static boolean isEmptyDir(Path path) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
			Iterator<Path> files = ds.iterator();
			return !files.hasNext();
		}
	}
}
