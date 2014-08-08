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

package org.kurento.repository.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.RepositoryApiTests;
import org.kurento.repository.internal.repoimpl.filesystem.ItemsMetadata;

@Category(RepositoryApiTests.class)
public class ItemsMetadataTest {

	@Test
	public void test() throws IOException {

		File tempFile = File.createTempFile("metadata", "");

		ItemsMetadata itemsMetadata = new ItemsMetadata(tempFile);

		for (int i = 0; i < 10; i++) {
			Map<String, String> md1 = itemsMetadata.loadMetadata("o" + i);
			md1.put("differentAtt", "value" + i);
			md1.put("sameAtt", "value");
		}

		itemsMetadata.save();

		itemsMetadata = new ItemsMetadata(tempFile);

		assertEquals(10, itemsMetadata.findByAttValue("sameAtt", "value")
				.size());
		assertEquals(1, itemsMetadata.findByAttValue("differentAtt", "value1")
				.size());
		assertEquals(10, itemsMetadata
				.findByAttRegex("differentAtt", "value.*").size());

	}

}
