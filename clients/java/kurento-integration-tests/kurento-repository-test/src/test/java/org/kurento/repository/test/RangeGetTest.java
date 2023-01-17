/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class RangeGetTest extends BaseRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(RangeGetTest.class);

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

    player.setAutoTerminationTimeout(10000);

    // Following sample
    // http://stackoverflow.com/questions/8293687/sample-http-range-request-session

    RestTemplate httpClient = getRestTemplate();

    acceptRanges(url, httpClient);
    log.debug("Accept ranges test passed");

    long fileLength = rangeFrom0(url, httpClient);
    log.debug("Range from 0 test passed");

    randomRange(url, httpClient, fileLength);
    log.debug("Random range test passed");
  }

  private void randomRange(String url, RestTemplate httpClient, long fileLength) {

    HttpHeaders requestHeaders = new HttpHeaders();

    long firstByte = fileLength - 3000;
    long lastByte = fileLength - 1;
    long numBytes = lastByte - firstByte + 1;

    requestHeaders.set("Range", "bytes=" + firstByte + "-" + lastByte);

    MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

    HttpEntity<MultiValueMap<String, String>> requestEntity =
        new HttpEntity<MultiValueMap<String, String>>(postParameters, requestHeaders);

    ResponseEntity<byte[]> response =
        httpClient.exchange(url, HttpMethod.GET, requestEntity, byte[].class);

    log.debug("Response: " + response);

    assertEquals("The server doesn't respond with http status code 206 to a request with ranges",
        response.getStatusCode(), HttpStatus.PARTIAL_CONTENT);

    long responseContentLength = Long.parseLong(response.getHeaders().get("Content-Length").get(0));
    assertEquals("The server doesn't send the requested bytes", numBytes, responseContentLength);

    assertEquals("The server doesn't send the requested bytes", responseContentLength,
        response.getBody().length);
  }

  private long rangeFrom0(String url, RestTemplate httpClient) {

    // Range: bytes=0-

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("Range", "bytes=0-");

    MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

    HttpEntity<MultiValueMap<String, String>> requestEntity =
        new HttpEntity<MultiValueMap<String, String>>(postParameters, requestHeaders);

    ResponseEntity<byte[]> response =
        httpClient.exchange(url, HttpMethod.GET, requestEntity, byte[].class);

    log.debug("Response: " + response);

    assertEquals("The server doesn't respond with http status code 206 to a request with ranges",
        HttpStatus.PARTIAL_CONTENT, response.getStatusCode());

    return Long.parseLong(response.getHeaders().get("Content-Length").get(0));
  }

  private void acceptRanges(String url, RestTemplate httpClient) {

    HttpHeaders requestHeaders = new HttpHeaders();

    MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();

    HttpEntity<MultiValueMap<String, String>> requestEntity =
        new HttpEntity<MultiValueMap<String, String>>(postParameters, requestHeaders);

    ResponseEntity<byte[]> response =
        httpClient.exchange(url, HttpMethod.GET, requestEntity, byte[].class);

    log.debug("Response: " + response);

    assertTrue("The server doesn't accept ranges",
        response.getHeaders().containsKey("Accept-ranges"));
    assertTrue("The server doesn't accept ranges with bytes",
        response.getHeaders().get("Accept-ranges").contains("bytes"));
  }
}