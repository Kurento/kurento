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
package org.kurento.test.monitor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Statistic enumeration.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.6
 */
public enum StatsOperation {

	AVG, SUM, DEL;

	private static Map<String, StatsOperation> statsOperationMap;
	private static final String DEFAULT_CSV_FILE = "stats.csv";
	private static final String CSV_FILE_SEPARATOR = ",";

	public static StatsOperation getType(String opStr) {
		StatsOperation op = null;
		if (opStr.equalsIgnoreCase("avg")) {
			op = AVG;
		} else if (opStr.equalsIgnoreCase("sum")) {
			op = SUM;
		} else if (opStr.equalsIgnoreCase("del")) {
			op = DEL;
		}
		return op;
	}

	public static Map<String, StatsOperation> map() {
		return map(DEFAULT_CSV_FILE);
	}

	@Override
	public String toString() {
		return "_" + super.toString().toLowerCase();
	}

	public static Map<String, StatsOperation> map(String csvFile) {
		if (statsOperationMap == null) {
			statsOperationMap = new HashMap<>();

			InputStream inputStream = StatsOperation.class.getClassLoader()
					.getResourceAsStream(csvFile);
			try {
				List<String> csv = CharStreams.readLines(new InputStreamReader(
						inputStream, Charsets.UTF_8));
				for (String line : csv) {
					String[] values = line.split(CSV_FILE_SEPARATOR);
					statsOperationMap.put(values[1] + "_" + values[0],
							StatsOperation.getType(values[2]));
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return statsOperationMap;
	}

}