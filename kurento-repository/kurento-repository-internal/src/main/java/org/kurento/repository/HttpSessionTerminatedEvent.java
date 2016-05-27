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

package org.kurento.repository;

/**
 * This class represents an event fired when an client is "considered" disconnected for the
 * {@link RepositoryHttpEndpoint} identified as source.
 * </p>
 * The {@link RepositoryHttpEndpoint} is based on http protocol. As this protocol is stateless,
 * there is no concept of "connection". For this reason, the way to consider that a client is
 * disconnected is when a time is elapsed without requests for the client. This concept is commonly
 * used to manage http sessions in web applications. The timeout can be configured in the endpoint
 * with {@link RepositoryHttpEndpoint#setAutoTerminationTimeout(long)}
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
public class HttpSessionTerminatedEvent extends RepositoryHttpSessionEvent {

  public HttpSessionTerminatedEvent(RepositoryHttpEndpoint source) {
    super(source);
  }

}
