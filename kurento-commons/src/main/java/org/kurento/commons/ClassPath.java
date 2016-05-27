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

    String str = uri.toString();
    int separator = str.indexOf("!/");
    String entryName = str.substring(separator + 2);
    URI fileUri = URI.create(str.substring(0, separator));

    FileSystem fs;
    try {

      fs = FileSystems.newFileSystem(fileUri, Collections.<String, Object>emptyMap());

    } catch (java.nio.file.FileSystemAlreadyExistsException e) {
      fs = FileSystems.getFileSystem(fileUri);
    }
    return fs.getPath(entryName);
  }

}
