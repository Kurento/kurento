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

package org.kurento.jsonrpc.internal.client;

import static org.kurento.jsonrpc.JsonUtils.INJECT_SESSION_ID;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.client.RequestAlreadyRespondedException;
import org.kurento.jsonrpc.message.Message;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

public class TransactionImpl extends AbstractTransaction {

  public interface ResponseSender {
    void sendResponse(Message message) throws IOException;

    void sendPingResponse(Message message) throws IOException;
  }

  private final AtomicBoolean responded = new AtomicBoolean(false);
  private final ResponseSender responseSender;

  public TransactionImpl(Session session, Request<?> request, ResponseSender responseSender) {
    super(session, request);
    this.responseSender = responseSender;
  }

  public boolean setRespondedIfNot() {
    return responded.compareAndSet(false, true);
  }

  protected void internalSendResponse(Response<? extends Object> response) throws IOException {

    boolean notResponded = setRespondedIfNot();

    if (notResponded) {

      if (response.getSessionId() == null && INJECT_SESSION_ID) {
        response.setSessionId(session.getSessionId());
      }

      if (response.getId() == null) {
        response.setId(request.getId());
      }

      responseSender.sendResponse(response);

    } else {
      throw new RequestAlreadyRespondedException("This request has already been responded");
    }
  }

}
