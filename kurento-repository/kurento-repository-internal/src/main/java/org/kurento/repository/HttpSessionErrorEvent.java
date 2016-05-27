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

package org.kurento.repository;

/**
 * This class represents an event fired when an error is detected in the
 * {@link RepositoryHttpEndpoint} identified as source.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
public class HttpSessionErrorEvent extends RepositoryHttpSessionEvent {

  private String description;
  private Throwable cause;

  public HttpSessionErrorEvent(RepositoryHttpEndpoint source, String description) {
    super(source);
  }

  public HttpSessionErrorEvent(RepositoryHttpEndpoint source, Throwable cause) {
    super(source);
    this.cause = cause;
    this.description = cause.getMessage();
  }

  /**
   * Returns the exception that caused this error or null if the error is not produced by means of
   * an exception.
   *
   * @return the exception or null if the error is not produced by means of an exception
   */
  public Throwable getCause() {
    return cause;
  }

  /**
   * Returns the description of the error. This description can be used to log the problem.
   *
   * @return the description of the error.
   */
  public String getDescription() {
    return description;
  }

}
