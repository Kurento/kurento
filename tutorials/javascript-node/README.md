Kurento tutorial for Node.js
============================
Examples on usage of the Kurento Node.js Client.

This project contains a set of simple applications built with JavaScript Kurento
Client APIs ([kurento-client-js] and [kurento-utils-js]) for [Node.js].

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] in your system:

```bash
curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
sudo apt-get install -y nodejs
```

It is recommended to update NPM to the latest version:

```bash
sudo npm install npm -g
```


Install node modules and bower components

```bash
npm install
```

Run the application and have fun ...

```bash
npm start
```

Parameters
----------

The Node.js server accept an optional parameter with the URI of the MediaServer
WebSocket endpoint, being set by default at ws://localhost:8888/kurento. You can
define its value by using the ```ws_uri``` flag:

```bash
npm start -- --ws_uri=ws://example.com:8888/kurento
```

It also accept an optional parameter with the URI of the application server root
that will serve the overlay image, being by default at https://localhost:8443/.
You can define its value by using the ```as_uri``` flag:

```bash
npm start -- --as_uri=https://example.org:8443/
```

For example, if you would like to start the node server in the localhost using
the port 8081, then the command is the following:

```bash
npm start -- --as_uri=https://localhost:8081/
```

Please notice that the double dash separator (```--```) is [on
purpose](https://www.npmjs.org/doc/cli/npm-run-script.html#description).

