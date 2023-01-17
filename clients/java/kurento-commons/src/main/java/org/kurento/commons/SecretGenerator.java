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

package org.kurento.commons;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Random word (integer) generator.
 *
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class SecretGenerator {

  /**
   * Secure random generator object.
   */
  private static SecureRandom secureRandom = new SecureRandom();

  /**
   * Random word generator.
   *
   * @return Generated word
   */
  public String nextSecret() {
    // SecureRandom is thread safe, so no synchronization issues here 10
    return new BigInteger(130, secureRandom).toString(32);
  }
}
