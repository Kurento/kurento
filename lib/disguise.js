/**
 * Generic `Promise.catch()` method
 *
 * It delegate its functionality on the `then()` of the object where it's
 * applied, both directly or on its class definition prototype
 *
 * @param {Function} [onRejected]
 *
 * @return {Promise}
 */
function promiseCatch(onRejected) {
  return this.then(null, onRejected)
}

//
// Public API
//

/**
 * Disguise an object giving it the appearance of another
 *
 * Add bind'ed functions and properties to a `target` object delegating the
 * actions and attributes updates to the `source` one while retaining its
 * original personality (i.e. duplicates and `instanceof` are preserved)
 *
 * @param {Object} target - the object to be disguised
 * @param {Object} source - the object where to fetch its methods and attributes
 * @param {Object} [unthenable] - the returned object should not be a thenable
 *
 * @return {Object} `target` disguised
 */
function disguise(target, source, unthenable) {
  if (source == null || target === source) return target

  for (var key in source) {
    if (target[key] !== undefined) continue
    if (unthenable && (key === 'then' || key === 'catch')) continue

    if (typeof source[key] === 'function')
      var descriptor = {
        value: source[key]
      }
    else
      var descriptor = {
        get: function () {
          return source[key]
        },
        set: function (value) {
          source[key] = value
        }
      }

    descriptor.enumerable = true

    Object.defineProperty(target, key, descriptor)
  }
  return target
}

/**
 * Disguise a thenable object
 *
 * If available, `target.then()` gets replaced by a method that exec the
 * `onFulfilled` and `onRejected` callbacks using `source` as `this` object, and
 * return the Promise returned by the original `target.then()` method already
 * disguised. It also add a `target.catch()` method pointing to the newly added
 * `target.then()`, being it previously available or not.
 *
 * @param {thenable} target - the object to be disguised
 * @param {Object} source - the object where to fetch its methods and attributes
 *
 * @return {thenable} `target` disguised
 */
function disguiseThenable(target, source) {
  if (target === source) return target

  if (target.then instanceof Function) {
    var target_then = target.then

    function then(onFulfilled, onRejected) {
      if (onFulfilled != null) onFulfilled = onFulfilled.bind(target)
      if (onRejected != null) onRejected = onRejected.bind(target)

      var promise = target_then.call(target, onFulfilled, onRejected)

      return disguiseThenable(promise, source)
    }

    Object.defineProperties(target, {
      then: {
        value: then
      },
      catch: {
        value: promiseCatch
      }
    })
  }

  return disguise(target, source)
}

/**
 * Return a copy of the input object without `.then()` and `.catch()`
 *
 * @param {thenable} input
 *
 * @return {Object} unthenabled input object
 */
function unthenable(input) {
  var output = Object.assign({}, input)
  delete output.then
  if (input !== undefined)
    output.constructor = input.constructor

  if (input && input.then instanceof Function) return disguise(output, input,
    true)

  // `input` is not thenable
  return input
}

disguiseThenable.disguise = disguise
disguiseThenable.disguiseThenable = disguiseThenable
disguiseThenable.unthenable = unthenable

module.exports = disguiseThenable
