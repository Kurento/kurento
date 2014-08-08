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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.kurento.repository.Repository;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.kurento.repository.test.util.HttpRepositoryTest;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class FilenameAsIdTest extends HttpRepositoryTest {

	@Test
	public void test() throws IOException {

		Repository repository = getRepository();

		if (repository instanceof MongoRepository) {

			MongoRepository mongoRepository = (MongoRepository) repository;

			GridFS gridFS = mongoRepository.getGridFS();

			GridFSInputFile file = gridFS.createFile(new File(
					"test-files/sample.txt"));

			file.setId("sample.txt");

			file.save();

			List<GridFSDBFile> files = gridFS.find((DBObject) JSON
					.parse("{ _id : 'sample.txt' }"));

			assertNotNull(files);
			assertEquals(1, files.size());
		}

	}

}
