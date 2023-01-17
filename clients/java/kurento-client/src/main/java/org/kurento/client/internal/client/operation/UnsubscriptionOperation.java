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

package org.kurento.client.internal.client.operation;

import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.ListenerSubscriptionImpl;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class UnsubscriptionOperation extends Operation {

  private KurentoObject kurentoObject;
  private ListenerSubscriptionImpl listenerSubscription;

  public UnsubscriptionOperation(KurentoObject kurentoObject,
      ListenerSubscriptionImpl listenerSubscription) {
    this.listenerSubscription = listenerSubscription;
    this.kurentoObject = kurentoObject;
  }

  public ListenerSubscriptionImpl getListenerSubscription() {
    return listenerSubscription;
  }

  public KurentoObject getKurentoObject() {
    return kurentoObject;
  }

  @Override
  public RequestAndResponseType createRequest(RomClientJsonRpcClient romClientJsonRpcClient) {

    return romClientJsonRpcClient.createUnsubscribeRequest(getObjectRef(kurentoObject),
        listenerSubscription.getSubscriptionId());
  }

  @Override
  public void processResponse(Object response) {
    // There is nothing to do here.
  }

  @Override
  public String getDescription() {
    return "Event " + listenerSubscription.getType() + " unsubscription";
  }

}
