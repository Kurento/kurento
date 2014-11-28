package org.kurento.test.latency;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class LatencyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private long latencyMilis;
	private TimeUnit latencyTimeUnit;
	private String lastLocalColor;
	private String lastRemoteColor;
	private long lastLocalColorChangeTime;
	private long lastRemoteColorChangeTime;

	public LatencyException(long latencyMilis, TimeUnit latencyTimeUnit,
			String lastLocalColor, String lastRemoteColor,
			long lastLocalColorChangeTime, long lastRemoteColorChangeTime) {
		this.latencyMilis = latencyMilis;
		this.latencyTimeUnit = latencyTimeUnit;
		this.lastLocalColor = lastLocalColor;
		this.lastRemoteColor = lastRemoteColor;
		this.lastLocalColorChangeTime = lastLocalColorChangeTime;
		this.lastRemoteColorChangeTime = lastRemoteColorChangeTime;
	}

	@Override
	public String getMessage() {
		String parsedLocaltime = new SimpleDateFormat("mm:ss.SSS")
				.format(lastLocalColorChangeTime);
		String parsedRemotetime = new SimpleDateFormat("mm:ss.SSS")
				.format(lastRemoteColorChangeTime);

		return "Latency error detected: " + latencyMilis + " "
				+ latencyTimeUnit
				+ " between last color change in remote tag (color="
				+ lastRemoteColor + " at minute " + parsedRemotetime
				+ ") and last color change in local tag (color="
				+ lastLocalColor + " at minute " + parsedLocaltime + ")";
	}

}
