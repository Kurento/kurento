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

package org.kurento.orion;

import org.kurento.commons.exception.KurentoException;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class OrionConnectorException extends KurentoException {

  private static final long serialVersionUID = 1981989759033830188L;

  /**
   * default constructor.
   */
  public OrionConnectorException() {
    // Default constructor
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not
   * initialised, and may subsequently be initialised by a call to initCause.
   *
   * @param msg
   *          the detail message. The detail message is saved for later retrieval by the
   *          {@link #getMessage()} method.
   */
  public OrionConnectorException(final String msg) {
    super(msg);
  }

  /**
   *
   * @param msg
   *          the detail message. The detail message is saved for later retrieval by the
   *          {@link #getMessage()} method.
   * @param throwable
   *          the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
   *          null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public OrionConnectorException(final String msg, final Throwable throwable) {
    super(msg, throwable);
  }

  /**
   *
   * @param cause
   *          the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
   *          null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public OrionConnectorException(final Throwable cause) {
    super(cause);
  }

}
