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

package org.kurento.client.internal.client;

import org.kurento.client.ListenerSubscription;

public class ListenerSubscriptionImpl implements ListenerSubscription {

  private String subscriptionId;
  private String type;
  private RemoteObjectEventListener listener;

  public ListenerSubscriptionImpl(String subscription, String type,
      RemoteObjectEventListener listener) {
    this.subscriptionId = subscription;
    this.type = type;
    this.listener = listener;
  }

  public ListenerSubscriptionImpl(String type, RemoteObjectEventListener listener) {
    this.type = type;
    this.listener = listener;
  }

  public String getSubscription() {
    return subscriptionId;
  }

  public String getType() {
    return type;
  }

  public RemoteObjectEventListener getListener() {
    return listener;
  }

  @Override
  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscription(String subscription) {
    this.subscriptionId = subscription;
  }
}
