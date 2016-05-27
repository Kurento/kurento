/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.latency;

import java.util.Observable;
import java.util.Observer;

/**
 * Change color observable (notifies observers when a color change is detected in the browser).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ChangeColorObservable extends Observable {

  @SuppressWarnings("unchecked")
  public void addListener(final ChangeColorEventListener<? extends ChangeColorEvent> listener) {
    addObserver(new Observer() {
      @Override
      public void update(Observable obs, Object e) {
        ChangeColorEvent changeColorEvent = (ChangeColorEvent) e;
        ((ChangeColorEventListener<ChangeColorEvent>) listener).onEvent(changeColorEvent);
      }
    });
  }

  public void detectedColorChange(ChangeColorEvent event) {
    setChanged();
    notifyObservers(event);
  }

}
