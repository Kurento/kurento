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

package com.kurento.kmf.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.test.util.HttpRepositoryTest;

public class RangeGetTests extends HttpRepositoryTest {

	@Test
	public void test() throws Exception {

		String id = "logo.png";

		RepositoryItem item;
		try {
			item = getRepository().findRepositoryItemById(id);
		} catch (NoSuchElementException e) {
			item = getRepository().createRepositoryItem(id);
			uploadFile(new File("test-files/" + id), item);
		}

		RepositoryHttpPlayer player = item.createRepositoryHttpPlayer();

		String url = player.getURL();

		player.setAutoTerminationTimeout(100000);

		// Following sample
		// http://stackoverflow.com/questions/8293687/sample-http-range-request-session

		RestTemplate httpClient = getRestTemplate();

		{
			HttpHeaders requestHeaders = new HttpHeaders();

			MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

			HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(
					postParameters, requestHeaders);

			ResponseEntity<byte[]> response = httpClient.exchange(url,
					HttpMethod.GET, requestEntity, byte[].class);

			System.out.println(response);

			assertTrue("The server doesn't accept ranges", response
					.getHeaders().containsKey("Accept-ranges"));
			assertTrue("The server doesn't accept ranges with bytes", response
					.getHeaders().get("Accept-ranges").contains("bytes"));
		}

		long fileLength = 0;

		{
			// Range: bytes=0-

			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.set("Range", "bytes=0-");

			MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

			HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(
					postParameters, requestHeaders);

			ResponseEntity<byte[]> response = httpClient.exchange(url,
					HttpMethod.GET, requestEntity, byte[].class);

			System.out.println(response);

			assertEquals(
					"The server doesn't respond with http status code 206 to a request with ranges",
					HttpStatus.PARTIAL_CONTENT, response.getStatusCode());

			fileLength = Long.parseLong(response.getHeaders()
					.get("Content-Length").get(0));

		}

		{
			HttpHeaders requestHeaders = new HttpHeaders();

			long firstByte = fileLength - 3000;
			long lastByte = fileLength - 1;
			long numBytes = lastByte - firstByte + 1;

			requestHeaders.set("Range", "bytes=" + firstByte + "-" + lastByte);

			MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

			HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(
					postParameters, requestHeaders);

			ResponseEntity<byte[]> response = httpClient.exchange(url,
					HttpMethod.GET, requestEntity, byte[].class);

			System.out.println(response);

			assertEquals(
					"The server doesn't respond with http status code 206 to a request with ranges",
					response.getStatusCode(), HttpStatus.PARTIAL_CONTENT);

			long responseContentLength = Long.parseLong(response.getHeaders()
					.get("Content-Length").get(0));
			assertEquals("The server doesn't send the requested bytes",
					numBytes, responseContentLength);

			assertEquals("The server doesn't send the requested bytes",
					responseContentLength, response.getBody().length);

		}
	}
}