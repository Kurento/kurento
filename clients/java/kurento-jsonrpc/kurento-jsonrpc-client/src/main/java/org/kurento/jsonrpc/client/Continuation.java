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

package org.kurento.jsonrpc.client;

/**
 * This interface is to be used in asynchronous calls to the media server.
 *
 * @param <F>
 *          The data type of the callback´s response in case of successful outcome.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface Continuation<F> {

  /**
   * This method is called when the operation succeeds
   * 
   * @param result
   */
  void onSuccess(F result);

  /**
   * This method gets called when the operation fails
   * 
   * @param cause
   *          The cause of the failure
   */
  void onError(Throwable cause);

}
