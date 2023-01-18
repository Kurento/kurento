Kurento jsonrpc library for Node.js and browsers
================================================

Kurento Web SDK RPC Builder.

The Kurento Web SDK RPC Builer project is a small RPC library for browser and Node.js.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed the Node.js tools in your system. It's heavily
encouraged to use the latest Node.js and NPM versions from the
[Node.js project PPA] instead of the packages available on the oficial Ubuntu
repositories, since due to the fast-moving Node.js community and environment
these last ones get easily outdated and can lead to incompatibility errors:

```bash
sudo add-apt-repository ppa:chris-lea/node.js
sudo apt-get update

sudo apt-get install nodejs nodejs-legacy
```

To install the library, it's recomended to do that from the [NPM repository] :

```bash
npm install kurento-jsonrpc
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kurento.git
cd kurento/clients/javascript/jsonrpc/
npm install
```

### Browser

To build the browser version of the library you'll only need to exec the [grunt]
task runner and they will be generated on the ```dist``` folder. Alternatively,
if you don't have it globally installed, you can run a local copy by executing

```bash
node_modules/.bin/grunt
```

How to test
-----------
Tests are autonomous and based on [nodeunit] testing framework. Their only
requirement is to exec previously ```npm install``` to have installed all the
dev dependencies.

### Node.js

To exec test in Node.js, you only need to exec ```npm test```, that will launch
all the tests automatically.

