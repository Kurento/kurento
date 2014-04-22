var express = require('express');
var OAuth2 = require('./oauth2').OAuth2;
var config = require('./config');


// Express configuration
var app = express();
app.use(express.logger());
app.use(express.bodyParser());
app.use(express.cookieParser());
app.use(express.session({
    secret: "skjghskdjfhbqigohqdiouk"
}));

app.configure(function()
{
  "use strict";

  app.use(express.errorHandler({ dumpExceptions: true, showStack: true }));
  //app.use(express.logger());
  app.use(express.static(__dirname + '/public'));
});


// Config data from config.js file
var client_id = config.client_id;
var client_secret = config.client_secret;
var idmURL = config.idmURL;
var callbackURL = config.callbackURL;

// Creates oauth library object with the config data
var oa = new OAuth2(client_id,
                    client_secret,
                    idmURL,
                    '/oauth2/authorize',
                    '/oauth2/token',
                    callbackURL);


// Handles requests to the main page
app.get('/', function(req, res)
{
  var file = req.session.access_token ? 'logged.html' : 'notLogged.html';

  res.sendfile(file);
});

// Redirection to IDM authentication portal
app.get('/auth', function(req, res)
{
  var path = oa.getAuthorizeUrl();
  res.redirect(path);
});

// Handles requests from IDM with the access code
app.get('/login', function(req, res)
{
  // Using the access code goes again to the IDM to obtain the access_token
  oa.getOAuthAccessToken(req.query.code, function(error, results)
  {
    // Stores the access_token in a session cookie
    req.session.access_token = results.access_token;

    res.redirect('/?access_token='+req.session.access_token);
  });
});

// Handles logout requests to remove access_token from the session cookie
app.get('/logout', function(req, res)
{
  req.session.access_token = undefined;
  res.redirect('/');
});


app.listen(80);
console.log('Server listen in port 80. Connect to localhost');
