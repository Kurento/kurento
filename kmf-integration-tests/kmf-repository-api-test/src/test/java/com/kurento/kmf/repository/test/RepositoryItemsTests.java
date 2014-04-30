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

package com.kurento.kmf.repository.test;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.test.util.HttpRepositoryTest;
import com.kurento.kmf.repository.test.util.TestUtils;

public class RepositoryItemsTests extends HttpRepositoryTest {

	@Test
	public void testFileUpload() throws Exception {
		uploadFile(new File("test-files/sample.txt"));
	}

	@Test
	public void testFileUploadWithPOSTAndDownload() throws Exception {

		RepositoryItem repositoryItem = getRepository().createRepositoryItem();

		String id = repositoryItem.getId();

		File fileToUpload = new File("test-files/sample.txt");
		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder();

		uploadFileWithPOST(recorder.getURL(), fileToUpload);

		recorder.stop();

		RepositoryItem newRepositoryItem = getRepository()
				.findRepositoryItemById(id);

		File downloadedFile = new File("test-files/tmp/" + id);
		downloadFromURL(
				newRepositoryItem.createRepositoryHttpPlayer().getURL(),
				downloadedFile);

		assertTrue(TestUtils.equalFiles(fileToUpload, downloadedFile));
	}

}