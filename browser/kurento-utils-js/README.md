Kurento Utils for Node.js and Browsers
======================================

> :warning: **Warning**
>
> This library is not actively maintained. It was written to simplify the [Kurento Tutorials](https://doc-kurento.readthedocs.io/en/latest/user/tutorials.html) and has several shortcomings for more advanced uses.
>
> For real-world applications we recommend to **avoid using this library** and instead to write your JavaScript code directly against the browser’s [WebRTC API](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API).

*kurento-utils-js* is a browser library that can be used to simplify creation and handling of [RTCPeerConnection](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection) objects, to control the browser’s [WebRTC API](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API).



Installation instructions
-------------------------

Be sure to have installed [Node.js](https://nodejs.org/en/) and [Bower](https://bower.io/) in your system:

```bash
curl -sSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
```

To install the library, it's recommended to do that from the [NPM repository](https://www.npmjs.com/):

```bash
npm install kurento-utils
```

Alternatively, you can download the code using git and install manually its dependencies:

```bash
git clone https://github.com/Kurento/kurento.git
cd kurento/browser/kurento-utils-js/
npm install
```

Screen and window sharing depends on the privative module `kurento-browser-extensions`. To enable its support, you'll need to install
the package dependency manually or use a `getScreenConstraints` function yourself on runtime. If it's not available, when trying to share the screen or a window content it will throw an exception.



### Browser

To build the browser version of the library you'll only need to exec the [grunt](https://gruntjs.com) task runner and they will be generated on the `dist` folder. Alternatively, if you don't have it globally installed, you can run a local copy by executing:

```bash
node_modules/.bin/grunt
```



Acknowledges
------------

* [Bertrand CHEVRIER](https://github.com/krampstudio) for
  [grunt-jsdoc](https://github.com/krampstudio/grunt-jsdoc)
