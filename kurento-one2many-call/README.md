[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento one to many node tutorial
============
Kurento Web SDK demos

This project makes possible one client to upstream to the server a WebRTC stream so that it can be distributed to other clients.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] in your system:

```bash
sudo add-apt-repository ppa:chris-lea/node.js
sudo apt-get update
sudo apt-get install nodejs
```
Also be sure to have installed [Bower] in your system:

```bash
sudo npm install -g bower
```

Install node modules and bower components

```bash
npm install
cd public
bower install
```

Run the application and have fun ...

```bash
node app.js
```