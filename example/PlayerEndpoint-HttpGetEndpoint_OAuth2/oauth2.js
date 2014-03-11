var querystring= require('querystring'),
    https= require('https'),
    http= require('http'),
    URL= require('url');

exports.OAuth2= function(clientId, clientSecret, baseSite, authorizePath, accessTokenPath, callbackURL, customHeaders) {
  this._clientId= clientId;
  this._clientSecret= clientSecret;
  this._baseSite= baseSite;
  this._authorizeUrl= authorizePath || "/oauth/authorize";
  this._accessTokenUrl= accessTokenPath || "/oauth/access_token";
  this._callbackURL = callbackURL;
  this._accessTokenName= "access_token";
  this._authMethod= "Basic";
  this._customHeaders = customHeaders || {};
}

// This 'hack' method is required for sites that don't use
// 'access_token' as the name of the access token (for requests).
// ( http://tools.ietf.org/html/draft-ietf-oauth-v2-16#section-7 )
// it isn't clear what the correct value should be atm, so allowing
// for specific (temporary?) override for now.
exports.OAuth2.prototype.setAccessTokenName= function ( name ) {
  this._accessTokenName= name;
}

exports.OAuth2.prototype._getAccessTokenUrl= function() {
  return this._baseSite + this._accessTokenUrl;
}

// Build the authorization header. In particular, build the part after the colon.
// e.g. Authorization: Bearer <token>  # Build "Bearer <token>"
exports.OAuth2.prototype.buildAuthHeader= function() {
  var key = this._clientId + ':' + this._clientSecret;
  var base64 = (new Buffer(key)).toString('base64');
  return this._authMethod + ' ' + base64;
};

exports.OAuth2.prototype._request= function(method, url, headers, post_body, access_token, callback) {

  var http_library= https;
  var parsedUrl= URL.parse( url, true );
  if( parsedUrl.protocol == "https:" && !parsedUrl.port ) {
    parsedUrl.port= 443;
  }

  // As this is OAUth2, we *assume* https unless told explicitly otherwise.
  if( parsedUrl.protocol != "https:" ) {
    http_library= http;
  }

  var realHeaders= {};
  for( var key in this._customHeaders ) {
    realHeaders[key]= this._customHeaders[key];
  }
  if( headers ) {
    for(var key in headers) {
      realHeaders[key] = headers[key];
    }
  }
  realHeaders['Host']= parsedUrl.host;

  //realHeaders['Content-Length']= post_body ? Buffer.byteLength(post_body) : 0;
  if( access_token && !('Authorization' in realHeaders)) {
    if( ! parsedUrl.query ) parsedUrl.query= {};
    parsedUrl.query[this._accessTokenName]= access_token;
  }

  var queryStr= querystring.stringify(parsedUrl.query);
  if( queryStr ) queryStr=  "?" + queryStr;
  var options = {
    host:parsedUrl.hostname,
    port: parsedUrl.port,
    path: parsedUrl.pathname + queryStr,
    method: method,
    headers: realHeaders
  };

  this._executeRequest( http_library, options, post_body, callback );
}

exports.OAuth2.prototype._executeRequest= function( http_library, options, post_body, callback ) {
  // Some hosts *cough* google appear to close the connection early / send no content-length header
  // allow this behaviour.
  var allowEarlyClose= options.host && options.host.match(".*google(apis)?.com$");
  var callbackCalled= false;
  function passBackControl( response, result ) {
    if(!callbackCalled) {
      callbackCalled=true;
      if( response.statusCode != 200 && (response.statusCode != 301) && (response.statusCode != 302) ) {
        callback({ statusCode: response.statusCode, data: result });
      } else {
        callback(null, result, response);
      }
    }
  }

  var result= "";

  var request = http_library.request(options, function (response) {
    response.on("data", function (chunk) {
      result+= chunk
    });
    response.on("close", function (err) {
      if( allowEarlyClose ) {
        passBackControl( response, result );
      }
    });
    response.addListener("end", function () {
      passBackControl( response, result );
    });
  });
  request.on('error', function(e) {
    callbackCalled= true;
    callback(e);
  });

  if(options.method == 'POST' && post_body) {
     request.write(post_body);
  }
  request.end();  
}

exports.OAuth2.prototype.getAuthorizeUrl= function() {
  return this._baseSite + this._authorizeUrl + '?response_type=code&client_id=' + this._clientId + '&state=xyz&redirect_uri=' + this._callbackURL;
}

exports.OAuth2.prototype.getOAuthAccessToken= function(code, callback) {

  var post_data = 'grant_type=authorization_code&code=' + code + '&redirect_uri=' + this._callbackURL;

  var post_headers= {
       'Authorization': this.buildAuthHeader(),
       'Content-Type': 'application/x-www-form-urlencoded'
   };

  this._request("POST", this._getAccessTokenUrl(), post_headers, post_data, null, function(error, data, response) {
    if( error )  callback(error);
    else {
      var results;
      try {
        // As of http://tools.ietf.org/html/draft-ietf-oauth-v2-07
        // responses should be in JSON
        results= JSON.parse(data);
      }
      catch(e) {
        // .... However both Facebook + Github currently use rev05 of the spec
        // and neither seem to specify a content-type correctly in their response headers :(
        // clients of these services will suffer a *minor* performance cost of the exception
        // being thrown
        results= querystring.parse(data);
      }
      callback(null, results);
    }
  });
}

exports.OAuth2.prototype.get= function(url, access_token, callback) {
  this._request("GET", url, {}, "", access_token, callback);
}
