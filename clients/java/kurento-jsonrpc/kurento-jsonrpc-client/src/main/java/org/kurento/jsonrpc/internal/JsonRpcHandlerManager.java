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

package org.kurento.jsonrpc.internal;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.internal.client.TransactionImpl;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.MessageUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class JsonRpcHandlerManager {

  private static final Logger log = LoggerFactory.getLogger(JsonRpcHandlerManager.class);

  private JsonRpcHandler<?> handler;

  public JsonRpcHandlerManager(JsonRpcHandler<?> handler) {
    this.handler = handler;
  }

  public JsonRpcHandlerManager() {
  }

  /**
   * Sets the handler. This method will also set the handlerClass, based on the {@link #getClass()}
   * method from the handler passed as parameter
   *
   * @param handler
   */
  public void setJsonRpcHandler(JsonRpcHandler<?> handler) {
    this.handler = handler;
  }

  public void afterConnectionClosed(Session session, String reason) {
    if (handler != null) {
      try {
        handler.afterConnectionClosed(session, reason);
      } catch (Exception e) {
        try {
          handler.handleUncaughtException(session, e);
        } catch (Exception e2) {
          log.error("Exception while executing handleUncaughtException", e2);
        }
      }
    }
  }

  public void afterConnectionEstablished(Session session) {

    try {
      if (handler != null) {
        handler.afterConnectionEstablished(session);
      }
    } catch (Exception e) {
      try {
        handler.handleUncaughtException(session, e);
      } catch (Exception e2) {
        log.error("Exception while executing handleUncaughtException", e2);
      }
    }
  }

	public void afterReconnection(Session session) {
		if (handler != null) {
			try {
				handler.afterReconnection(session);
			} catch (Exception e) {
				try {
					handler.handleUncaughtException(session, e);
				} catch (Exception e2) {
					log.error("Exception while executing handleUncaughtException", e2);
				}
			}
		}
	}

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void handleRequest(Session session, Request<JsonElement> request, ResponseSender rs) {

    try {

      if (handler == null) {
        log.warn("JsonRpcClient has received a request from server but"
            + " there is no JsonRpcHandler configured to manage this" + " request");
        return;
      }

      Class<?> paramsType = getParamsType(handler.getHandlerType());
      Request<?> nonGenRequest;
      try {

        nonGenRequest = MessageUtils.convertRequest(request, paramsType);

      } catch (ClassCastException e) {

        String message = "The handler " + handler.getClass()
            + " is trying to process the request. But request params '" + request.getParams()
            + "' cannot be converted to " + paramsType.getCanonicalName()
            + ". The type to convert params is specified in the"
            + " handler as the supertype generic parameter";

        // TODO Maybe use the pattern handleUncaughtException
        log.error(message, e);

        if (request.getId() != null) {
          rs.sendResponse(new Response<>(null, new ResponseError(0, message)));
        }
        return;
      }

      JsonRpcHandler nonGenHandler = handler;

      TransactionImpl tx = new TransactionImpl(session, request, rs);
      nonGenHandler.handleRequest(tx, nonGenRequest);

      if (!tx.isAsync() && request.getId() != null) {

        log.debug("Request {} is processed asynchronously", request);

        boolean notResponded = tx.setRespondedIfNot();

        if (notResponded) {
          // Empty response
          rs.sendResponse(new Response<>(request.getId(), ""));
        }
      }

    } catch (Exception e) {

      // TODO Maybe use the pattern handleUncaughtException
      log.error("Exception while processing request {}", request, e);

      ResponseError error = ResponseError.newFromException(e);
      try {
        rs.sendResponse(new Response<>(request.getId(), error));
      } catch (IOException e1) {
        log.error("Exception sending error to client", e1);
      }
    }
  }

  // TODO Improve this way to obtain the generic parameters in class
  // hierarchies
  public static Class<?> getParamsType(Class<?> handlerClass) {

    Type[] genericInterfaces = handlerClass.getGenericInterfaces();

    for (Type type : genericInterfaces) {

      if (type instanceof ParameterizedType) {
        ParameterizedType parameterized = (ParameterizedType) type;

        if (parameterized.getRawType() == JsonRpcHandler.class) {
          return (Class<?>) parameterized.getActualTypeArguments()[0];
        }
      }
    }

    Type genericSuperclass = handlerClass.getGenericSuperclass();
    if (genericSuperclass != null) {

      if (genericSuperclass instanceof Class) {
        return getParamsType((Class<?>) genericSuperclass);
      }

      ParameterizedType paramClass = (ParameterizedType) genericSuperclass;

      if (paramClass.getRawType() == DefaultJsonRpcHandler.class) {
        return (Class<?>) paramClass.getActualTypeArguments()[0];
      }

      return getParamsType((Class<?>) paramClass.getRawType());

    }

    throw new JsonRpcException("Unable to obtain the type paramter of JsonRpcHandler");
  }

  public void handleTransportError(Session session, Throwable exception) {
    if (handler != null) {
      try {
        handler.handleTransportError(session, exception);
      } catch (Exception e) {
        try {
          handler.handleUncaughtException(session, e);
        } catch (Exception e2) {
          log.error("Exception while executing handleUncaughtException", e2);
        }
      }
    }
  }

  public JsonRpcHandler<?> getHandler() {
    return handler;
  }
}
