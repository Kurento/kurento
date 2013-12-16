/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.kurento.kmf.repository.internal.repoimpl.filesystem;

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

import com.kurento.kmf.repository.RepositoryItemAttributes;
import com.kurento.kmf.repository.internal.repoimpl.AbstractRepositoryItem;

public class FileRepositoryItem extends AbstractRepositoryItem {

	private File file;
	private OutputStream storingOutputStream;

	public FileRepositoryItem(FileSystemRepository repository, File file,
			String id, Map<String, String> metadata) {

		super(id, calculateState(file), loadAttributes(file), repository);
		this.file = file;
		setMetadata(metadata);
	}

	private static State calculateState(File file) {
		return file.exists() && file.length() > 0 ? State.STORED : State.NEW;
	}

	private static RepositoryItemAttributes loadAttributes(File file) {

		RepositoryItemAttributes attributes = new RepositoryItemAttributes();
		attributes.setContentLength(file.length());
		attributes.setLastModified(file.lastModified());

		String mimeType = null;
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			mimeType = URLConnection.guessContentTypeFromStream(is);
			is.close();
		} catch (Exception e) {
		}

		attributes.setMimeType(mimeType);

		return attributes;
	}

	@Override
	public InputStream createInputStreamToRead() {

		checkState(State.STORED);

		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(
					"The file storing this repositoty item was deleted before creation",
					e);
		}
	}

	@Override
	public OutputStream createOutputStreamToWrite() {

		checkState(State.NEW);

		try {

			this.state = State.STORING;

			storingOutputStream = new FilterOutputStream(new FileOutputStream(
					file)) {
				public void close() throws java.io.IOException {
					refreshAttributesOnClose();
				}
			};

			return storingOutputStream;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(
					"There is a problem to open the output stream to the file "
							+ "that will store the contents of the repositoty item",
					e);
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
