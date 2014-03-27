.. _building:

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Building Kurento Media Server
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: sh

Requirements
============

.. note::
    These instructions are prepared for Ubuntu 13.10

    If you have an Intel graphical card, you should probably do
    ``sudo apt-get install ocl-icd-libopencl1`` before starting.
    See :ref:`FAQ question <intel_nvidia>`.



You need to execute the following commands:


::

     $ sudo add-apt-repository ppa:kurento/kurento
     $ sudo apt-get update
     $ sudo apt-get install libthrift-dev thrift-compiler
     $ sudo apt-get install gstreamer1.0* libgstreamer1.0-dev
     $ sudo apt-get install libgstreamer-plugins-base1.0-dev libnice-dev gtk-doc-tools
     $ sudo apt-get install cmake libglibmm-2.4-dev uuid-dev libevent-dev libboost-dev
     $ sudo apt-get install libboost-system-dev libboost-filesystem-dev
     $ sudo apt-get install libopencv-dev autoconf git
     $ sudo apt-get install libtool libsoup2.4-dev tesseract-ocr-dev tesseract-ocr-eng
     $ sudo apt-get install libgnutls28-dev gnutls-bin libvpx-dev

Compilation
===========

::

    $ mkdir build
    $ cd build
    $ cmake ..
    $ make

Installation and running
========================

Installation

::

    $ git submodule update --recursive --init
    $ dpkg-buildpackage -us -uc
    $ sudo dpkg -i ../kurento_<version>_<arch>.deb

Running the server

::

    $ sudo /etc/init.d/kurento start

Logs are written to ``/var/log/kurento/kurento.log``

Server configuration files:

::

    /etc/default/kurento
    /etc/kurento/kurento.conf
    /etc/kurento/pattern.sdp

