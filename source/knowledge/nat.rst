===========================
NAT Types and NAT Traversal
===========================

.. contents:: Table of Contents

Sources:

- :wikipedia:`Network address translation`
- `Symmetric NAT and It's Problems <https://www.think-like-a-computer.com/2011/09/19/symmetric-nat/>`__ (`archive <https://archive.is/jt4c2>`__)
- `Peer-to-Peer Communication Across Network Address Translators <http://www.brynosaurus.com/pub/net/p2pnat/>`__ (`archive <https://archive.is/u7His>`__)
- `The hole trick - How Skype & Co. get round firewalls <http://www.h-online.com/security/features/How-Skype-Co-get-round-firewalls-747197.html>`__ (`archive <https://archive.is/NTvAl>`__)
- `What type of NAT combinations requires a TURN server? <https://stackoverflow.com/questions/31424904/what-type-of-nat-combinations-requires-a-turn-server>`__
- `Under what scenarios Server- and Peer-Reflexive candidates differ? <https://stackoverflow.com/questions/19905239/under-what-scenarios-does-server-reflexive-and-peer-reflexive-addresses-candidat>`__



Basic Concepts
==============

IP connection
-------------

An IP connection is uniquely identified by a connection "quadruplet" consisting of two tuples:

- (Source IP address, source port number).
- (Destination IP address, destination port number).

Visualization:

.. code-block:: text

   (SRC_ADDR, SRC_PORT) -> (DST_ADDR, DST_PORT)

.. note::

   - ``->`` denotes the direction of the connection.
   - ``(ADDR, PORT)`` denotes an (IP address, IP port) tuple.
   - ``(SRC_ADDR, SRC_PORT)`` is the IP tuple of the machine that makes the connection.
   - ``(DST_ADDR, DST_PORT)`` is the IP tuple of the machine that receives the connection.



Inbound NAT connection
----------------------

A NAT maintains **inbound** rules which are used to translate IP tuples between the NAT's *external* and *internal* sides. Usually, the *external* side of the NAT is facing the public internet (*WAN*), while the *internal* side of the NAT is facing the local network (*LAN*). These inbound rules have the form of a *hash table* (or *map*); for each given key, there is a resulting value:

- The key of the table is an IP quadruplet, formed by a NAT's external IP tuple and a remote machine's IP tuple.
- The value of the table is a NAT's internal IP tuple.

In other words, a NAT creates an equivalence between external combinations of IP addresses and ports, and internal IP addresses and ports.

Typically, these NAT rules are created automatically during an outbound connection from within the LAN to some remote machine: it's at that moment when the NAT creates a new entry into its table (this is **step 1** in the following visualizations). Later, this entry in the NAT table is used to decide which local machine needs to receive the response that the remote machine might send (this is **step 2** in the visualizations). Rules created in this way are called "*dynamic rules*"; on the other hand, "*static rules*" can be explicitly created by an administrator, in order to set up a fixed NAT table for a given local machine.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote machine}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT) <- (REM_ADDR, REM_PORT) ]

.. note::

   - ``->`` and ``<-`` denote the direction of the connection in each step.
   - ``=>`` denotes the creation of a new rule (key) in the NAT table.
   - ``[ (ADDR, PORT), (ADDR, PORT) ]`` denotes the **key** (IP quadruplet) used to access the NAT table.
   - ``<=`` denotes the resolution of the NAT mapping.
   - ``(INT_ADDR, INT_PORT)`` is the **source** IP tuple on the *internal side* of the NAT for a local machine making a connection during step 1, and it is the **destination** IP tuple for the same machine receiving the connection during step 2.
   - ``(EXT_ADDR, EXT_PORT)`` is the **source** IP tuple on the *external side* of the NAT from where a connection is originated during step 1, and it is the **destination** IP tuple for the connection being received during step 2.
   - ``(REM_ADDR, REM_PORT)`` is the **destination** IP tuple of some remote machine receiving a connection during step 1, and it is the **source** IP tuple of a remote machine that makes a connection during step 2.



Types of NAT
============

There are two categories of NAT behavior, namely **Cone** and **Symmetric** NAT. The crucial difference between them is that the former will use the same port numbers for internal and external IP tuples, while the later will always use different numbers for each side of the NAT. This will be explained later in more detail.

Besides, there are 3 types of Cone NATs, with varying degrees of restrictions regarding the allowed sources of incoming connections. To connect with a local machine which is behind a Cone NAT, it's first required that the local machine performs an outbound connection to a remote one. This way, a dynamic rule will be created for the destination IP tuple, allowing the remote machine to connect back. The only exception is the Full Cone NAT, where a static rule can be created beforehand by an administrator, thanks to the fact that this kind of NAT ignores what is the source IP tuple of the remote machine that is connecting.



Full Cone NAT
-------------

This type of NAT allows inbound connections from *any source IP address* and *any source port*, as long as the destination tuple exists in a previously created rule.

Typically, these rules are statically created beforehand by an administrator. These are the kind of rules that are used to configure *Port Forwarding* (aka. "*opening the ports*") in most consumer-grade routers. Of course, as it is the case for all NAT types, it is also possible to create dynamic rules by first performing an outbound connection.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote machine}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (   *    ,    *    ) ]

.. note::

   - ``*`` means that any value could be used: a remote machine can connect from *any* IP address and port.
   - The **source** IP address (``REM_ADDR``) in step 2 can be different from the **destination** IP address that was used in step 1.
   - The **source** IP port (``REM_PORT``) in step 2 can be different from the **destination** IP port that was used in step 1.
   - The *same* port (``INT_PORT``) is used in the internal and the external sides of the NAT. This is the most common case for all Cone NATs, only being different for Symmetric NATs.



(Address-)Restricted Cone NAT
-----------------------------

This type of NAT allows inbound connections from a *specific source IP address* but allowing for *any source port*. Typically, an inbound rule of this type was previously created dynamically, when the local machine initiated an outbound connection to a remote one.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote machine}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (REM_ADDR,    *    ) ]

.. note::

   - The **source** IP address (``REM_ADDR``) in step 2 must be the same as the **destination** IP address that was used in step 1.
   - The **source** IP port (``REM_PORT``) in step 2 can be different from the **destination** IP port that was used in step 1.
   - The *same* port (``INT_PORT``) is used in the internal and the external sides of the NAT.



Port-Restricted Cone NAT
------------------------

This is the most restrictive type of Cone NAT: it only allows inbound connections from a *specific source IP address* and a *specific source port*. Again, an inbound rule of this type was previously created dynamically, when the local machine initiated an outbound connection to a remote one.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote machine}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, INT_PORT) -> (REM_ADDR, REM_PORT) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, INT_PORT) <- (REM_ADDR, REM_PORT) ]

.. note::

   - The **source** IP address (``REM_ADDR``) in step 2 must be the same as the **destination** IP address that was used in step 1.
   - The **source** IP port (``REM_PORT``) in step 2 must be the same as the **destination** IP port that was used in step 1.
   - The *same* port (``INT_PORT``) is used in the internal and the external sides of the NAT.



.. _nat-symmetric:

Symmetric NAT
-------------

This type of NAT behaves in the same way of a Port-Restricted Cone NAT, with an important difference: for each outbound connection to a different remote IP tuple (i.e. to a different remote machine), the NAT assigns a **new random source port** on the external side. This means that two consecutive connections from the same local port to two different remote machines will have two different external source ports, even if the internal source IP tuple is the same for both of them.

This is also the only case where the ICE connectivity protocol will find `Peer Reflexive candidates <https://tools.ietf.org/html/rfc5245#section-7.1.3.2.1>`__ which differ from the Server Reflexive ones, due to the differing ports between the connection to the STUN server and the direct connection between peers.

Visualization:

.. code-block:: text

       {NAT internal side}  |    {NAT external side}  |  {Remote machine}
                            |                         |
   1. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT1) -> (REM_ADDR, REM_PORT1) ]
   2. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT1) <- (REM_ADDR, REM_PORT1) ]
   ...
   3. (INT_ADDR, INT_PORT) => [ (EXT_ADDR, EXT_PORT2) -> (REM_ADDR, REM_PORT2) ]
   4. (INT_ADDR, INT_PORT) <= [ (EXT_ADDR, EXT_PORT2) <- (REM_ADDR, REM_PORT2) ]

.. note::

   - When the outbound connection is done in step 1, ``EXT_PORT1`` gets defined as a new random port number, assigned for the new remote IP tuple ``(REM_ADDR, REM_PORT1)``.
   - Later, another outbound connection is done in step 3, from the same local address and port to the same remote machine but at a different port. ``EXT_PORT2`` is a new random port number, assigned for the new remote IP tuple ``(REM_ADDR, REM_PORT2)``.



Types of NAT in the Real World
==============================

Quoting from :wikipedia:`Wikipedia <en,Network_address_translation#Methods_of_translation>`:

    This terminology has been the source of much confusion, as it has proven inadequate at describing real-life NAT behavior. Many NAT implementations combine these types, and it is, therefore, better to refer to specific individual NAT behaviors instead of using the Cone/Symmetric terminology. :rfc:`4787` attempts to alleviate this issue by introducing standardized terminology for observed behaviors. For the first bullet in each row of the above table, the RFC would characterize Full-Cone, Restricted-Cone, and Port-Restricted Cone NATs as having an *Endpoint-Independent Mapping*, whereas it would characterize a Symmetric NAT as having an *Address-* and *Port-Dependent Mapping*. For the second bullet in each row of the above table, :rfc:`4787` would also label Full-Cone NAT as having an *Endpoint-Independent Filtering*, Restricted-Cone NAT as having an *Address-Dependent Filtering*, Port-Restricted Cone NAT as having an *Address and Port-Dependent Filtering*, and Symmetric NAT as having either an *Address-Dependent Filtering* or *Address and Port-Dependent Filtering*. There are other classifications of NAT behavior mentioned, such as whether they preserve ports, when and how mappings are refreshed, whether external mappings can be used by internal hosts (i.e., its :wikipedia:`Hairpinning` behavior), and the level of determinism NATs exhibit when applying all these rules.[2]

    Especially, most NATs combine *symmetric NAT* for outgoing connections with *static port mapping*, where incoming packets addressed to the external address and port are redirected to a specific internal address and port. Some products can redirect packets to several internal hosts, e.g., to divide the load between a few servers. However, this introduces problems with more sophisticated communications that have many interconnected packets, and thus is rarely used.



NAT Traversal
=============

The NAT mechanism is implemented in a vast majority of home and corporate routers, and it completely prevents the possibility of running any kind of server software in a local machine that sits behind these kinds of devices. NAT Traversal, also known as *Hole Punching*, is the procedure of opening an inbound port in the NAT tables of these routers.

To connect with a local machine which is behind any type of NAT, it's first required that the local machine performs an outbound connection to the remote one. This way, a dynamic rule will be created for the destination IP tuple, allowing the remote machine to connect back.

In order to tell one machine when it has to perform an outbound connection to another one, and the destination IP tuple it must use, the typical solution is to use a helper service such as STUN. This is usually managed by a third machine, a server sitting on a public internet address. It retrieves the external IP and port of each peer, and gives that information to the other peers that want to communicate.

STUN/TURN requirement:

- Symmetric to Symmetric: *TURN*.
- Symmetric to Port-Restricted Cone: *TURN*.
- Symmetric to Address-Restricted Cone: *STUN* (but probably not reliable).
- Symmetric to Full Cone: *STUN*.
- Everything else: *STUN*.



Do-It-Yourself hole punching
----------------------------

It is very easy to test the NAT capabilities in a local network. To do this, you need access to two machines:

A. One siting behind a NAT. We'll call this the host **A**.
B. One directly connected to the internet, with no firewall. This is host **B**.

Set some helper variables: the *public* IP address of each host, and their listening ports:

.. code-block:: bash

   A_IP="11.11.11.11"  # Public IP address of the NAT which hides the host A
   A_PORT="1111"       # Listening port on the host A
   B_IP="22.22.22.22"  # Public IP address of the host B
   B_PORT="2222"       # Listening port of the host B

1. **A** starts listening for data. Leave this running in A:

   .. code-block:: bash

      nc -4nul "$A_PORT"

2. **B** tries to send data, but the NAT in front of **A** will discard the packets. Run in B:

   .. code-block:: bash

      echo "TEST" | nc -4nu -q 1 -p "$B_PORT" "$A_IP" "$A_PORT"

3. **A** performs a hole punch, forcing its NAT to create a new inbound rule. **B** awaits for the UDP packet, for verification purposes.

   Run in B:

   .. code-block:: bash

      sudo tcpdump -n -i eth0 "src host $A_IP and udp dst port $B_PORT"

   Run in A:

   .. code-block:: bash

      sudo hping3 --count 1 --udp --baseport "$A_PORT" --keep --destport "$B_PORT" "$B_IP"

4. **B** tries to send data again. Run in B:

   .. code-block:: bash

      echo "TEST" | nc -4nu -q 1 -p "$B_PORT" "$A_IP" "$A_PORT"

.. note::

   - The difference between a Cone NAT and a Symmetric NAT can be detected during step 3. If the ``tcpdump`` command on **B** shows a source port equal to ``$A_PORT``, then the NAT is respecting the source port chosen by the application, which means that it is one of the Cone NAT types. However, if ``tcpdump`` shows that the source port is different from ``$A_PORT``, then the NAT is changing the source port during outbound mapping, which means that it is a Symmetric NAT.

   - In the case of a Cone NAT, the data sent from **B** should arrive correctly at **A** after step 4.

   - In the case of a Symmetric NAT, the data sent from **B** won't arrive at **A** after step 4, because ``$A_PORT`` is the wrong destination port. If you write the correct port (as discovered in step 3) instead of ``$A_PORT``, then the data should arrive to **A**.



PySTUN
------

**PySTUN** is a tool that uses STUN servers in order to try and detect what is the type of the NAT, when ran from a machine behind it.

Currently it has been best updated in one of its forks, so we suggest using that instead of the version from the original creator. To install and run:

.. code-block:: bash

   git clone https://github.com/konradkonrad/pystun.git pystun-konrad
   cd pystun-konrad/
   git checkout research
   mv README.md README.rst
   sudo python setup.py install
   pystun
