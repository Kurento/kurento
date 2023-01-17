===========================
NAT Types and NAT Traversal
===========================

.. contents:: Table of Contents

Sources:

- :wikipedia:`Network address translation`
- `Symmetric NAT and It's Problems <https://www.think-like-a-computer.com/2011/09/19/symmetric-nat/>`__ (`archive <https://archive.is/jt4c2>`__)
- `Peer-to-Peer Communication Across Network Address Translators <https://bford.info/pub/net/p2pnat/>`__ (`archive <https://archive.is/u7His>`__)
- `The hole trick - How Skype & Co. get round firewalls <http://www.h-online.com/security/features/How-Skype-Co-get-round-firewalls-747197.html>`__ (`archive <https://archive.is/NTvAl>`__)
- `What type of NAT combinations requires a TURN server? <https://stackoverflow.com/questions/31424904/what-type-of-nat-combinations-requires-a-turn-server>`__
- `Under what scenarios Server- and Peer-Reflexive candidates differ? <https://stackoverflow.com/questions/19905239/under-what-scenarios-does-server-reflexive-and-peer-reflexive-addresses-candidat>`__



Basic Concepts
==============

Transport Address
-----------------

A *Transport Address* is the combination of a host's IP address and a port. When talking about transmission of packets between hosts, we'll refer to them in terms of the transport addresses of both the source and the destination.



Packet transmission
-------------------

In order to talk about traffic flows in this document, we'll refer to packet transmissions as the fact of sending data packets from a source to a destination host. The transmission is represented by the transport addresses of each host, plus a direction:

.. code-block:: text

   (SRC_ADDR, SRC_PORT) -> (DST_ADDR, DST_PORT)

.. note::

   - ``->`` denotes the direction of the transmission.
   - ``(ADDR, PORT)`` denotes a transport address.
   - ``(SRC_ADDR, SRC_PORT)`` is the transport address of the host that sends packets.
   - ``(DST_ADDR, DST_PORT)`` is the transport address of the host that receives packets.



Inbound NAT transmission
------------------------

A :term:`NAT` maintains **inbound** rules which are used to translate transport addresses between the NAT's *external* and *internal* sides. Usually, the **external** side of the NAT is facing the public internet (**WAN**), while the **internal** side of the NAT is facing a private local network (**LAN**).

The inbound rules have the form of a *hash table* (or *map*), which stores a direct relationship between a pair of external transport addresses (a *quadruplet*) and a uniquely corresponding internal transport address. In other words:

- Given the quadruplet formed by the NAT external transport address, and a remote host's transport address ...
- ... there is an inbound rule for an internal transport address.

Typically, these NAT rules are created automatically during an outbound transmission that originated from within the LAN towards some remote host: it is at that moment when the NAT creates a new entry into its table (this is **step 1** in the following visualizations). Later, this entry in the NAT table is used to decide which local host needs to receive the response that the remote host might send (this is **step 2** in the visualizations). Rules created in this way are called "*dynamic rules*"; on the other hand, "*static rules*" can be explicitly created by an administrator, in order to set up a fixed NAT table.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote host}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT) <- (REM_ADDR, REM_PORT) ]

Meaning: Some host initiated an outbound packet transmission from the IP address INT_ADDR and port INT_PORT, towards the remote host at REM_ADDR and port REM_PORT. When the first packet crossed the NAT, it automatically created a dynamic rule, translating the internal transport address (INT_ADDR, INT_PORT) into the external transport address (EXT_ADDR, EXT_PORT). EXT_ADDR is the external IP address of the NAT, while EXT_PORT might be the same as INT_PORT, or it might be different (that is up to the NAT to decide).

.. note::

   - ``->`` and ``<-`` denote the direction of the transmission in each step.
   - ``=>`` denotes the creation of a new rule in the NAT table.
   - ``[ (ADDR, PORT), (ADDR, PORT) ]``, with square brackets, denotes the transport address quadruplet used to access the NAT mapping table.
   - ``<=`` denotes the resolution of the NAT mapping.
   - ``(INT_ADDR, INT_PORT)`` is the **source** transport address on the *internal side* of the NAT for a local host making a transmission during step 1, and it is the **destination** transport address for the same host receiving the transmission during step 2.
   - ``(EXT_ADDR, EXT_PORT)`` is the **source** transport address on the *external side* of the NAT from where a transmission is originated during step 1, and it is the **destination** transport address for the transmission being received during step 2.
   - ``(REM_ADDR, REM_PORT)`` is the **destination** transport address of some remote host receiving a transmission during step 1, and it is the **source** transport address of a remote host that makes a transmission during step 2.



Types of NAT
============

There are two categories of NAT behavior, namely **Cone** and **Symmetric** NAT. The crucial difference between them is that the former will use the same port numbers for internal and external transport addresses, while the latter will always use different numbers for each side of the NAT. This will be explained later in more detail.

Besides, there are 3 types of Cone NATs, with varying degrees of restrictions regarding the allowed sources of inbound transmissions. To connect with a local host which is behind a Cone NAT, it's first required that the local host performs an outbound transmission to a remote one. This way, a dynamic rule will be created for the destination transport address, allowing the remote host to connect back. The only exception is the Full Cone NAT, where a static rule can be created beforehand by an administrator, thanks to the fact that this kind of NAT ignores what is the source transport address of the remote host that is connecting.



Full Cone NAT
-------------

This type of NAT allows inbound transmissions from *any source IP address* and *any source port*, as long as the destination tuple exists in a previously created rule.

Typically, these rules are statically created beforehand by an administrator. These are the kind of rules that are used to configure *Port Forwarding* (aka. "*opening the ports*") in most consumer-grade routers. Of course, as it is the case for all NAT types, it is also possible to create dynamic rules by first performing an outbound transmission.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote host}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (   *    ,    *    ) ]

.. note::

   - ``*`` means that any value could be used: a remote host can connect from *any* IP address and port.
   - The **source** IP address (*REM_ADDR*) in step 2 can be different from the **destination** IP address that was used in step 1.
   - The **source** IP port (*REM_PORT*) in step 2 can be different from the **destination** IP port that was used in step 1.
   - The *same* port (*INT_PORT*) is used in the internal and the external sides of the NAT. This is the most common case for all Cone NATs, only being different for Symmetric NATs.



(Address-)Restricted Cone NAT
-----------------------------

This type of NAT allows inbound transmissions from a *specific source IP address* but allows for *any source port*. Typically, an inbound rule of this type was previously created dynamically, when the local host initiated an outbound transmission to a remote one.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote host}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (REM_ADDR,    *    ) ]

.. note::

   - The **source** IP address (*REM_ADDR*) in step 2 must be the same as the **destination** IP address that was used in step 1.
   - The **source** IP port (*REM_PORT*) in step 2 can be different from the **destination** IP port that was used in step 1.
   - The *same* port (*INT_PORT*) is used in the internal and the external sides of the NAT.



Port-Restricted Cone NAT
------------------------

This is the most restrictive type of Cone NAT: it only allows inbound transmissions from a *specific source IP address* and a *specific source port*. Again, an inbound rule of this type was previously created dynamically, when the local host initiated an outbound transmission to a remote one.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote host}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (REM_ADDR, REM_PORT) ]

.. note::

   - The **source** IP address (*REM_ADDR*) in step 2 must be the same as the **destination** IP address that was used in step 1.
   - The **source** IP port (*REM_PORT*) in step 2 must be the same as the **destination** IP port that was used in step 1.
   - The *same* port (*INT_PORT*) is used in the internal and the external sides of the NAT.



.. _nat-symmetric:

Symmetric NAT
-------------

This type of NAT behaves in the same way of a Port-Restricted Cone NAT, with an important difference: for each outbound transmission to a different remote transport address (i.e. to a different remote host), the NAT assigns a **new random source port** on the external side. This means that two consecutive transmissions from the same local port to two different remote hosts will have two different external source ports, even if the internal source transport address is the same for both of them.

This is also the only case where the ICE connectivity protocol will find `Peer Reflexive candidates <https://tools.ietf.org/html/rfc5245#section-7.1.3.2.1>`__ which differ from the Server Reflexive ones, due to the differing ports between the transmission to the :term:`STUN` server and the direct transmission between peers.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote host}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT1) -> (REM_ADDR, REM_PORT1) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT1) <- (REM_ADDR, REM_PORT1) ]
   ...
   3. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT2) -> (REM_ADDR, REM_PORT2) ]
   4. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT2) <- (REM_ADDR, REM_PORT2) ]

.. note::

   - When the outbound transmission is done in step 1, *EXT_PORT1* gets defined as a new random port number, assigned for the new remote transport address *(REM_ADDR, REM_PORT1)*.
   - Later, another outbound transmission is done in step 3, from the same local address and port to the same remote host but at a different port. *EXT_PORT2* is a new random port number, assigned for the new remote transport address *(REM_ADDR, REM_PORT2)*.



Types of NAT in the Real World
==============================

Quoting from :wikipedia:`Wikipedia <en,Network_address_translation#Methods_of_translation>`:

    This terminology has been the source of much confusion, as it has proven inadequate at describing real-life NAT behavior. Many NAT implementations combine these types, and it is, therefore, better to refer to specific individual NAT behaviors instead of using the Cone/Symmetric terminology. :rfc:`4787` attempts to alleviate this issue by introducing standardized terminology for observed behaviors. For the first bullet in each row of the above table, the RFC would characterize Full-Cone, Restricted-Cone, and Port-Restricted Cone NATs as having an *Endpoint-Independent Mapping*, whereas it would characterize a Symmetric NAT as having an *Address-* and *Port-Dependent Mapping*. For the second bullet in each row of the above table, :rfc:`4787` would also label Full-Cone NAT as having an *Endpoint-Independent Filtering*, Restricted-Cone NAT as having an *Address-Dependent Filtering*, Port-Restricted Cone NAT as having an *Address and Port-Dependent Filtering*, and Symmetric NAT as having either an *Address-Dependent Filtering* or *Address and Port-Dependent Filtering*. There are other classifications of NAT behavior mentioned, such as whether they preserve ports, when and how mappings are refreshed, whether external mappings can be used by internal hosts (i.e., its :wikipedia:`Hairpinning` behavior), and the level of determinism NATs exhibit when applying all these rules.[2]

    Especially, most NATs combine *symmetric NAT* for outbound transmissions with *static port mapping*, where inbound packets addressed to the external address and port are redirected to a specific internal address and port. Some products can redirect packets to several internal hosts, e.g., to divide the load between a few servers. However, this introduces problems with more sophisticated communications that have many interconnected packets, and thus is rarely used.



NAT Traversal
=============

The NAT mechanism is implemented in a vast majority of home and corporate routers, and it completely prevents the possibility of running any kind of server software in a local host that sits behind these kinds of devices. NAT Traversal, also known as *Hole Punching*, is the procedure of opening an inbound port in the NAT tables of these routers.

To connect with a local host which is behind any type of NAT, it's first required that the local host performs an outbound transmission to the remote one. This way, a dynamic rule will be created for the destination transport address, allowing the remote host to connect back.

In order to tell one host when it has to perform an outbound transmission to another one, and the destination transport address it must use, the typical solution is to use a helper service such as :term:`STUN`. This is usually managed by a third host, a server sitting on a public internet address. It retrieves the external IP and port of each peer, and gives that information to the other peers that want to communicate.

:term:`STUN` / :term:`TURN` requirements:

- Symmetric to Symmetric: *TURN*.
- Symmetric to Port-Restricted Cone: *TURN*.
- Symmetric to Address-Restricted Cone: *STUN* (but probably not reliable).
- Symmetric to Full Cone: *STUN*.
- Everything else: *STUN*.



.. _nat-diy-holepunch:

Do-It-Yourself hole punching
----------------------------

It is very easy to test the NAT capabilities in a local network. To do this, you need access to two machines:

* One outside the NAT, e.g. by directly connecting it to the internet, with no firewall. We'll call this the **[Server]**.
* One sitting behind a NAT. This is the typical situation for consumer-grade home networks, so this one will be the **[Client]**.

Set some helper variables: the *public* IP address of each host, and their listening ports:

.. code-block:: shell

   SERVER_IP="203.0.113.2"  # Public IP address of the Server
   SERVER_PORT="1111"       # Listening port of the Server

   CLIENT_IP="198.51.100.1" # Public IP address of the NAT that hides the Client
   CLIENT_PORT="2222"       # Listening port of the Client

1. **[Client]** starts listening for data. Leave this running in [Client]:

   .. code-block:: shell

      nc -vnul "$CLIENT_PORT"

2. **[Server]** tries to send data, but the NAT in front of **[Client]** will discard the packets. Run in [Server]:

   .. code-block:: shell

      echo "TEST" | nc -vnu -p "$SERVER_PORT" "$CLIENT_IP" "$CLIENT_PORT"

3. **[Client]** performs a hole punch, forcing its NAT to create a new inbound rule. **[Server]** awaits for the UDP packet, for verification purposes.

   Run in [Server]:

   .. code-block:: shell

      sudo tcpdump -n -i eth0 "src host $CLIENT_IP and udp dst port $SERVER_PORT"

   Run in [Client]:

   .. code-block:: shell

      sudo hping3 --count 1 --udp --baseport "$CLIENT_PORT" --keep --destport "$SERVER_PORT" "$SERVER_IP"

   As an alternative to *hping3*, it's also possible to use plain *netcat*:

   .. code-block:: shell

      echo "TEST" | nc -vnu -p "$CLIENT_PORT" "$SERVER_IP" "$SERVER_PORT"

4. **[Server]** tries to send data again. Run in [Server]:

   .. code-block:: shell

      echo "TEST" | nc -vnu -p "$SERVER_PORT" "$CLIENT_IP" "$CLIENT_PORT"

   After this command, you should see the "TEST" string appearing on the Client.

.. note::

   The difference between a Cone NAT and a Symmetric NAT can be detected during step 3:

   * If the *tcpdump* command on **[Server]** shows a source port equal to *$CLIENT_PORT*, then the NAT is respecting the source port chosen by the application, which means that it is one of the Cone NAT types.

     In this case, the data sent from **[Server]** should arrive correctly at **[Client]** after step 4.

   * However, if *tcpdump* shows that the source port is different from *$CLIENT_PORT*, then the NAT is changing the source port during outbound mapping, which means that it is a Symmetric NAT.

     When this happens, the data sent from **[Server]** won't arrive at **[Client]** after step 4, because *$CLIENT_PORT* is the wrong destination port. If you write the correct port (as discovered in step 3) instead of *$CLIENT_PORT*, then the data should arrive at **[Client]**.



PyNAT
-----

**PyNAT** is a tool that uses STUN servers in order to try and detect what is the type of the NAT, when running from a host behind it. To install and run:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       python3 python3-pip

   sudo -H python3 -m pip install --upgrade pynat

   pynat

You will see an output similar to this:

.. code-block:: shell-session

   $ pynat
   Network type: Restricted-port NAT
   Internal address: 192.168.1.2:54320
   External address: 203.0.113.9:54320
