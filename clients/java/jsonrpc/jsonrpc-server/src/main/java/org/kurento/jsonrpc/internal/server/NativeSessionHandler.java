/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.internal.server;

import org.kurento.jsonrpc.Session;

/**
 * This interface will be implemented by JsonRpcHandlers that want a low level handling of requests
 * with unknown sessionId. It is specially useful in clustered environments when session can be
 * stored in other data structures
 *
 * @author micael.gallego@gmail.com
 */
public interface NativeSessionHandler {

  public boolean isSessionKnown(String sessionId);

  public void processNewCreatedKnownSession(Session session);

}
