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
 * Interface to be implemented by {@link MediaEvent} listeners. Implementors of this interface will
 * be on charge of processing the events raised by media elements.
 *
 * @param <T>
 *          A class that extends from {@link Event}
 *
 * @author Luis López (llopez@gsyc.es), Ivan Gracia (igracia@gsyc.es)
 *
 **/
public interface EventListener<T extends Event> {
  /**
   * Called from the framework when an event is raised at the media server.
   *
   * @param event
   *          a T event
   *
   */
  void onEvent(T event);
}
