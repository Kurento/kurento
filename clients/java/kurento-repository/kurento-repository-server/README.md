[![License badge](https://img.shields.io/badge/license-Apache2-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

kurento-repository-server
=========================

> :warning: **Warning**
>
> This module is not actively maintained.
>
> All content here is available for legacy reasons, but no support is provided at all, and you'll be on your own if you decide to use it.

The server module of Kurento Repository is a [Spring Boot][SpringBoot]
application which exposes the configured media repository through an
easy-to-use Http REST API.

This module can also be integrated into existing applications instead
of a standalone execution. The application will have access to a Java
API which wraps around the repository internal library.

There is a Kurento Java [tutorial application][helloworld-repository] that
employs a running instance of this server to record and play media over HTTP
using the capabilities of the Kurento Media Server.

Table of Contents
-----------------

- [Running the server](#running-the-server)
  - [Dependencies](#dependencies)
  - [Binaries](#binaries)
  - [Configuration](#configuration)
    - [Logging configuration](#logging-configuration)
  - [Execution](#execution)
    - [Run at user-level](#run-at-user-level)
    - [Run as daemon](#run-as-daemon)
  - [Version upgrade](#version-upgrade)
  - [Installation over MongoDB](#installation-over-mongodb)
- [Http REST API](#http-rest-api)
  - [Create repository item](#create-repository-item)
  - [Remove repository item](#remove-repository-item)
  - [Get repository item read endpoint](#get-repository-item-read-endpoint)
  - [Find repository items by metadata](#find-repository-items-by-metadata)
  - [Find repository items by metadata regex](#find-repository-items-by-metadata-regex)
  - [Get the metadata of a repository item](#get-the-metadata-of-a-repository-item)
  - [Update the metadata of a repository item](#update-the-metadata-of-a-repository-item)
- [Repository Rest Java API](#repository-rest-java-api)
- [What is Kurento](#what-is-kurento)

Running the server
------------------

### Dependencies

  * Ubuntu 14.04 LTS
  * [Java JDK][Java] version 7 or 8
  * [MongoDB][mongo] (we provide an install [guide](#installation-over-mongodb))
  * Kurento Media Server or connection with a running instance
    (to install follow the [official guide][kurento-install])

### Binaries

To build the installation binaries from the source code you'll need
to have installed on your machine [Git][Git], [Java JDK][Java]
and [Maven][Maven].

Clone the parent project, `kurento-java` from its
[GitHub Repository][GitHub Kurento Java].

```
$ git clone git@github.com:Kurento/kurento-java.git
```

Then build the `kurento-repository-server` project together
with its required modules:

```
$ cd kurento-java
$ mvn clean package -DskipTests -Pdefault -am \
    -pl kurento-repository/kurento-repository-server
```

Now unzip the generated install binaries (where `x.y.z` is the current
version and could include the `-SNAPSHOT` suffix):

```
$ cd kurento-repository/kurento-repository-server/target
$ unzip kurento-repository-server-x.y.z.zip
```

### Configuration

The configuration file, `kurento-repo.conf.json` is located in the `config`
folder inside the uncompressed installation binaries. When installing the
repository as a system service, the configuration files will be located
after the installation inside `/etc/kurento`.

```
$ cd kurento-repository-server-x.y.z
$ vim config/kurento-repo.conf.json
```

The default contents of the configuration file:

```json
{
  "repository": {
    "port": 7676,
    "hostname": "127.0.0.1",

    //mongodb or filesystem
    "type": "mongodb",

    "mongodb": {
      "dbName": "kurento",
      "gridName": "kfs",
      "urlConn": "mongodb://localhost"
    },
    "filesystem": {
      "folder": "/tmp/repository"
    }
  }
}
```

These properties and their values will configure the repository application.

  * `port` and `hostname` are where the HTTP repository servlet will be
    listening for incoming connections (REST API).
  * `type` indicates the storage type. The repository that stores media served
    by KMS can be backed by GridFS on MongoDB or it can use file storage
    directly on the system’s disks (regular filesystem).
  * `mongodb` configuration:
    * `dbname` is the database name
    * `gridName` is the name of the gridfs collection used for the repository
    * `urlConn` is the connection to the Mongo database
  * `filesystem` configuration:
    * `folder` is a local path to be used as media storage

#### Logging configuration

The logging configuration is specified by the file
`kurento-repo-log4j.properties`, also found in the `config` folder.

```
$ cd kurento-repository-server-x.y.z
$ vim config/kurento-repo-log4j.properties
```

In it, the location of the server's output log file can be set up, the default
location will be `kurento-repository-server-x.y.z/logs/` (or
`/var/log/kurento/` for system-wide installations).

To change it, replace the `${kurento-repo.log.file}` variable for an
absolute path on your system:

```
log4j.appender.file.File=${kurento-repo.log.file}
```

### Execution

There are two options for running the server:

  * user-level execution - doesn’t need additional installation steps, can be
    done right after uncompressing the installer
  * system-level execution - requires installation of the repository
    application as a system service, which enables automatic startup after
    system reboots

In both cases, as the application uses the [Spring Boot][SpringBoot]
framework, it executes inside an embedded Tomcat container instance, so
there’s no need for extra deployment actions (like using a third-party
servlet container). If required, the project's build configuration could
be modified in order to generate a *WAR* instead of a *JAR*.

#### Run at user-level

After having [configured](#configuration) the server instance just execute the start
script:

```
$ cd kurento-repository-server-x.y.z
$ ./bin/start.sh
```

#### Run as daemon

First install the repository after having built and uncompressed the generating
binaries. **sudo** privileges are required to install it as a service:

```
$ cd kurento-repository-server-x.y.z
$ sudo ./bin/install.sh
```

The service **kurento-repo** will be automatically started.

Now, you can configure the repository as stated in the
[previous section](#configuration) and restart the service.

```
$ sudo service kurento-repo {start|stop|status|restart|reload}
```

### Version upgrade

To update to a newer version, it suffices to follow once again the
installation procedures.

### Installation over MongoDB

For the sake of testing *kurento-repository* on Ubuntu (*14.04 LTS 64 bits*),
the default installation of MongoDB is enough.
Execute the following commands (from MongoDB [webpage][mongo-install]):

```
$ sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
$ echo "deb http://repo.mongodb.org/apt/ubuntu \
    "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" \
    | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list
$ sudo apt-get update
$ sudo apt-get install -y mongodb-org
```

Http REST API
-------------

Primitives provided by the repository server, can be used to control items from
the respository (*add*, *delete*, *search*, *update*, *get download URL*).

### Create repository item

**Description**

Creates a new repository item with the provided metadata and its associated
recorder endpoint.

**Request method and URL**

```
POST /repo/item
```

**Request Content-Type**

```
application/json
```

**Request parameters**

Pairs of key-value Strings in JSON format (a representation of the Java object
`Map<String, String>`).

|Parameter|Type|Description|
|---------|----|-----------|
|`keyN`  |O   |Metadata associated to `keyN`|

> *M=Mandatory, O=Optional*

```json
{
  "key1": "value1",
  "key2": "value2",
  ...
}
```

**Response elements**

Returns an entity of type `application/json` including a POJO of type
`RepositoryItemRecorder` with the following information:

|Element|Type|Description|
|-------|----|-----------|
|`id`   |M   |Public ID of the newly created item|
|`url`  |M   |URL of the item’s recording Http endpoint|

> *M=Mandatory, O=Optional*

```json
{
  "id": "Item's public ID",
  "url": "Recorder Http endpoint"
}
```

**Response Codes**

|Code|Description|
|----|-----------|
|200 OK|New item created and ready for recording.|

### Remove repository item

**Description**

Removes the repository item associated to the provided id.

**Request method and URL**

```
DELETE /repo/item/{itemId}
```

**Request Content-Type**

```
NONE
```

**Request parameters**

The item’s ID is coded in the URL’s path info.

|Parameter|Type|Description|
|---------|----|-----------|
|`itemId`|M   |Repository item’s identifier|

> *M=Mandatory, O=Optional*

**Response elements**

```
NONE
```

**Response Codes**

|Code|Description|
|----|-----------|
|200 OK|Item successfully deleted.|
|404 Not Found|Item does not exist.|

### Get repository item read endpoint

**Description**

Obtains a new endpoint for reading (playing multimedia) from the repository item.

**Request method and URL**

```
GET /repo/item/{itemId}
```

**Request Content-Type**

```
NONE
```

**Request parameters**

The item’s ID is coded in the URL’s path info.

|Parameter|Type|Description|
|---------|----|-----------|
|`itemId`|M   |Repository item’s identifier|

> *M=Mandatory, O=Optional*

**Response elements**
Returns an entity of type `application/json` including a POJO of type
`RepositoryItemPlayer` with the following information:

|Element|Type|Description|
|-------|----|-----------|
|`id`   |M   |Public ID of the newly created item|
|`url`  |M   |URL of the item’s reading (playing) Http endpoint|

> *M=Mandatory, O=Optional*

```json
{
  "id": "Item's public ID",
  "url": "Player Http endpoint"
}
```

**Response Codes**

|Code|Description|
|----|-----------|
|200 OK|New player item created.|
|404 Not Found|Item does not exist.|

### Find repository items by metadata

**Description**

Searches for repository items by each pair of attributes and their exact values.

**Request method and URL**

```
POST /repo/item/find
```

**Request Content-Type**

```
application/json
```

**Request parameters**

Pairs of key-value Strings in JSON format (a representation of the Java object
`Map<String, String>`).

|Parameter|Type|Description|
|---------|----|-----------|
|`searchKeyN`|M   |Metadata associated to `searchKeyN`|

> *M=Mandatory, O=Optional*

```json
{
  "searchKey1": "searchValue1",
  "searchKey2": "searchValue2",
  ...
}
```

**Response elements**

Returns an entity of type `application/json` including a POJO of type
`Set<String>` with the following information:

|Element|Type|Description|
|-------|----|-----------|
|`idN`  |O   |Id of the N-th repository item whose metadata matches one of the search terms|

> *M=Mandatory, O=Optional*

```json
[ "id1", "id2" ... ]
```

**Response Codes**

|Code  |Description|
|------|-----------|
|200 OK|Query successfully executed.|

### Find repository items by metadata regex

**Description**

Searches for repository items by each pair of attributes and their values which
can represent a regular expression (Perl compatible regular expressions -
[PCRE]).

**Request method and URL**

```
POST /repo/item/find/regex
```

**Request Content-Type**

```
application/json
```

**Request parameters**

Pairs of key-value Strings in JSON format (a representation of the Java object
`Map<String, String>`).

|Parameter|Type|Description|
|---------|----|-----------|
|`searchKeyN`|M   |Regex for metadata associated to  `searchKeyN`|

> *M=Mandatory, O=Optional*

```json
{
  "searchKey1": "searchRegex1",
  "searchKey2": "searchRegex2",
  ...
}
```

**Response elements**

Returns an entity of type `application/json` including a POJO of type
`Set<String>` with the following information:

|Element|Type|Description|
|-------|----|-----------|
|`idN`  |O   |Id of the N-th repository item whose metadata matches one of the search terms|

> *M=Mandatory, O=Optional*

```json
[ "id1", "id2" ... ]
```

**Response Codes**

|Code  |Description|
|------|-----------|
|200 OK|Query successfully executed.|

### Get the metadata of a repository item

**Description**

Returns the metadata from a repository item.

**Request method and URL**

```
GET /repo/item/{itemId}/metadata
```

**Request Content-Type**

```
NONE
```

**Request parameters**

The item’s ID is coded in the URL’s path info.

|Parameter|Type|Description|
|---------|----|-----------|
|`itemId`|M   |Repository item’s identifier|

> *M=Mandatory, O=Optional*

**Response elements**

Returns an entity of type `application/json` including a POJO of type
`Map<String, String>` with the following information:

|Element|Type|Description|
|-------|----|-----------|
|`keyN` |O   |Metadata associated to `keyN`|

> *M=Mandatory, O=Optional*

```json
{
  "key1": "value1",
  "key2": "value2",
  ...
}
```

**Response Codes**

|Code|Description|
|----|-----------|
|200 OK|Query successfully executed.|
|404 Not Found|Item does not exist.|

### Update the metadata of a repository item

**Description**

Replaces the metadata of a repository item with the provided values from the
request’s body.

**Request method and URL**

```
PUT /repo/item/{itemId}/metadata
```

**Request Content-Type**

```
application/json
```

**Request parameters**

The item’s ID is coded in the URL’s path info and the request’s body contains
key-value Strings in JSON format (a representation of the Java object
`Map<String, String>`).

|Parameter|Type|Description|
|---------|----|-----------|
|`itemId`|M   |Repository item’s identifier|
|`keyN`  |O   |Metadata associated to `keyN`|

> *M=Mandatory, O=Optional*

**Response elements**

```
NONE
```

**Response Codes**

|Code|Description|
|----|-----------|
|200 OK|Item successfully updated.|
|404 Not Found|Item does not exist.|

Repository Rest Java API
------------------------

This API is used directly by the REST interface layer, so the Java primitives
mirror the REST ones.

The only difference is that to use this API, it is required to include a
dependency on `kurento-repository-server` and to use the [Spring] framework.

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
[GitHub Kurento Java]: https://github.com/Kurento/kurento-java
[kurento-install]: https://kurento.openvidu.io/docs/current/installation_guide.html
[helloworld-repository]: https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world-recording
[mongo]: https://www.mongodb.org/
[mongo-install]: http://docs.mongodb.org/manual/tutorial/install-mongodb-on-ubuntu/
[Spring]: https://spring.io/
[SpringBoot]: http://projects.spring.io/spring-boot/
[Git]: https://git-scm.com/
[Java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[Maven]: https://maven.apache.org/
[PCRE]: http://php.net/manual/en/book.pcre.php
