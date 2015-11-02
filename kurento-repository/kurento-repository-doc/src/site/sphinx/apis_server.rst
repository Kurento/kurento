%%%%%%%%%%%
Server APIs
%%%%%%%%%%%

This section details the REST API through which an application can communicate
with the Kurento Repository server.

There's also the possibility to integrate the server as a Spring component and
in this case the class ``org.kurento.repository.RepositoryService`` can be used 
to control the Repository as an instance local to the application. The REST API
maps over the service's one, so the methods and parameters involved are the 
exact same ones. 

.. _server-rest-api:

HTTP REST API
-------------

Primitives provided by the repository server, can be used to control items from 
the repository (*add*, *delete*, *search*, *update*, *get download URL*).

.. contents:: :local: 
   :backlinks: entry

Create repository item
######################

- **Description**: Creates a new repository item with the provided metadata and 
  its associated recorder endpoint.
- **Request method and URL**: ``POST /repo/item``
- **Request Content-Type**: ``application/json``
- **Request parameters**: Pairs of key-value Strings in JSON format (a 
  representation of the Java object ``Map<String, String>``).

 +-----------+-------+---------------------------------+  
 | Parameter | Type  | Description                     |
 +===========+=======+=================================+
 | ``keyN``  | **O** | Metadata associated to ``keyN`` |
 +-----------+-------+---------------------------------+
 | *M=Mandatory, O=Optional*                           |
 +-----------------------------------------------------+

- **Request parameters example**:

 .. sourcecode:: json

    {
        "key1": "value1",
        "keyN": "valueN"
    }

- **Response elements**: Returns an entity of type ``application/json`` 
  including a POJO of type ``RepositoryItemRecorder`` with the following 
  information:

 +-----------+-------+-------------------------------------------+ 
 | Element   | Type  | Description                               |
 +===========+=======+===========================================+
 | ``id``    | **M** | Public ID of the newly created item       |
 +-----------+-------+-------------------------------------------+
 | ``url``   | **M** | URL of the item's recording Http endpoint |
 +-----------+-------+-------------------------------------------+
 | *M=Mandatory, O=Optional*                                     |
 +---------------------------------------------------------------+

- **Response example**:

 .. sourcecode :: json

    {
        "id": "Item's public ID",
        "url": "Recorder Http endpoint"
    }

- **Response Codes**:

 +--------+-------------------------------------------+
 | Code   | Description                               |
 +========+===========================================+
 | 200 OK | New item created and ready for recording. |
 +--------+-------------------------------------------+

Remove repository item
######################

- **Description**: Removes the repository item associated to the provided id.
- **Request method and URL**: ``DELETE /repo/item/{itemId}``
- **Request Content-Type**: ``NONE``
- **Request parameters**: The item’s ID is coded in the URL’s path info.

 +-----------+-------+------------------------------+  
 | Parameter | Type  | Description                  |
 +===========+=======+==============================+
 | ``itemId``| **M** | Repository item's identifier |
 +-----------+-------+------------------------------+
 | *M=Mandatory, O=Optional*                        |
 +--------------------------------------------------+

- **Response elements**: ``NONE``
- **Response Codes**:

 +---------------+----------------------------+
 | Code          | Description                |
 +===============+============================+
 | 200 OK        | Item successfully deleted. |
 +---------------+----------------------------+
 | 404 Not Found | Item does not exist.       |
 +---------------+----------------------------+

Get repository item read endpoint
#################################

- **Description**: Obtains a new endpoint for reading (playing 
  :term:`multimedia`) from the repository item.
- **Request method and URL**: ``GET /repo/item/{itemId}``
- **Request Content-Type**: ``NONE``
- **Request parameters**: The item’s ID is coded in the URL’s path info.

 +-----------+-------+------------------------------+  
 | Parameter | Type  | Description                  |
 +===========+=======+==============================+
 | ``itemId``| **M** | Repository item's identifier |
 +-----------+-------+------------------------------+
 | *M=Mandatory, O=Optional*                        |
 +--------------------------------------------------+

- **Response elements**: Returns an entity of type ``application/json`` 
  including a POJO of type ``RepositoryItemPlayer`` with the following 
  information:

 +-----------+-------+---------------------------------------------------+ 
 | Element   | Type  | Description                                       |
 +===========+=======+===================================================+
 | ``id``    | **M** | Public ID of the newly created item               |
 +-----------+-------+---------------------------------------------------+
 | ``url``   | **M** | URL of the item's reading (playing) Http endpoint |
 +-----------+-------+---------------------------------------------------+
 | *M=Mandatory, O=Optional*                                             |
 +-----------------------------------------------------------------------+

- **Response example**:

 .. sourcecode :: json

    {
        "id": "Item's public ID",
        "url": "Player Http endpoint"
    }

- **Response Codes**:

 +---------------+--------------------------+
 | Code          | Description              |
 +===============+==========================+
 | 200 OK        | New player item created. |
 +---------------+--------------------------+
 | 404 Not Found | Item does not exist.     |
 +---------------+--------------------------+

Find repository items by metadata
#################################

- **Description**: Searches for repository items by each pair of attributes and 
  their exact values.
- **Request method and URL**: ``POST /repo/item/find``
- **Request Content-Type**: ``application/json``
- **Request parameters**: Pairs of key-value Strings in JSON format (a 
  representation of the Java object ``Map<String, String>``).

 +---------------+-------+---------------------------------------+  
 | Parameter     | Type  | Description                           |
 +===============+=======+=======================================+
 | ``searchKeyN``| **M** | Metadata associated to ``searchKeyN`` |
 +---------------+-------+---------------------------------------+
 | *M=Mandatory, O=Optional*                                     |
 +---------------------------------------------------------------+

- **Request parameters example**:

 .. sourcecode:: json

    {
        "searchKey1": "searchValue1",
        "searchKeyN": "searchValueN"
    }

- **Response elements**: Returns an entity of type ``application/json`` including 
  a POJO of type ``Set<String>`` with the following information:

 +---------+-------+---------------------------------------------------+ 
 | Element | Type  | Description                                       |
 +=========+=======+===================================================+
 | ``idN`` | **O** | | Id of the N-th repository item whose metadata   |
 |         |       | | matches one of the search terms                 |
 +---------+-------+---------------------------------------------------+
 | *M=Mandatory, O=Optional*                                           |
 +---------------------------------------------------------------------+

- **Response example**:

 .. sourcecode :: json

    [ "id1", "idN" ]

- **Response Codes**:

 +--------+------------------------------+
 | Code   | Description                  |
 +========+==============================+
 | 200 OK | Query successfully executed. |
 +--------+------------------------------+

Find repository items by metadata regex
#######################################

- **Description**: Searches for repository items by each pair of attributes and 
  their values which can represent a regular expression (
  `Perl compatible regular expressions <http://php.net/manual/en/book.pcre.php>`_).
- **Request method and URL**: ``POST /repo/item/find/regex``
- **Request Content-Type**: ``application/json``
- **Request parameters**: Pairs of key-value Strings in JSON format (a 
  representation of the Java object ``Map<String, String>``).

 +---------------+-------+-------------------------------------------------+  
 | Parameter     | Type  | Description                                     |
 +===============+=======+=================================================+
 | ``searchKeyN``| **M** | Regex for metadata associated to ``searchKeyN`` |
 +---------------+-------+-------------------------------------------------+
 | *M=Mandatory, O=Optional*                                               |
 +-------------------------------------------------------------------------+

- **Request parameters example**:

 .. sourcecode:: json

    {
        "searchKey1": "searchRegex1",
        "searchKeyN": "searchRegexN"
    }

- **Response elements**: Returns an entity of type ``application/json`` including 
  a POJO of type ``Set<String>`` with the following information:

 +---------+-------+---------------------------------------------------+ 
 | Element | Type  | Description                                       |
 +=========+=======+===================================================+
 | ``idN`` | **O** | | Id of the N-th repository item whose metadata   |
 |         |       | | matches one of the search terms                 |
 +---------+-------+---------------------------------------------------+
 | *M=Mandatory, O=Optional*                                           |
 +---------------------------------------------------------------------+

- **Response example**:

 .. sourcecode :: json

    [ "id1", "idN" ]

- **Response Codes**:

 +--------+------------------------------+
 | Code   | Description                  |
 +========+==============================+
 | 200 OK | Query successfully executed. |
 +--------+------------------------------+

Get the metadata of a repository item
#####################################

- **Description**: Returns the metadata from a repository item.
- **Request method and URL**: ``GET /repo/item/{itemId}/metadata``
- **Request Content-Type**: ``NONE``
- **Request parameters**: The item’s ID is coded in the URL’s path info.

 +-----------+-------+------------------------------+  
 | Parameter | Type  | Description                  |
 +===========+=======+==============================+
 | ``itemId``| **M** | Repository item's identifier |
 +-----------+-------+------------------------------+
 | *M=Mandatory, O=Optional*                        |
 +--------------------------------------------------+

- **Response elements**: Returns an entity of type ``application/json`` 
  including a POJO of type ``Map<String, String>`` with the following information:


 +----------+-------+---------------------------------+ 
 | Element  | Type  | Description                     |
 +==========+=======+=================================+
 | ``keyN`` | **O** | Metadata associated to ``keyN`` |
 +----------+-------+---------------------------------+
 | *M=Mandatory, O=Optional*                          |
 +----------------------------------------------------+

- **Response example**:

 .. sourcecode :: json

    {
        "key1": "value1",
        "keyN": "valueN"
    }

- **Response Codes**:

 +---------------+------------------------------+
 | Code          | Description                  |
 +===============+==============================+
 | 200 OK        | Query successfully executed. |
 +---------------+------------------------------+
 | 404 Not Found | Item does not exist.         |
 +---------------+------------------------------+

Update the metadata of a repository item
########################################

- **Description**: Replaces the metadata of a repository item with the provided 
  values from the request’s body.
- **Request method and URL**: ``PUT /repo/item/{itemId}/metadata``
- **Request Content-Type**: ``application/json``
- **Request parameters**: The item’s ID is coded in the URL’s path info and the 
  request’s body contains key-value Strings in JSON format (a representation of 
  the Java object ``Map<String, String>``).

 +------------+-------+---------------------------------+  
 | Parameter  | Type  | Description                     |
 +============+=======+=================================+
 | ``itemId`` | **M** | Repository item's identifier    |
 +------------+-------+---------------------------------+
 | ``keyN``   | **O** | Metadata associated to ``keyN`` |
 +------------+-------+---------------------------------+
 | *M=Mandatory, O=Optional*                            |
 +------------------------------------------------------+

- **Request parameters example**:

 .. sourcecode:: json

    {
        "key1": "value1",
        "keyN": "valueN"
    }

- **Response elements**: ``NONE``
- **Response Codes**:

 +---------------+----------------------------+
 | Code          | Description                |
 +===============+============================+
 | 200 OK        | Item successfully updated. |
 +---------------+----------------------------+
 | 404 Not Found | Item does not exist.       |
 +---------------+----------------------------+
