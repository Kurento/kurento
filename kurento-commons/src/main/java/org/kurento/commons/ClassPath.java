package org.kurento.commons;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class ClassPath {

  public static Path get(String resource) throws IOException {

    URL url = ClassPath.class.getResource(resource);

    if (url == null) {
      return null;
    }

    URI uri;
    try {
      uri = url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Exception converting classpath URL to URI", e);
    }

    String scheme = uri.getScheme();
    if (scheme.equals("file")) {
      return Paths.get(uri);
    }

    String s = uri.toString();
    int separator = s.indexOf("!/");
    String entryName = s.substring(separator + 2);
    URI fileURI = URI.create(s.substring(0, separator));

    FileSystem fs;
    try {

      fs = FileSystems.newFileSystem(fileURI, Collections.<String, Object> emptyMap());

    } catch (java.nio.file.FileSystemAlreadyExistsException e) {
      fs = FileSystems.getFileSystem(fileURI);
    }
    return fs.getPath(entryName);
  }

}
