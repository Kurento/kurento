/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.kmf.test.simple;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.kurento.kmf.common.PropertiesManager;

/**
 * Simple test for CI server.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.3.1
 */

public class SimpleTest {

	@Test
	public void testWorkspace() throws IOException {
		String workspace = PropertiesManager.getProperty("kurento.workspace",
				"/tmp");
		if (!workspace.endsWith("/")) {
			workspace += "/";
		}
		writeFile("Hello world (testWorkspace)", workspace
				+ "testWorkspace.txt");
	}

	@Test
	public void testCurrentDir() throws IOException {
		writeFile("Hello world (testCurrentDir)", "./testCurrentDir.txt");
	}

	@Test
	public void testAbsoluteDir() throws IOException {
		final String absoluteDir = "/tmp/testAbsoluteDir.txt";
		System.out.println("[[ATTACHMENT|" + absoluteDir + "]]");
		writeFile("Hello world (testAbsoluteDir)", absoluteDir);
	}

	private void writeFile(String content, String target) throws IOException {
		Files.write(content, new File(target), Charsets.UTF_8);
	}

}
