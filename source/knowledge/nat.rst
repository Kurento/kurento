===========================
NAT Types and NAT Traversal
===========================

.. contents:: Table of Contents

Sources:

- `Symmetric NAT and It’s Problems <http://www.think-like-a-computer.com/2011/09/19/symmetric-nat/>`_
- `Peer-to-Peer Communication Across Network Address Translators <http://www.brynosaurus.com/pub/net/p2pnat/>`_
- `The hole trick - How Skype & Co. get round firewalls <http://www.h-online.com/security/features/How-Skype-Co-get-round-firewalls-747197.html>`_



Basic Concepts
==============

IP connection
-------------

An IP connection is uniquely identified by a connection "quadruplet" consisting of two tuples:

- (Source IP address, source port number).
- (Destination IP address, destination port number).

Concept:

.. code-block:: text

   (SRC_IP, SRC_PORT) -> (DST_IP, DST_PORT)

.. note::

   - ``(SRC_IP, SRC_PORT)`` is the IP tuple of a local machine making a connection.
   - ``->`` denotes the direction of the communication.
   - ``(DST_IP, DST_PORT)`` is the IP tuple of a remote machine receiving the connection.



Inbound NAT connection
----------------------

A NAT establishes **inbound** rules which are used to convert an IP tuple existing in the external network (*WAN*) to an IP tuple existing in the internal network (*LAN*). Usually, the external network is the public internet while the internal network is the local network. These inbound rules have the form of a *hash table* (or *map*): for each given key, there is a resulting value:

- The key of the table is an IP quadruplet, formed by the WAN IP tuple and the destination IP tuple.
- The value of the table is the LAN IP tuple.

In other words, a NAT creates an equivalence between external combinations of IP addresses and ports, and internal IP addresses and ports.

Typically, these NAT rules were created automatically during an earlier outbound connection from the LAN to some external machine: it's at that moment when the NAT inserts a new entry into its table. Later, this entry in the NAT table is used to know which local machine needs to receive the response that the external machine might send. Rules created in this way are called "dynamic rules"; on the other hand, "static rules" can be created by the administrator in order to set a fixed table for a given local machine.

Also, it is worth noting that the port number used in the internal side of the network will also be kept by the NAT on the external side, and it won't change for each new connection that the local machine does to any external server. This is the crucial difference between Cone NAT and Symmetric NAT, as explained later.

Concept:

.. code-block:: text

   (LAN_IP, LAN_PORT) <= [(WAN_IP, LAN_PORT) <- (REM_IP, REM_PORT)]

.. note::

   - ``(REM_IP, REM_PORT)`` is the **source** IP tuple of a remote machine making a connection.
   - ``(WAN_IP, LAN_PORT)`` is the **destination** IP tuple on the *external side* of the NAT receiving the connection.
   - ``<=`` denotes the resolution of the NAT mapping.
   - ``(LAN_IP, LAN_PORT)`` is the **destination** IP tuple on the *internal side* of the NAT for a local machine receiving the connection.
   - Note how the *same* port (``LAN_PORT``) is used in the internal and the external sides of the NAT. This is the most common case, only differing for Symmetric NAT.



Types of NAT
============

Full Cone NAT
-------------

This type of NAT allows inbound connections from *any source IP address* and *any source port*, as long as the destination tuple exists in any previously created rule. Typically, these rules are statically created beforehand by an administrator. These are the kind of rules that are used to configure *Port Forwarding* (aka. "*opening the ports*") in most consumer routers.

Concept:

.. code-block:: text

   (LAN_IP, LAN_PORT) <= [(WAN_IP, LAN_PORT) <- (*, *)]

.. note::

   - ``*`` means that any value could be used here.



(Address-)Restricted Cone NAT
-----------------------------

This type of NAT allows inbound connections from *any source port* of a *specific source IP address*. Typically, an inbound rule of this type was previously created dynamically, when the local machine initiated a connection to a remote one.

To connect with a local machine which is behind an Address-Restricted Cone NAT, it is first required that the local machine performs an outbound connection to the remote one. This way, a dynamic rule will be created for the destination IP tuple, allowing the remote machine to connect back.

Concept:

.. code-block:: text

   1. (LAN_IP, LAN_PORT) => [(WAN_IP, LAN_PORT) -> (REM_IP, REM_PORT)]
   2. (LAN_IP, LAN_PORT) <= [(WAN_IP, LAN_PORT) <- (REM_IP, *)]

.. note::

   - ``=>`` denotes the creation of a new rule in the NAT table.
   - The **destination** IP address ``REM_IP`` in step 1 must be the same as the **source** IP address ``REM_IP`` in step 2.



Port-Restricted Cone NAT
------------------------

This is the most restrictive type of NAT: it only allows inbound connections from a *specific source port* of a *specific source IP address*. Again, an inbound rule of this type was previously created dynamically, when the local machine initiated an outbound connection to a remote one.

To connect with a local machine which is behind a Port-Restricted Cone NAT, it is first required that the local machine performs an outbound connection to the remote one. This way, a dynamic rule will be created for the destination IP tuple, allowing the remote machine to connect back.

Concept:

.. code-block:: text

   1. (LAN_IP, LAN_PORT) => [(WAN_IP, LAN_PORT) -> (REM_IP, REM_PORT)]
   2. (LAN_IP, LAN_PORT) <= [(WAN_IP, LAN_PORT) <- (REM_IP, REM_PORT)]

.. note::

   - The **destination** IP address ``REM_IP`` in step 1 must be the same as the **source** IP address ``REM_IP`` in step 2.
   - The **destination** port ``REM_PORT`` in step 1 must be the same as the **source** port ``REM_PORT`` in step 2.



Symmetric NAT
-------------

This type of NAT behaves in the same way of a Port-Restricted Cone NAT, with a crucial difference: for each outbound connection to a different remote machine, the NAT assigns a **new random source port** on the external side. This means that two consecutive connections to two different machines will have two different external source ports, even if the internal source IP tuple is the same for both of them.

This is also the only case where the ICE connectivity protocol will find Peer Reflexive candidates which differ from the Server Reflexive ones, due to the differing ports between the connection to the STUN server and the direct connection between peers.

Concept:

.. code-block:: text

   1. (LAN_IP, LAN_PORT) => [(WAN_IP, WAN_PORT) -> (REM_IP, REM_PORT)]
   2. (LAN_IP, LAN_PORT) <= [(WAN_IP, WAN_PORT) <- (REM_IP, REM_PORT)]

.. note::

   - When the outbound connection is done in step 1, ``WAN_PORT`` gets defined as a new random port number, assigned for each new remote IP tuple ``(REM_IP, REM_PORT)``.



NAT Traversal
=============

The NAT mechanism is implemented in a vast majority of home and corporate routers, and it completely prevents the possibility of running any kind of server software in a local machine which sits behind these kinds of devices. NAT make impossible for a remote client to be the active peer and send any kind of request to the server. NAT Traversal, also known as *Hole Punching*, is the procedure of opening an inbound port in the NAT tables of these routers.

To connect with a local machine which is behind an Address-Restricted Cone NAT, a Port-Restricted Cone NAT or a Symmetric NAT, it is first required that the local machine performs an outbound connection to the remote one. This way, a dynamic rule will be created for the destination IP tuple, allowing the remote machine to connect back.

In order to tell one machine when it has to perform an outbound connection to another one, and the destination IP tuple it must use, the typical solution is to use a signaling service such as STUN. This is usually managed by a third machine, a server sitting on a public internet address. It retrieves the external IP and port of each peer, and gives that information to the other peers that want to communicate.

To connect with a machine which is behind a Full Cone NAT, however, any direct connection to the external IP tuple will work.

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
