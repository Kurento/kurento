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
package org.kurento.test.latency;

import java.awt.Color;

/**
 * Latency registry (for store and draw latency results compiled for
 * LatencyController).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class LatencyRegistry {

	private Color color;
	private long latency; // millisecons
	private boolean latencyError;
	private LatencyException latencyException;

	public LatencyRegistry(Color color, long latency) {
		this.color = color;
		this.latency = latency;

		// By default, we suppose everything went fine
		this.latencyError = false;
		this.latencyException = null;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency) {
		this.latency = latency;
	}

	public boolean isLatencyError() {
		return latencyError;
	}

	public void setLatencyError(boolean latencyError) {
		this.latencyError = latencyError;
	}

	public LatencyException getLatencyException() {
		return latencyException;
	}

	public void setLatencyException(LatencyException latencyException) {
		this.latencyError = true;
		this.latencyException = latencyException;
	}

}
