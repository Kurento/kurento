====================
Memory Fragmentation
====================

.. contents:: Table of Contents



Problem background
==================

Not all memory problems are related to memory leaks, and an application without leaks still could raise out-of-memory errors. One of the possible reasons for this is **Memory Fragmentation**, a problem that is conceptually similar to the well known disk fragmentation that affected some popular file systems such as FAT or NTFS on Windows systems.

There are numerous online resources where a description of the Memory Fragmentation problem can be found; two such resources are `Memory Leak Caused by Fragmentation <https://www.codeproject.com/articles/11151/memory-leak-caused-by-fragmentation>`__ (`archive <https://web.archive.org/web/20131005100426/https://www.codeproject.com/articles/11151/memory-leak-caused-by-fragmentation>`__) and `Preventing Memory Fragmentation <https://www.devx.com/tips/Tip/14060>`__ (`archive <https://web.archive.org/web/20201028033928/https://www.devx.com/tips/Tip/14060>`__).

In the presence of small and big memory allocations in a lineal memory space it may happen that, as memory is allocated, the free spaces between already allocated blocks may not be big enough to allocate a newly requested amount of memory, and this causes the process to request more memory form the Operating System. Later, before that big memory area is released, some other smaller blocks could have been allocated near it. This would cause that the big memory area cannot be given back to the OS. when the application releases it. This memory area would then be marked as "ready to be reclaimed", but the Kernel won't get an immediate handle of it.

Forward some hundreds or thousands of memory allocations later, and this kind of memory fragmentation can end up causing an out-of-memory error, even though technically there are lots of released memory areas, which the Kernel hasn't been able to reclaim.

This issue is more likely to happen in systems that make heavy use of dynamic memory and with different sizes for allocated memory, which is really the case with Kurento Media Server.



Solution
========

The best option we know about is replacing ``malloc``, the standard memory allocator that comes by default with the system, with a specific-purpose allocator, written with this issue in mind in order to avoid or mitigate it as much as possible.

Two of the most known alternative allocators are `jemalloc <http://jemalloc.net/>`__ (`code repository <https://github.com/jemalloc/jemalloc>`__) and Google's `TCMalloc <https://google.github.io/tcmalloc/>`__ (`code repository <https://github.com/google/tcmalloc>`__).

*jemalloc* has been tested with Kurento Media Server, and found to give pretty good results. It is important to note that internal fragmentation cannot be reduced to zero, but this alternative allocator was able to reduce memory fragmentation issues to a minimum.



.. _knowledge-memfrag-jemalloc:

Using jemalloc
==============

First install it on your system. For the versions of Ubuntu that are explicitly supported by Kurento, you can run this command:

.. code-block:: shell

   sudo apt-get update && sudo apt-get install --yes libjemalloc1

*jemalloc* is installed as a standalone library. To actually use it, you need to preload it when launching KMS:

.. code-block:: shell

   LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.1  /usr/bin/kurento-media-server

This will use jemalloc with its default configuration, which should be good enough for normal operation.

If you know how to fine-tune the allocator with better settings than the default ones, you can do so on the command line too. A useful environment variable to learn more about the internals of *jemalloc* is ``MALLOC_CONF``. For example:

.. code-block:: shell

   export MALLOC_CONF=stats_print:true
   LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.1  /usr/bin/kurento-media-server

This will cause KMS to dump memory usage statistics when exiting. Those statistics may be used to tune *jemalloc* and define the configuration to use.

.. note::

   To use *jemalloc* from inside a Docker container, you'll want to make a custom image that is derived from the official one, where the required package is installed and the Docker Entrypoint script has been modified to add the library preloading step.

   There is some additional information about how to start making a customized image in :ref:`faq-docker`.

.. note::

   Since Ubuntu 20.04, the package is named ``libjemalloc2`` and the library file is ``/usr/lib/x86_64-linux-gnu/libjemalloc.so.2``.



Other suggestions
=================

It is a good idea to maintain health checks on servers that are running Kurento Media Server and show memory exhaustion issues.

As it still may present some internal fragmentation level (it is really difficult to get rid of this in a server that makes a heavy use of dynamic memory), we suggest maintaining some health probes on KMS, that at least should take care of memory usage and behave as follows:

1. Maintain a probe on memory usage of the Kurento Media Server process.

2. As soon as that usage grows over a threshold value in a sustained manner (i.e. it does not get back when sessions finish), that server instance should be recycled:

   2.1. It should not accept further sessions, and

   2.2. As soon as the last session is finished, the Kurento Media Server instance should be stopped (and probably restarted).
