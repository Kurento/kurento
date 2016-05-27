/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository.internal.repoimpl.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Map;

import org.kurento.commons.exception.KurentoException;
import org.kurento.repository.RepositoryItemAttributes;
import org.kurento.repository.internal.repoimpl.AbstractRepositoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRepositoryItem extends AbstractRepositoryItem {

  private static final Logger log = LoggerFactory.getLogger(FileRepositoryItem.class);
  private final File file;
  private OutputStream storingOutputStream;

  public FileRepositoryItem(FileSystemRepository repository, File file, String id,
      Map<String, String> metadata) {

    super(id, calculateState(file), loadAttributes(file), repository);
    this.file = file;
    setMetadata(metadata);
  }

  private static State calculateState(File file) {
    return file.exists() && file.length() > 0 ? State.STORED : State.NEW;
  }

  private static RepositoryItemAttributes loadAttributes(File file) {

    RepositoryItemAttributes attributes = new RepositoryItemAttributes();

    if (file.exists()) {
      attributes.setContentLength(file.length());
      attributes.setLastModified(file.lastModified());

      String mimeType = null;
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        mimeType = URLConnection.guessContentTypeFromStream(is);
      } catch (Exception e) {
        log.warn("Exception produced during load of attributes", e);
      }

      attributes.setMimeType(mimeType);
    }

    return attributes;
  }

  @Override
  public InputStream createInputStreamToRead() {

    checkState(State.STORED);

    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new KurentoException(
          "The file storing this repositoty item was deleted before creation", e);
    }
  }

  @Override
  public OutputStream createOutputStreamToWrite() {

    checkState(State.NEW);

    try {

      this.state = State.STORING;

      storingOutputStream = new FilterOutputStream(new FileOutputStream(file)) {
        @Override
        public void close() throws java.io.IOException {
          refreshAttributesOnClose();
        }
      };

      return storingOutputStream;

    } catch (FileNotFoundException e) {
      throw new KurentoException("There is a problem opening the output stream to the file "
          + "that will store the contents of the repositoty item", e);
    }
  }

  private void refreshAttributesOnClose() {
    state = State.STORED;
    attributes.setContentLength(file.length());
  }

  public File getFile() {
    return file;
  }

  @Override
  public void setMetadata(Map<String, String> metadata) {
    super.setMetadata(metadata);
    ((FileSystemRepository) repository).setMetadataForItem(this, metadata);
  }
}
