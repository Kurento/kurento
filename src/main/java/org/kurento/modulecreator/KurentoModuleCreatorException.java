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
package org.kurento.modulecreator;

public class KurentoModuleCreatorException extends RuntimeException {

  private static final long serialVersionUID = -5373430051337208460L;

  public KurentoModuleCreatorException() {
  }

  public KurentoModuleCreatorException(String message) {
    super(message);
  }

  public KurentoModuleCreatorException(Throwable cause) {
    super(cause);
  }

  public KurentoModuleCreatorException(String message, Throwable cause) {
    super(message, cause);
  }

  public KurentoModuleCreatorException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
