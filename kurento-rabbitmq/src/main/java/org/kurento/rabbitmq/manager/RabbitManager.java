package org.kurento.rabbitmq.manager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class RabbitManager {

	private static Logger log = LoggerFactory.getLogger(RabbitManager.class);

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private String vhost;
	private String host = "localhost";
	private int port = 15672;

	private String user = "guest";
	private String password = "guest";

	private CloseableHttpClient httpclient;

	public RabbitManager(String vhost) {
		try {
			this.vhost = URLEncoder
					.encode(vhost, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(host, port),
				new UsernamePasswordCredentials(user, password));

		httpclient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();
	}

	public RabbitManager() {
		this("/");
	}

	public JsonObject getQueueInfo(String queue) throws IOException {
		return getRequest("queues/" + vhost + "/" + queue);
	}

	public void deleteQueue(String queue) throws IOException {
		deleteRequest("queues/" + vhost + "/" + queue);
	}

	private void deleteRequest(String command) throws IOException {
		// This should trigger MediaSessionStartedEvent
		String url = getUrl(command);

		log.debug("Rabbit management: DELETE " + url);

		CloseableHttpResponse response = httpclient
				.execute(new HttpDelete(url));

		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode < 200 || statusCode > 299) {
			throw new No2xxOKStatusResponseException(response);
		}
	}

	private JsonObject getRequest(String command) throws IOException {

		// This should trigger MediaSessionStartedEvent
		String url = getUrl(command);

		log.debug("Rabbit management: GET " + url);

		CloseableHttpResponse response = httpclient.execute(new HttpGet(url));

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new No2xxOKStatusResponseException(response);
		}

		String responseStr = EntityUtils.toString(response.getEntity());
		log.debug("Rabbit management response:" + responseStr);
		return gson.fromJson(responseStr, JsonObject.class);
	}

	private String getUrl(String command) {
		return "http://" + host + ":" + port + "/api/" + command;
	}
}
