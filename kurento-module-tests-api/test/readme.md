[![License badge](https://img.shields.io/badge/license-Apache2-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

JavaScript Kurento Module Tests API
===================================
Tests are autonomous, only requirement is to exec previously ```npm install```
to have installed all the dev dependencies.

## Environments

### Node.js

To exec test in Node.js, you only need to exec ```npm test``` that will launch
all the tests automatically using [QUnit-cli].

If you need to use a WebSocket endpoint different from the default one, you can exec the underlying test command with
```node_modules/.bin/qunit-cli -c kurentoClient:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js``` and append the *ws_uri* parameter.


[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[website]: http://kurento.org
