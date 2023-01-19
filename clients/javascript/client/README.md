JavaScript Kurento Client
=========================

The project contains the implementation of the JavaScript Kurento Client
for web applications and Node.js.

The source code of this project can be cloned from the [GitHub repository].

supports async-await out of the box

Installation instructions
-------------------------

These instructions are intended for code contributors or people willing to
compile the browser version themselves. If you are a browser-only developer,
it's better that you have a look at the [JavaScript Kurento Client for Bower]
instructions.

### Node.js

Be sure to have installed [Node.js] in your system:

```bash
curl -sSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

To install the library, it's recommended to do that from the [NPM repository] :

```bash
npm install kurento-client
```

Alternatively, or if you want to modify the JavaScript Kurento Client code or
generate yourself the browser version of the library, you can download the
development code files using git and install manually its dependencies:

```bash
git clone https://github.com/Kurento/kurento.git
cd kurento/clients/javascript/client/
npm install
```

In this last case, you will also need to have installed [Kurento Module Creator]
so you can be able to generate the client libraries code.

### Browser

To build the browser version of the library, after downloading the development
code files, you'll only need to exec the [grunt] task runner from the root of
the project and they will be generated on the ```dist``` folder. Alternatively,
if you don't have it globally installed, you can run a local copy by executing

```bash
node_modules/.bin/grunt
```

How to test
-----------
Tests are autonomous and based on [QUnit] testing framework. Their only
requirement is to exec previously ```npm install``` to have installed all the
dev dependencies.

### Browser

After building the web browser version of the library, just open the file
```test/index.html``` with any browser, and the tests will launch automatically.
In case of the browser raise some security policy errors, you can host the tests
code by running any static web server at the source code root folder, for
example by launching the command

```bash
python -m SimpleHTTPServer 8000
```

You can be able to configure to what WebSocket endpoint you want to connect on
the dropdown list at the top of the tests page.

### Node.js

To exec test in Node.js, you only need to exec ```npm test```, that will launch
all the tests automatically using [QUnit-cli]. Note that a Kurento Media Server
instance must be also up, in order to have all tests run against it.

At this moment, the default WebSocket endpoint can not be changed due to limits
of the current implementation of NPM. If you need to use a different WebSocket
endpoint from the default one, you can exec the underlying test command and
append a *ws_uri* parameter pointing to the alternative WebSocket endpoint:

```bash
node_modules/.bin/qunit-cli -c kurentoClient:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js --ws_uri=ws://localhost:8080
```
