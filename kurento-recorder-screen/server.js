#!/usr/bin/env node

const HTTP_PORT  = 7080;
const HTTPS_PORT = 7443;


var fs    = require('fs');
var https = require('https');

var static = require('node-static');


var file = new static.Server('./static');

var options =
{
  key:  fs.readFileSync('keys/server.key'),
  cert: fs.readFileSync('keys/server.crt')
};

https.createServer(options, function(request, response)
{
  request.addListener('end', function()
  {
    file.serve(request, response);
  }).resume();
}).listen(HTTPS_PORT, function()
{
  console.log('HTTPS server available at https://localhost:'+HTTPS_PORT)
});


// Redirect from http to https - http://stackoverflow.com/a/23977269/586382
var http = require('http');

http.createServer(function(req, res)
{
  var host = req.headers['host'].split(':')[0] + ':' + HTTPS_PORT;
  var headers = { "Location": "https://" + host + req.url };

  res.writeHead(301, headers);
  res.end();
}).listen(HTTP_PORT, function()
{
  console.log('HTTP server available at http://localhost:'+HTTP_PORT)
});
