%%%%%%%%%%%%%%%%%
Server Deployment
%%%%%%%%%%%%%%%%%

This section explains how to build configure and run the Kurento Repository server
as a standalone application.

Dependencies
############

  * Ubuntu 14.04 LTS
  * Java JDK version 7 or 8
  * :term:`MongoDB` (we provide an :ref:`install guide <mongo-install>`)
  * :term:`Kurento Media Server` or connection with a running instance 
    (to install follow the `official guide <http://www.kurento.org/docs/current/installation_guide.html>`_)

Binaries
########

To build the installation binaries from the source code you'll need
to have installed on your machine Git, Java JDK and :term:`Maven`.

Clone the parent project, **kurento-java** from its 
`GitHub repository <https://github.com/Kurento/kurento-java>`_.

.. sourcecode:: bash

   $ git clone git@github.com:Kurento/kurento-java.git


Then build the **kurento-repository-server** project together 
with its required modules:

.. sourcecode:: bash

   $ cd kurento-java
   $ mvn clean package -DskipTests -Pdefault -am \
      -pl kurento-repository/kurento-repository-server


Now unzip the generated install binaries (where ``x.y.z`` is the current 
version and could include the ``-SNAPSHOT`` suffix):

.. sourcecode:: bash

   $ cd kurento-repository/kurento-repository-server/target
   $ unzip kurento-repository-server-x.y.z.zip

.. _server-configuration:

Configuration
#############

The configuration file, ``kurento-repo.conf.json`` is located in the ``config``
folder inside the uncompressed installation binaries. When installing the
repository as a system service, the configuration files will be located 
after the installation inside ``/etc/kurento``.

.. sourcecode:: bash

   $ cd kurento-repository-server-x.y.z
   $ vim config/kurento-repo.conf.json

The default contents of the configuration file:

.. sourcecode:: json

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

These properties and their values will configure the repository application.

  * ``port`` and ``hostname`` are where the HTTP repository servlet will be 
    listening for incoming connections (REST API).
  * ``type`` indicates the storage type. The repository that stores media served 
    by KMS can be backed by GridFS on MongoDB or it can use file storage 
    directly on the system’s disks (regular filesystem).
  * ``mongodb`` configuration:
    * ``dbname`` is the database name
    * ``gridName`` is the name of the gridfs collection used for the repository
    * ``urlConn`` is the connection to the Mongo database
  * ``filesystem`` configuration:
    * ``folder`` is a local path to be used as media storage

Logging configuration
#####################

The logging configuration is specified by the file 
``kurento-repo-log4j.properties``, also found in the ``config`` folder.

.. sourcecode:: bash

   $ cd kurento-repository-server-x.y.z
   $ vim config/kurento-repo-log4j.properties

In it, the location of the server's output log file can be set up, the default 
location will be ``kurento-repository-server-x.y.z/logs/`` (or
``/var/log/kurento/`` for system-wide installations).

To change it, replace the ``${kurento-repo.log.file}`` variable for an 
absolute path on your system:

.. sourcecode

   log4j.appender.file.File=${kurento-repo.log.file}

Execution
#########

There are two options for running the server:

* user-level execution - doesn’t need additional installation steps, can be 
  done right after uncompressing the installer
* system-level execution - requires installation of the repository 
  application as a system service, which enables automatic startup after 
  system reboots

In both cases, as the application uses the :term:`Spring Boot` framework, it 
executes inside an embedded Tomcat container instance, so there’s no need for 
extra deployment actions (like using a third-party servlet container). If 
required, the project's build configuration could be modified in order to 
generate a *WAR* instead of a *JAR*.

Run at user-level
#################

After having :ref:`configured <server-configuration>` the server instance just execute the start 
script:

.. sourcecode:: bash

   $ cd kurento-repository-server-x.y.z
   $ ./bin/start.sh

Run as daemon
#############

First install the repository after having built and uncompressed the generating
binaries. **sudo** privileges are required to install it as a service:

.. sourcecode:: bash

   $ cd kurento-repository-server-x.y.z
   $ sudo ./bin/install.sh

The service **kurento-repo** will be automatically started.

Now, you can configure the repository as stated in the 
:ref:`previous section <server-configuration>` and restart the service.

.. sourcecode:: bash
   
   $ sudo service kurento-repo {start|stop|status|restart|reload}


Version upgrade
###############

To update to a newer version, it suffices to follow once again the 
installation procedures.

.. _mongo-install:

Installation over MongoDB
#########################

For the sake of testing *kurento-repository* on Ubuntu (*14.04 LTS 64 bits*), 
the default installation of MongoDB is enough. 
Execute the following commands (taken from MongoDB 
`webpage <http://docs.mongodb.org/manual/tutorial/install-mongodb-on-ubuntu/>`_):

.. sourcecode:: bash

   $ sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
   $ echo "deb http://repo.mongodb.org/apt/ubuntu \
      "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" \
      | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list
   $ sudo apt-get update
   $ sudo apt-get install -y mongodb-org
