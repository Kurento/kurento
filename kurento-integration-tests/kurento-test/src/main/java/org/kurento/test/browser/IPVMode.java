/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.test.browser;

/**
 * Type of internet protocol version
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public enum IPVMode {
  IPV4, IPV6, BOTH;

  @Override
  public String toString() {
    switch (this) {
      case IPV4:
        return "IPV4";
      case IPV6:
        return "IPV6";
      case BOTH:
        return "BOTH";
      default:
        throw new IllegalArgumentException();
    }
  }
}
