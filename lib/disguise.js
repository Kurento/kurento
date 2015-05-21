/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

/*
 * Disguise an object giving it the appearance of another
 *
 * Add bind'ed functions and properties to an object delegating the actions and
 * updates to the source one so it can act as another one while retaining its
 * original personality (i.e. duplicates and instanceof are preserved)
 */
function disguise(target, source) {
  for (var key in source) {
    if (target[key] !== undefined) continue

    if (typeof source[key] === 'function')
      Object.defineProperty(target, key, {
        value: source[key].bind(source)
      })
    else
      Object.defineProperty(target, key, {
        get: function () {
          return source[key]
        },
        set: function (value) {
          source[key] = value
        }
      })
  }

  return target
}

module.exports = disguise
