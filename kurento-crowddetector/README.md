[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento crowd detector Node.js tutorial
=======================================

This project detect when there's a crowd in front of the webcam.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] in your system:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
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

The double dash separator (```--```) is [on purposse](https://www.npmjs.org/doc/cli/npm-run-script.html#description).


[GitHub Repository]: https://github.com/Kurento/kurento-tutorial-node
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org
[website]: http://kurento.org
