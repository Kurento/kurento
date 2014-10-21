/**
 * Loader for the kurento-client package on the browser
 */

if(typeof kurentoClient == 'undefined')
  window.kurentoClient = require('kurento-client');
//  window.kurentoClient = require('./index.js');

var oldRequire = (require instanceof Function) ? require
: function(id)
{
  throw new Error("require() is undefined, '"+id+"' couldn't be imported");
};

window.require = function(id)
{
  if(id === 'kurento-client') return kurentoClient;

  return oldRequire(id);
};
