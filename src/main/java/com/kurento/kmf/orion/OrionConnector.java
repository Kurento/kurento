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
package com.kurento.kmf.orion;

import static com.kurento.kmf.orion.entities.ContextUpdate.ContextUpdateAction.APPEND;
import static com.kurento.kmf.orion.entities.ContextUpdate.ContextUpdateAction.DELETE;
import static com.kurento.kmf.orion.entities.ContextUpdate.ContextUpdateAction.UPDATE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.kurento.kmf.orion.entities.ContextUpdate;
import com.kurento.kmf.orion.entities.ContextUpdateResponse;
import com.kurento.kmf.orion.entities.OrionAttribute;
import com.kurento.kmf.orion.entities.OrionContextElement;
import com.kurento.kmf.orion.entities.QueryContext;
import com.kurento.kmf.orion.entities.QueryContextResponse;

/**
 * Connector to the ORion context broker. This connector uses only the NGSI10
 * service from Orion, and none of it's convenience methods.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
public class OrionConnector {

	private static final String QUERY_PATH = "/ngsi10/queryContext";
	private static final String UPDATE_PATH = "/ngsi10/updateContext";
	private static final String SUBSCRIBE_PATH = "/ngsi10/subscribeContext";
	private static final String ENTITIES_PATH = "/ngsi10/contextEntities";
	private static final String UNSUBSCRIBE_PATH = "/ngsi10/unsubscribeContext";
	private static final String UPDATE_SUBSCRIBE_PATH = "/ngsi10/updateContextSubscription";

	private static final Gson gson = new Gson();
	private static final Logger log = LoggerFactory
			.getLogger(OrionConnector.class);

	@Autowired
	private OrionConnectorConfiguration config;

	private URI orionAddr;

	/**
	 * Default constructor to be used when the orion connector is created from a
	 * spring context.
	 */
	public OrionConnector() {

	}

	/**
	 * Orion connector constructor. This constructor is to be used when the
	 * connector is used outside from a spring context.
	 * 
	 * @param config
	 *            Configuration object
	 */
	public OrionConnector(OrionConnectorConfiguration config) {
		this.config = config;
		this.init();
	}

	/**
	 * Initiates the {@link #orionAddr}. This step is performed to validate the
	 * fields from the configuration object.
	 */
	@PostConstruct
	private void init() {
		try {
			orionAddr = new URIBuilder().setScheme(config.getOrionScheme())
					.setHost(config.getOrionHost())
					.setPort(config.getOrionPort()).build();
		} catch (URISyntaxException e) {
			throw new OrionConnectorException(
					"Could not build URI to make a request to Orion", e);
		}
	}

	/**
	 * Register context elements in the Orion context broker.
	 * 
	 * @param events
	 *            List of events
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 * 
	 */
	public ContextUpdateResponse registerContextElements(
			OrionContextElement... events) {
		ContextUpdate ctxUpdate = new ContextUpdate(APPEND, events);
		return sendRequestToOrion(ctxUpdate, UPDATE_PATH,
				ContextUpdateResponse.class);
	}

	/**
	 * Updates context elements that exist in Orion.
	 * 
	 * @param events
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	public ContextUpdateResponse updateContextElements(
			OrionContextElement... events) {
		ContextUpdate ctxUpdate = new ContextUpdate(UPDATE, events);
		return sendRequestToOrion(ctxUpdate, UPDATE_PATH,
				ContextUpdateResponse.class);
	}

	/**
	 * Deletes one or more context elements from Orion
	 * 
	 * @param events
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	public ContextUpdateResponse deleteContextElements(
			OrionContextElement... events) {
		ContextUpdate ctxUpdate = new ContextUpdate(DELETE, events);
		return sendRequestToOrion(ctxUpdate, UPDATE_PATH,
				ContextUpdateResponse.class);
	}

	/**
	 * Deletes an attribute from a registered Orion context element.
	 * 
	 * @param element
	 * @param attribute
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	public String deleteContextElementAttribute(OrionContextElement element,
			OrionAttribute<?> attribute) {
		// curl localhost:1026/NGSI10/contextEntities/E1/attribute/B -s -S
		// --header 'Content-Type: application/json' -X DELETE --header 'Accept:
		// application/json' | python -mjson.tool
		return null;
		// sendRemoveRequestToOrion(element.getId(), attribute.getName());
	}

	/**
	 * Queries the context broker for a certain element.
	 * 
	 * @param type
	 *            The type of context element
	 * @param id
	 *            the id of the context element
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	public QueryContextResponse queryContext(String type, String id) {
		OrionContextElement element = new OrionContextElement();
		element.setId(id);
		element.setType(type);
		QueryContext query = new QueryContext(element);
		return sendRequestToOrion(query, QUERY_PATH, QueryContextResponse.class);
	}

	/**
	 * Queries the context broker for a pattern-based group of context elements
	 * 
	 * @param type
	 *            the type of the context element.
	 * @param pattern
	 *            the pattern to search IDs that fulfil this pattern.
	 * @return The response from the context broker.
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	public QueryContextResponse queryContextWithPattern(String type,
			String pattern) {
		OrionContextElement element = new OrionContextElement();
		element.setId(pattern);
		element.setPattern(true);
		element.setType(type);
		QueryContext query = new QueryContext(element);
		return sendRequestToOrion(query, QUERY_PATH, QueryContextResponse.class);
	}

	/**
	 * Sends a request to Orion
	 * 
	 * @param ctxElement
	 *            The context element
	 * @param path
	 *            the path from the context broker that determines which
	 *            "operation"will be executed
	 * @param responseClazz
	 *            The class expected for the response
	 * @return The object representing the JSON answer from Orion
	 * @throws OrionConnectorException
	 *             if a communication exception happens, either when contacting
	 *             the context broker at the given address, or obtaining the
	 *             answer from it.
	 */
	private <E, T> T sendRequestToOrion(E ctxElement, String path,
			Class<T> responseClazz) {
		String jsonEntity = gson.toJson(ctxElement);
		log.debug("Send request to Orion: {}", jsonEntity);

		Request req = Request.Post(orionAddr.toString() + path)
				.addHeader("Accept", APPLICATION_JSON.getMimeType())
				.bodyString(jsonEntity, APPLICATION_JSON).connectTimeout(5000)
				.socketTimeout(5000);
		Response response;
		try {
			response = req.execute();
		} catch (IOException e) {
			throw new OrionConnectorException("Could not execute HTTP request",
					e);
		}

		HttpResponse httpResponse = checkResponse(response);

		T ctxResp = getOrionObjFromResponse(httpResponse, responseClazz);

		log.debug("Send to Orion response: {}", httpResponse);

		return ctxResp;
	}

	private <T> HttpResponse checkResponse(Response response) {
		HttpResponse httpResponse;
		try {
			httpResponse = response.returnResponse();
		} catch (IOException e) {
			throw new OrionConnectorException("Could not obtain HTTP response",
					e);
		}

		if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new OrionConnectorException("Failed with HTTP error code : "
					+ httpResponse.getStatusLine().getStatusCode());
		}

		return httpResponse;
	}

	private <T> T getOrionObjFromResponse(HttpResponse httpResponse,
			Class<T> responseClazz) {
		InputStream source;
		try {
			source = httpResponse.getEntity().getContent();
		} catch (IllegalStateException | IOException e) {
			throw new OrionConnectorException(
					"Could not obtain entity content from HTTP response", e);
		}

		T ctxResp = null;
		try (Reader reader = new InputStreamReader(source)) {
			ctxResp = gson.fromJson(reader, responseClazz);
		} catch (IOException e) {
			log.warn("Could not close input stream from HttpResponse.", e);
		}

		return ctxResp;
	}

	// contextEntities consultas a entidades, borrados...
	// queryContext
	// notifyContext notifyContextRequest notifyContextResponse
	// updateContext

	// queryContext access the context information stored. con lista de
	// atributos
	// devuelve sólo los atributos deseados
	// contextEntities

	/*-
	 *	subscribeContext
	 *	updateContextSubscription
	 *	unsubscribeContext
	 * 
	 * (curl localhost:1026/NGSI10/subscribeContext -s -S 
	 *  --header 'Content-Type: application/json' 
	 *  --header 'Accept: application/json' -d @- | python -mjson.tool) <<EOF
	 *	{
	 *		"entities": [
	 *  		{
	 *				"type": "Room",
	 *				"isPattern": "false",
	 *				"id": "Room1"
	 *			}
	 *		],
	 *		"attributes": [
	 *			"temperature"
	 *		],
	 *		"reference": "http://localhost:1028/accumulate",
	 *		"duration": "P1M",
	 *		"notifyConditions": [
	 *			{
	 *				"type": "ONTIMEINTERVAL",
	 *				"condValues": [
	 *				"PT10S"
	 *				]
	 *			}
	 *		]
	 *	}
	 *	EOF
	 */
}
