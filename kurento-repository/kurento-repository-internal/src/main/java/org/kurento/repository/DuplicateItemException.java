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

import org.kurento.commons.exception.KurentoException;

/**
 * This exception is thrown when the user is trying to create a repository item with the same id
 * than existing repository item.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 *
 */
public class DuplicateItemException extends KurentoException {

  private static final long serialVersionUID = 3515920000618086477L;

  public DuplicateItemException(String id) {
    super("An item with id " + id + " already exists");
  }

}
