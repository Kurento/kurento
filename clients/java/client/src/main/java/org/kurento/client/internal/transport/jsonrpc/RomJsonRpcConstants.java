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

package org.kurento.client.internal.transport.jsonrpc;

public class RomJsonRpcConstants {

  public static final String CREATE_METHOD = "create";
  public static final String CREATE_CONSTRUCTOR_PARAMS = "constructorParams";
  public static final String CREATE_PROPERTIES = "properties";
  public static final String CREATE_TYPE = "type";

  public static final String INVOKE_METHOD = "invoke";
  public static final String INVOKE_OPERATION_PARAMS = "operationParams";
  public static final String INVOKE_OPERATION_NAME = "operation";
  public static final String INVOKE_OBJECT = "object";

  public static final String RELEASE_METHOD = "release";
  public static final String RELEASE_OBJECT = "object";

  public static final String ONEVENT_METHOD = "onEvent";
  public static final String ONEVENT_OBJECT = "object";
  public static final String ONEVENT_TYPE = "type";
  public static final String ONEVENT_DATA = "data";
  public static final String ONEVENT_SUBSCRIPTION = "subscription";

  public static final String SUBSCRIBE_METHOD = "subscribe";
  public static final String SUBSCRIBE_OBJECT = "object";
  public static final String SUBSCRIBE_TYPE = "type";

  public static final String UNSUBSCRIBE_METHOD = "unsubscribe";
  public static final String UNSUBSCRIBE_OBJECT = "object";
  public static final String UNSUBSCRIBE_LISTENER = "subscription";

  public static final String TRANSACTION_METHOD = "transaction";
  public static final String TRANSACTION_OPERATIONS = "operations";

}
