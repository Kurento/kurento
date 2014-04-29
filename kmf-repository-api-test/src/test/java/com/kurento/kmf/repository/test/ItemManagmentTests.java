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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.kurento.kmf.repository.DuplicateItemException;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.test.util.HttpRepositoryTest;

public class ItemManagmentTests extends HttpRepositoryTest {

	@Test(expected = DuplicateItemException.class)
	public void duplicateTest() throws IOException {

		Repository repository = getRepository();

		RepositoryItem item = repository.createRepositoryItem("file1");
		item.createOutputStreamToWrite().close();

		RepositoryItem item2 = repository.createRepositoryItem("file1");
		item2.createOutputStreamToWrite().close();

	}

	@Test
	public void metadataTest() throws IOException {

		Repository repository = getRepository();

		for (int i = 0; i < 10; i++) {
			try {
				RepositoryItem item = repository.findRepositoryItemById("File"
						+ i + ".txt");
				repository.remove(item);
			} catch (NoSuchElementException e) {
				// Do nothing if repository item doesn't exist
			}
		}

		for (int i = 0; i < 10; i++) {

			RepositoryItem item = repository.createRepositoryItem("File" + i
					+ ".txt");
			item.putMetadataEntry("numFile", Integer.toString(i));
			item.putMetadataEntry("att", "value");
			item.putMetadataEntry("regexAtt", "token" + Integer.toString(i));
			OutputStream os = item.createOutputStreamToWrite();
			os.write(0);
			os.close();

		}

		for (int i = 0; i < 10; i++) {

			try {
				RepositoryItem item = repository.findRepositoryItemById("File"
						+ i + ".txt");

				String numString = item.getMetadata().get("numFile");
				assertEquals(numString, Integer.toString(i));

				assertEquals(item.getMetadata().get("att"), "value");

			} catch (NoSuchElementException e) {
				fail("Element 'File" + i + ".txt' doesn't exist");
			}

		}

		List<RepositoryItem> items = repository.findRepositoryItemsByAttValue(
				"att", "value");

		assertEquals("Found different items than expected", 10, items.size());

		items = repository.findRepositoryItemsByAttRegex("regexAtt", "token.*");

		assertEquals("Found different items than expected", 10, items.size());

	}

}
