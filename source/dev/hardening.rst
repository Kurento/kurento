==================
Security Hardening
==================

*Hardening* is a set of mechanisms that can be activated by turning on several compiler flags, and are commonly used to protect resulting programs against memory corruption attacks. These mechanisms have been standard practice since at least 2011, when the Debian project set on the goal of releasing all their packages with the security hardening build flags enabled [#Debian]_. Ubuntu has also followed the same policy regarding their own build procedures [#Ubuntu]_.

Kurento Media Server had been lagging in this respect, and old releases only implemented the standard `Debian hardening options`_ that are applied by the *dpkg-buildflags* tool by default:

- **Format string checks** (``-Wformat -Werror=format-security``) [#format]_. These options instruct the compiler to make sure that the arguments supplied to string functions such as *printf* and *scanf* have types appropriate to the format string specified, and that the conversions specified in the format string make sense.

- **Fortify Source** (``-D_FORTIFY_SOURCE=2``) [#fortify]_. When this macro is defined at compilation time, several compile-time and run-time protections are enabled around unsafe use of some glibc string and memory functions such as *memcpy* and *strcpy*, with get replaced by their safer counterparts. This feature can prevent some buffer overflow attacks, but it requires optimization level ``-O1`` or higher so it is not enabled in Debug builds (which use ``-O0``).

- **Stack protector** (``-fstack-protector-strong``) [#stack]_. This compiler option provides a randomized stack canary that protects against *stack smashing* attacks that could lead to buffer overflows, and reduces the chances of arbitrary code execution via controlling return address destinations.

- **Read-Only Relocations** (RELRO) (``-Wl,-z,relro``). This linker option marks any regions of the relocation table as "read-only" if they were resolved before execution begins. This reduces the possible areas of memory in a program that can be used by an attacker that performs a successful *GOT-overwrite* memory corruption exploit. This option works best with the linker's *Immediate Binding* mode, which forces *all* regions of the relocation table to be resolved before execution begins. However, immediate binding is disabled by default.

Starting from version **6.7**, KMS also implements these extra hardening measurements:

- **Position Independent Code** (``-fPIC``) / **Position Independent Executable** (``-fPIE -pie``) [#pie]_. Allows taking advantage of the :wikipedia:`Address Space Layout Randomization (ASLR) <en,Address_space_layout_randomization>` protection offered by the Kernel. This protects against :wikipedia:`Return-Oriented Programming (ROP) <en,Return-oriented_programming>` attacks and generally frustrates memory corruption attacks. This option was initially made the default in Ubuntu 16.10 for some selected architectures, and in Ubuntu 17.10 was finally enabled by default across all architectures supported by Ubuntu.

  .. note::

     The *PIC*/*PIE* feature adds a very valuable protection against attacks, but has one important requisite: *all shared objects must be compiled as position-independent code*. If your shared library has stopped linking with KMS, or your plugin stopped loading at run-time, try recompiling your code with the ``-fPIC`` option.

- **Immediate Binding** (``-Wl,-z,now``). This linker option improves upon the *Read-Only Relocations* (RELRO) option, by forcing that all dynamic symbols get resolved at start-up (instead of on-demand). Combined with the RELRO flag, this means that the GOT can be made entirely read-only, which prevents even more types of *GOT-overwrite* memory corruption attacks.



Hardening validation
====================

Debian-based distributions provide the *hardening-check* tool (package *hardening-includes*), which can be used to check if a binary file (either an executable or a shared library) was properly hardened:

.. code-block:: console

   $ hardening-check /usr/sbin/sshd
   /usr/sbin/sshd:
   Position Independent Executable: yes
   Stack protected: yes
   Fortify Source functions: yes
   Read-only relocations: yes
   Immediate binding: yes



Hardening in Kurento
====================

Since version 6.7, Kurento Media Server is built with all the mentioned hardening measurements. All required flags are added in the Debian package generation step, by setting the environment variable *DEB_BUILD_MAINT_OPTIONS* to ``hardening=+all``, as described by `Debian hardening options`_. This variable is injected into the build environment by the CMake module ``kms-cmake-utils/CMake/CommonBuildFlags.cmake``, which is included by all modules of KMS.



PIC/PIE in GCC
==============

This section explains how the Position Independent Code (PIC) and Position Independent Executable (PIE) features are intended to be used (in GCC). Proper use of these is required to achieve correct application of ASLR by the Kernel.

- PIC must be enabled in the compiler for compilation units that will end up linked into a shared library. Note that this includes objects that get packed as a static library before being linked into a shared library.
- PIC must be enabled in the linker for shared libraries.
- PIE *or* PIC (the former being the recommended one) must be enabled in the compiler for compilation units that will end up linked into an executable file. Note that this includes objects that get packed as a static library before being linked into the executable.
- PIE must be enabled in the linker for executable files.

Now follows some examples of applying these rules into an hypothetical project composed of one shared library and one executable file:

- The shared library (*SHARED.so*) is composed of 4 source files:

  - *A.c* and *B.c* are compiled first into a static library: *AB.a*.
    GCC flags: ``-fPIC``.
  - *C.c* and *D.c* are compiled into object files *C.o* and *D.o*.
    GCC flags: ``-fPIC``.
  - *AB.a*, *C.o*, and *D.o* are linked into a shared library: *SHARED.so*.
    GCC flags: ``-shared -fPIC``.

- The executable file (*PROGRAM*) is composed of 4 source files:

  - *E.c* and *F.c* are compiled first into a static library: *EF.a*.
    GCC flags: ``-fPIE`` (*).
  - *G.c* and *H.c* are compiled into object files *G.o* and *H.o*.
    GCC flags: ``-fPIE`` (*).
  - *EF.a*, *G.o*, and *H.o* are linked into an executable file: *PROGRAM*.
    GCC flags: ``-pie -fPIE`` (... *-lSHARED*).

(*): In these cases, it is also possible to compile these files with ``-fPIC``, although ``-fPIE`` is recommended. It is also possible to mix both; for example *E.c* and *F.c* can be compiled with ``-fPIC``, while *G.c* and *H.c* are compiled with ``-fPIE`` (empirically tested, it works fine).

.. seealso::

   `Options for Code Generation Conventions <https://gcc.gnu.org/onlinedocs/gcc-7.2.0/gcc/Code-Gen-Options.html>`__
       See ``-fPIC``, ``-fPIE``.

   `Options for Linking <https://gcc.gnu.org/onlinedocs/gcc-7.2.0/gcc/Link-Options.html>`__
       See ``-shared``, ``-pie``.

   `dpkg-buildflags <http://man7.org/linux/man-pages/man1/dpkg-buildflags.1.html>`__
       See ``FEATURE AREAS > hardening > pie``.



PIC/PIE in CMake
================

CMake has *partial* native support to enable PIC/PIE in a project, via the *POSITION_INDEPENDENT_CODE* and *CMAKE_POSITION_INDEPENDENT_CODE* variables. We consider it "partial" because these variables add the corresponding flags for the compilation steps, but the flag ``-pie`` is not automatically added to the linker step.

We raised awareness about this issue in their bug tracker: `POSITION_INDEPENDENT_CODE does not add -pie <https://gitlab.kitware.com/cmake/cmake/issues/14983>`__.

The effect of setting *POSITION_INDEPENDENT_CODE* to *ON* for a CMake target (or setting *CMAKE_POSITION_INDEPENDENT_CODE* for the whole project), is the following:

- If the target is a library, the flag ``-fPIC`` is added by CMake to the compilation and linker steps.
- If the target is an executable, the flag ``-fPIE`` is added by CMake to the compilation and linker steps.

However, CMake is lacking that it *does not* add the flag ``-pie`` to the linker step of executable targets, so final executable programs are *not* properly hardened for ASLR protection by the Kernel.

Kurento Media Server works around this limitation of CMake by doing this in the CMake configuration:

.. code-block:: cmake

   # Use "-fPIC" / "-fPIE" for all targets by default, including static libs
   set(CMAKE_POSITION_INDEPENDENT_CODE ON)

   # CMake doesn't add "-pie" by default for executables (CMake issue #14983)
   set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pie")

It would be nice if CMake took over the whole process of generating valid PIC/PIE libraries and executables, by ensuring that all needed flags are added in the correct places. It's actually very close to that, by only missing the `-pie` flag while linking executable programs.



.. rubric:: Footnotes

.. [#Debian] https://wiki.debian.org/Hardening#Notes_on_Memory_Corruption_Mitigation_Methods
.. [#Ubuntu] https://wiki.ubuntu.com/Security/Features#Userspace_Hardening

.. [#format] https://gcc.gnu.org/onlinedocs/gcc/Warning-Options.html
.. [#fortify] http://man7.org/linux/man-pages/man7/feature_test_macros.7.html
.. [#stack] https://gcc.gnu.org/onlinedocs/gcc/Instrumentation-Options.html
.. [#pie] https://gcc.gnu.org/onlinedocs/gcc/Code-Gen-Options.html

.. _Debian hardening options: https://wiki.debian.org/HardeningWalkthrough#Selecting_security_hardening_options
