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
package com.kurento.kmf.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.kurento.kmf.common.exception.KurentoException;

/**
 * Local shell.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class Shell {

	public static Logger log = LoggerFactory.getLogger(Shell.class);

	public static void run(final String... command) {
		log.debug("Running command on the shell: {}", Arrays.toString(command));

		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			log.error(
					"Exception while executing command '"
							+ Arrays.toString(command) + "'", e);
		}
	}

	public static String runAndWait(final String... command) {
		log.debug("Running command on the shell: {}", Arrays.toString(command));

		Process p;
		try {
			p = new ProcessBuilder(command).redirectErrorStream(true).start();

			String output = CharStreams.toString(new InputStreamReader(p
					.getInputStream(), "UTF-8"));

			return output;

		} catch (IOException e) {
			throw new KurentoException(
					"Exception executing command on the shell: "
							+ Arrays.toString(command), e);
		}
	}

}
