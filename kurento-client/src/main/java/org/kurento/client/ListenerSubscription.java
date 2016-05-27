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


package org.kurento.client;

/**
 * Interface to be implemented by objects that represent the subscription to an event in Kurento.
 * Implementers of this interface may be used by the system to track listeners of events registered
 * by users. Subscribing to a certain {@link MediaEvent} raised by a {@link MediaObject} generates a
 * {@code ListenerSubscription}, that can be used by the client to unregister this listener.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface ListenerSubscription {

  /**
   * Returns the registration id for this listener.
   *
   * @return The id
   */
  String getSubscriptionId();

}
