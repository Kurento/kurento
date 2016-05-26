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
package org.kurento.modulecreator;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PathUtils {

  public static class Finder extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private final List<Path> paths = new ArrayList<>();

    Finder(String pattern) {
      matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    // Compares the glob pattern against
    // the file or directory name.
    void find(Path file) {
      Path name = file.getFileName();
      if (name != null && matcher.matches(name)) {
        paths.add(file);
      }
    }

    // Invoke the pattern matching
    // method on each file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      find(file);
      return CONTINUE;
    }

    // Invoke the pattern matching
    // method on each directory.
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      find(dir);
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      System.err.println(exc);
      return CONTINUE;
    }

    public List<Path> getPaths() {
      return paths;
    }
  }

  public static Path getPathInClasspath(String resourceName)
      throws IOException, URISyntaxException {
    return getPathInClasspath(PathUtils.class.getResource(resourceName));
  }

  public static Path getPathInClasspath(URL resource) throws IOException, URISyntaxException {

    Objects.requireNonNull(resource, "Resource URL cannot be null");
    URI uri = resource.toURI();

    String scheme = uri.getScheme();
    if (scheme.equals("file")) {
      return Paths.get(uri);
    }

    if (!scheme.equals("jar")) {
      throw new IllegalArgumentException("Cannot convert to Path: " + uri);
    }

    String uriStr = uri.toString();
    int separator = uriStr.indexOf("!/");
    String entryName = uriStr.substring(separator + 2);
    URI fileUri = URI.create(uriStr.substring(0, separator));

    FileSystem fs = null;

    try {

      fs = FileSystems.newFileSystem(fileUri, Collections.<String, Object>emptyMap());

    } catch (FileSystemAlreadyExistsException e) {
      fs = FileSystems.getFileSystem(fileUri);
    }

    return fs.getPath(entryName);
  }

  public static void delete(Path folder, List<String> noDeleteFiles) throws IOException {
    delete(folder, folder, noDeleteFiles);
  }

  public static void delete(Path basePath, Path path, List<String> noDeleteFiles)
      throws IOException {

    Path relativePath = basePath.relativize(path);

    if (noDeleteFiles.contains(relativePath.toString())) {
      return;
    }

    if (Files.isDirectory(path)) {

      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
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

  public static List<Path> getPaths(String[] pathNames, String globPattern) throws IOException {

    List<Path> paths = new ArrayList<Path>();
    for (String pathName : pathNames) {
      Path path = Paths.get(pathName);
      if (Files.exists(path)) {
        paths.addAll(searchFiles(path, globPattern));
      }
    }
    return paths;
  }

  public static List<Path> searchFiles(Path path, String globPattern) throws IOException {

    if (Files.isDirectory(path)) {
      Finder finder = new Finder(globPattern);
      Files.walkFileTree(path, finder);
      return finder.getPaths();
    } else {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

      if (matcher.matches(path.getFileName())) {
        return Arrays.asList(path);
      } else {
        return Collections.emptyList();
      }
    }
  }

  public static void deleteRecursive(Path path) throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null) {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        } else {
          throw exc;
        }
      }
    });
  }

  public static boolean isEmptyDir(Path path) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
      Iterator<Path> files = ds.iterator();
      return !files.hasNext();
    }
  }
}
