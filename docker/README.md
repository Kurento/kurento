[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [LGPL v2.1 License].

docker
======

# How to use this Dockerfile

You can build a docker image based on this Dockerfile. This image will contain
only an Stream oriented kurento instance, exposing port 8888. This requires
that you have docker installed on your machine.

If you just want to have an Stream oriented kurento running as quickly as
possible jump to section The Fastest Way.

If you want to know what is behind the scenes of our container you can go ahead
and read the build and run sections.

## The Fastest Way

### Run a container from an image you just built

If you have downloaded the [Stream oriented
kurento's](https://github.com/kurento/kurento-docker/) code simply navigate to
the docker directory and run

    sudo docker build -t fiware/stream-oriented-kurento .
    sudo docker run fiware/stream-oriented-kurento

This will build a new docker image and store it locally as
kurento/kurento-media-server. Then, it starts the created image in the frontend.

The parameter `-t fiware/stream-oriented-kurento` gives the image a name. This
name could be anything, or even include an organization like
`-t org/fiware-kurento`. This name is later used to run the container based on
the image.

If you want to know more about images and the building process you can find it
in [Docker's documentation](https://docs.docker.com/userguide/dockerimages/).

### Run a container pulling an image from the cloud (recommended)

If you do not have or want to download the Stream oriented kurento repository,
you can run stream-oriented-kurento directly:

    sudo docker run fiware/stream-oriented-kurento

This way is equivalent to the previous one, except that it pulls the image from
the Docker Registry instead of building your own. Keep in mind though that
everything is run locally.

> **Note**
> If you do not want to have to use `sudo` in this or in the next section follow [these instructions](http://askubuntu.com/questions/477551/how-can-i-use-docker-without-sudo).

### Run the container

The following line will run the container exposing port `8888`, giving it a name
-in this case `kurento`:

	  sudo docker run -d --name kurento -p 8888:8888 fiware/stream-oriented-kurento

As a result of this command, there is a stream-oriented-kurento listening on
port 8888 on localhost. Try to see if it works now with

curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Host: 127.0.0.1:8888" -H "Origin: 127.0.0.1" http://127.0.0.1:8888 | grep -q "Server: WebSocket++"

Stream oriented kurento is run by default with debug level 5
(```GST_DEBUG=Kurento*:5```). You can change the debug level by passing in the environment variable GST_DEBUG with -e "GST_DEBUG=<log level>".

## Get help about kurento media server

    docker run --rm fiware/stream-oriented-kurento --help

Kurento
=======

What is Kurento
---------------

Kurento is an open source software project providing a platform suitable 
for creating modular applications with advanced real-time communication
capabilities. For knowing more about Kurento, please visit the Kurento
project website: http://www.kurento.org.

Kurento is part of [FIWARE]. For further information on the relationship of 
FIWARE and Kurento check the [Kurento FIWARE Catalog Entry]

Kurento is part of the [NUBOMEDIA] research initiative.

Documentation
-------------

The Kurento project provides detailed [documentation] including tutorials,
installation and development guides. A simplified version of the documentation
can be found on [readthedocs.org]. The [Open API specification] (a.k.a. Kurento
Protocol) is also available on [apiary.io].

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

Software associated to Kurento is provided as open source under GNU Library or
"Lesser" General Public License, version 2.1 (LGPL-2.1). Please check the
specific terms and conditions linked to this open source license at
http://opensource.org/licenses/LGPL-2.1. Please note that software derived as a
result of modifying the source code of Kurento software in order to fix a bug
or incorporate enhancements is considered a derivative work of the product.
Software that merely uses or aggregates (i.e. links to) an otherwise unmodified
version of existing software is not considered a derivative work.

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

[documentation]: http://www.kurento.org/documentation
[FIWARE]: http://www.fiware.org
[GitHub Kurento bugtracker]: https://github.com/Kurento/bugtracker/issues
[GitHub Kurento Group]: https://github.com/kurento
[kurentoms]: http://twitter.com/kurentoms
[Kurento]: http://kurento.org
[Kurento Blog]: http://www.kurento.org/blog
[Kurento FIWARE Catalog Entry]: http://catalogue.fiware.org/enablers/stream-oriented-kurento
[Kurento Netiquette Guidelines]: http://www.kurento.org/blog/kurento-netiquette-guidelines
[Kurento Public Mailing list]: https://groups.google.com/forum/#!forum/kurento
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[LGPL v2.1 License]: http://www.gnu.org/licenses/lgpl-2.1.html
[NUBOMEDIA]: http://www.nubomedia.eu
[StackOverflow]: http://stackoverflow.com/search?q=kurento
[Docker]: https://www.docker.com/
[Read-the-docs]: http://read-the-docs.readthedocs.org/
[readthedocs.org]: http://kurento.readthedocs.org/
[Open API specification]: http://kurento.github.io/doc-kurento/
[apiary.io]: http://docs.streamoriented.apiary.io/
