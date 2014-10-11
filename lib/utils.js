/**
 * Get the constructor for a type
 *
 * If the type is not registered, use generic {module:core/abstracts.MediaObject}
 */
function getConstructor(type)
{
  var result = register.classes[type] || register.abstracts[type];
  if(result) return result;

  console.warn("Unknown type '"+type+"', using MediaObject instead");
  return MediaObject;
};

function createPromise(data, func, callback)
{
  var promise = new Promise(function(resolve, reject)
  {
    function callback2(error, result)
    {
      if(error) return reject(error);

      resolve(result);
    };

    if(data instanceof Array)
      async.map(data, func, callback2);
    else
      func(data, callback2);
  });

  return promiseCallback(promise, callback);
};
