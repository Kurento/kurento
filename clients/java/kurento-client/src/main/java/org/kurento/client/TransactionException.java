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

import org.kurento.commons.exception.KurentoException;

public class TransactionException extends KurentoException {

  private static final long serialVersionUID = 1876787972925582820L;

  /**
   * default constructor.
   */
  public TransactionException() {
    // Default constructor
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not
   * initialized, and may subsequently be initialized by a call to initCause.
   *
   * @param msg
   *          the detail message. The detail message is saved for later retrieval by the
   *          {@link #getMessage()} method.
   */
  public TransactionException(final String msg) {
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
  public TransactionException(final String msg, final Throwable throwable) {
    super(msg, throwable);
  }

  /**
   *
   * @param throwable
   *          the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
   *          null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public TransactionException(final Throwable throwable) {
    super(throwable);
  }

}
