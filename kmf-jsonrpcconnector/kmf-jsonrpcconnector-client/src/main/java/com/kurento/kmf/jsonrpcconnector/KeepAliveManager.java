package com.kurento.kmf.jsonrpcconnector;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.kurento.kmf.common.PropertiesManager;
import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class KeepAliveManager {

	private static final String KEEP_ALIVE_INTERVAL_TIME_PROPERTY = "keepAliveIntervalTime";

	private static final int KEEP_ALIVE_TIME_DEFAULT_VALUE = 120000;

	private static final Object DUMMY_OBJECT_FOR_MAP = new Object();
	private static Logger log = LoggerFactory.getLogger(KeepAliveManager.class);

	public enum Mode {
		PER_CLIENT, PER_ID_AS_SESSION, PER_ID_AS_MEDIAPIPELINE
	};

	private JsonRpcClient client;
	private long keepAliveIntervalTime;
	private Mode mode;

	private ConcurrentHashMap<String, Object> ids = new ConcurrentHashMap<String, Object>();

	// TODO Is single thread executor the best choice?
	private ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();

	public KeepAliveManager(JsonRpcClient client, long keepAliveIntervalTime) {
		this(client, keepAliveIntervalTime, Mode.PER_CLIENT);
	}

	public KeepAliveManager(JsonRpcClient client, Mode mode) {
		this(client, -1, mode);
	}

	public KeepAliveManager(JsonRpcClient client, long keepAliveIntervalTime,
			Mode mode) {

		this.client = client;
		this.mode = mode;

		if (keepAliveIntervalTime != -1) {
			this.keepAliveIntervalTime = keepAliveIntervalTime;
		} else {
			this.keepAliveIntervalTime = PropertiesManager.getProperty(
					KEEP_ALIVE_INTERVAL_TIME_PROPERTY,
					KEEP_ALIVE_TIME_DEFAULT_VALUE);
		}
	}

	public void start() {
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				sendKeepAlives();
			}
		}, keepAliveIntervalTime, keepAliveIntervalTime, TimeUnit.MILLISECONDS);
	}

	protected void sendKeepAlives() {

		if (mode == Mode.PER_ID_AS_SESSION) {

			for (String id : ids.keySet()) {

				JsonObject params = new JsonObject();
				params.addProperty("object", id);
				try {
					client.sendRequest("keepAlive", params);
				} catch (IOException e) {
					log.error(
							"Exception while sending keepAlive from mediaPipeline "
									+ id, e);
				}
			}

		} else if (mode == Mode.PER_ID_AS_MEDIAPIPELINE) {

			for (String id : ids.keySet()) {

				JsonObject params = new JsonObject();
				params.addProperty("object", id);
				try {
					client.sendRequest(new Request<JsonObject>(id, null,
							"keepAlive", params));
				} catch (JsonRpcErrorException e) {
					log.warn(
							"Error while sending keepAlive for MediaPipeline '{}':"
									+ " {}. Removing this MediaPipeline from keepAlive list.",
							id, e.getMessage());

				} catch (IOException e) {
					log.error(
							"Exception while sending keepAlive from mediaPipeline "
									+ id, e);
				}
			}

		} else if (mode == Mode.PER_CLIENT) {

			try {
				client.sendRequest("keepAlive", null);
			} catch (IOException e) {
				throw new KurentoException(
						"Exception while sending keepAlive from session "
								+ client.getSession().getSessionId());
			}

		} else {
			throw new KurentoException("Unrecognized keepAlive mode = " + mode);
		}
	}

	public void addId(String id) {
		ids.put(id, DUMMY_OBJECT_FOR_MAP);
	}

	public void removeId(String id) {
		ids.remove(id);
	}

	public void stop() {
		this.executor.shutdown();
	}
}
