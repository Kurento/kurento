[![License badge](https://img.shields.io/badge/license-Apache2-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

kurento-hello-world-repository
==============================

> :warning: **Warning**
>
> This tutorial is not actively maintained. It was written to showcase the use of [Kurento Repository Server][repository], which itself if not maintained either.
>
> All content here is available for legacy reasons, but no support is provided at all, and you'll be on your own if you decide to use it.

Kurento Java Tutorial: Hello World (WebRTC in loopback) with recording and
storage of media in the kurento-repository-server through the
kurento-repository-client (i.e. the Java API to handle the Repository Server).

Requires a running instance of [Kurento Repository Server][repository] so that
the streamed media (from webcam and microphone) is recorded and played by
Kurento Media Server using a media repository. In turn, the repository will be
backed up by a MongoDB database or by a filesystem (see the server's
[configuration][repository-cfg]).


Configuration
-------------

The tutorial has to know the repository's location so that it might access its
Http REST API. This configuration property is `repository.uri` and has the
default value `http://localhost:7676`.

Another required property is the URI of the Kurento Media Server, `kms.ws.uri`,
with the default value `ws://localhost:8888/kurento`.

Running the tutorial
--------------------

In order to run this tutorial, please read the following [instructions].

After cloning the tutorial, it can be executed directly from the terminal by
using Maven's `exec` plugin (`[...]` are optional):

```
$ git clone git@github.com:Kurento/kurento-tutorial-java.git
$ cd kurento-tutorial-java/kurento-hello-world-recording
$ mvn -U clean spring-boot:run [-Drepository.uri=http://localhost:7676] \
     [-Dkms.url=ws://localhost:8888/kurento]
```

### Dependencies ###

If using a *SNAPSHOT* version (e.g. latest commit from **master** branch), the
project `kurento-java` is also required to exist (built and installed) in the
local Maven repository.

```
$ git clone git@github.com:Kurento/kurento-java.git
$ cd kurento-java
$ mvn clean install -DskipTests -Pdefault
```

What is Kurento
---------------

Kurento is an open source software project providing a platform suitable
for creating modular applications with advanced real-time communication
capabilities. For knowing more about Kurento, please visit the Kurento
project website: https://kurento.openvidu.io/.

Kurento is part of [FIWARE]. For further information on the relationship of
FIWARE and Kurento check the [Kurento FIWARE Catalog Entry]

Kurento is part of the [NUBOMEDIA] research initiative.

Documentation
-------------

The Kurento project provides detailed [documentation] including tutorials,
installation and development guides. A simplified version of the documentation
can be found on [readthedocs.org]. The [Open API specification] a.k.a. Kurento
Protocol is also available on [apiary.io].

Source
------

Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------

Check the [Kurento blog]
Follow us on Twitter @[kurentoms].

Issue tracker
-------------

Issues and bug reports should be posted to the [GitHub Kurento bugtracker]

Licensing and distribution
--------------------------

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contribution policy
-------------------

You can contribute to the Kurento community through bug-reports, bug-fixes, new
code or new documentation. For contributing to the Kurento community, drop a
post to the [Kurento Public Mailing List] providing full information about your
contribution and its value. In your contributions, you must comply with the
following guidelines

* You must specify the specific contents of your contribution either through a
  detailed bug description, through a pull-request or through a patch.
* You must specify the licensing restrictions of the code you contribute.
* For newly created code to be incorporated in the Kurento code-base, you must
  accept Kurento to own the code copyright, so that its open source nature is
  guaranteed.
* You must justify appropriately the need and value of your contribution. The
  Kurento project has no obligations in relation to accepting contributions
  from third parties.
* The Kurento project leaders have the right of asking for further
  explanations, tests or validations of any code contributed to the community
  before it being incorporated into the Kurento code-base. You must be ready to
  addressing all these kind of concerns before having your code approved.

Support
-------

The Kurento project provides community support through the  [Kurento Public
Mailing List] and through [StackOverflow] using the tags *kurento* and
*fiware-kurento*.

Before asking for support, please read first the [Kurento Netiquette Guidelines]


[documentation]: https://kurento.openvidu.io/documentation
[FIWARE]: http://www.fiware.org
[GitHub Kurento bugtracker]: https://github.com/Kurento/bugtracker/issues
[GitHub Kurento Group]: https://github.com/kurento
[kurentoms]: http://twitter.com/kurentoms
[Kurento]: https://kurento.openvidu.io/
[Kurento Blog]: https://kurento.openvidu.io/blog
[Kurento FIWARE Catalog Entry]: http://catalogue.fiware.org/enablers/stream-oriented-kurento
[Kurento Netiquette Guidelines]: https://kurento.openvidu.io/blog/kurento-netiquette-guidelines
[Kurento Public Mailing list]: https://groups.google.com/forum/#!forum/kurento
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[NUBOMEDIA]: http://www.nubomedia.eu
[StackOverflow]: http://stackoverflow.com/search?q=kurento
[Read-the-docs]: http://read-the-docs.readthedocs.org/
[readthedocs.org]: http://kurento.readthedocs.org/
[Open API specification]: http://kurento.github.io/doc-kurento/
[apiary.io]: http://docs.streamoriented.apiary.io/
[repository]: https://github.com/Kurento/kurento-java/tree/master/kurento-repository/kurento-repository-server
[repository-cfg]: https://github.com/Kurento/kurento-java/tree/master/kurento-repository/kurento-repository-server#configuration
[instructions]: http://doc-kurento.readthedocs.org/en/stable/tutorials/java/tutorial-repository.html
