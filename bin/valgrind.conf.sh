#!/bin/bash
# (shebang not really needed, but required by ShellCheck)

# Default settings for Valgrind commands.
#
# This config file acts as a central place where all Valgrind settings can
# be defined. This allows to have a common set of general settings.
#
# The environment variable VALGRIND_ARGS gets created as an array of strings,
# containing all command-line options that should be passed to the Valgrind
# command.
#
# To use this file, source it and expand the VALGRIND_ARGS array:
#
#     source valgrind.conf.sh
#     valgrind --tool=memcheck "${VALGRIND_ARGS[@]}" ./my-program



BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path

# shellcheck disable=SC2034
VALGRIND_ARGS=(
    # Basic Options
    # =============
    # http://valgrind.org/docs/manual/manual-core.html#manual-core.basicopts

    # "--quiet"
    "--verbose"

    # Trace into sub-processes initiated via the `execve` system call.
    # =<yes|no> [default: no]
    "--trace-children=yes"

    # Print elapsed wallclock timestamps since startup.
    # =<yes|no> [default: no]
    "--time-stamp=yes"

    # Write all messages to the specified file.
    # Format specifiers:
    # - '%p': the current process ID. Useful for '--trace-children=yes'.
    # - '%n': a file sequence number, unique for the process.
    # - '%q{FOO}': replaced with contents of the environment variable 'FOO'.
    # =<filename>
    # "--log-file='valgrind-%n-%p.log'"



    # Error-related Options
    # =====================
    # http://valgrind.org/docs/manual/manual-core.html#manual-core.erropts

    # Maximum number of entries shown in stack traces.
    # =<number> [default: 12]
    "--num-callers=30"

    # Stop reporting errors after 10,000,000 in total, or 1,000 different ones
    # =<yes|no> [default: yes]
    # "--error-limit=no"

    # Exit code to return if any errors were found by Valgrind.
    # If 0, returns the code of the process being simulated.
    # =<number> [default: 0]
    "--error-exitcode=42"

    # Add lines with begin/end markers before/after each error.
    # =<begin>,<end> [default: none]
    # "--error-markers=BEGIN_ERROR,END_ERROR"

    # Add an extra file from which to read error suppressions.
    # =<filename> [default: $PREFIX/lib/valgrind/default.supp]
    "--suppressions='${BASEPATH}/val grind/GNOME.supp'"
    "--suppressions='${BASEPATH}/val grind/debian.supp'"
    "--suppressions='${BASEPATH}/val grind/glib.supp'"
    "--suppressions='${BASEPATH}/valgrind/gst.supp'"
    "--suppressions='${BASEPATH}/valgrind/walbottle.supp'"

    # Generate a suppression for every reported error.
    # Useful to generate new suppression files.
    # =<yes|no|all> [default: no]
    # "--gen-suppressions=all"

    # Maximum number of threads handled by Valgrind.
    # =<number> [default: 500]
    "--max-threads=9999"



    # Uncommon Options
    # ================
    # http://valgrind.org/docs/manual/manual-core.html#manual-core.rareopts

    # Detect self-modifying code.
    # Needed for JIT languages, like Qt's QML interpreter.
    # =<none|stack|all|all-non-file> [default: all-non-file for x86/amd64]
    # "--smc-check=all"

    # Read debug info about inlined function calls.
    # Results in more detailed stacktraces.
    # SPEED WARNING: Slows Valgrind startup and makes it use more memory.
    # =<yes|no> [default: yes for Valgrind >= 3.10.0 on Linux for Memcheck/Helgrind/DRD]
    "--read-inline-info=yes"

    # Read debug info about stack and global variables.
    # Result in more precise error messages, for Memcheck, Helgrind, and DRD.
    #
    # SPEED WARNING: Slows Valgrind startup significantly and makes it use
    # significantly more memory.
    # =<yes|no> [default: no]
    "--read-var-info=yes"

    # Control the scheduler used to serialise thread execution.
    # Improves reproducibility of thread scheduling for multithreaded applications,
    # which is particularly helpful when using Helgrind or DRD.
    # SPEED WARNING: If CPU scaling is enabled, this could decrease significantly the
    # performance of a multithreaded app. Prevent that by disabling the system's
    # CPU scaling mechanism. E.g. with `cpufreq-set`:
    #     for i in $(seq 0 $(($(nproc)-1))); do
    #         sudo cpufreq-set -c $i -g performance
    #     done
    # =<no|yes|try> [default: no]
    "--fair-sched=try"



    # Memcheck Options
    # ================
    # http://valgrind.org/docs/manual/mc-manual.html#mc-manual.options

    # Search for memory leaks when the client program finishes.
    # - 'no': Disable reporting of memory leaks.
    # - 'summary': Just says how many leaks occurred.
    # - 'full', 'yes': Each individual leak will be shown in detail.
    # =<no|summary|yes|full> [default: summary]
    "--memcheck:leak-check=full"

    # Threshold to merge different backtraces into the same leak report.
    # - 'low': Only the first two entries need match.
    # - 'med': Four entries have to match.
    # - 'high': All entries need to match.
    # =<low|med|high> [default: high]
    "--memcheck:leak-resolution=med"

    # Leak kinds to show in a 'full' leak search.
    # =<definite,indirect,possible,reachable> [default: definite,possible]
    "--memcheck:show-leak-kinds=definite,indirect,possible"

    # Output a 'Callgrind Format' execution tree file with leak results.
    # =<no|yes> [default: no]
    # "--memcheck:xtree-leak=yes"
    # "--memcheck:xtree-leak-file='valgrind-memcheck-xtleak-%p.kcg'"

    # Report uses of 'undefined value' errors.
    # =<yes|no> [default: yes]
    # "--memcheck:undef-value-errors=no"

    # Track the origin of uninitialised values.
    # It can drastically reduce the effort required to identify the root cause
    # of uninitialised value errors, and so is often a productivity win.
    # SPEED WARNING: Reduces Memcheck's speed by 50% and increases memory use
    # by a minimum of 100MB, and possibly more.
    # =<yes|no> [default: no]
    # "--memcheck:track-origins=yes"

    # Control how loads from partially invalid addresses are treated.
    # Code with invalid addresses is in violation of the ISO C/C++ standards,
    # and should be considered broken.
    # =<yes|no> [default: yes]
    "--memcheck:partial-loads-ok=no"

    # Size of the freed memory buffer that Memcheck uses to detect invalid
    # accesses to blocks for some time after they have been freed.
    # Raising this value may detect invalid uses of freed blocks which would
    # otherwise go undetected.
    # =<number> [default: 20000000], in Bytes
    "--memcheck:freelist-vol=256000000"



    # Massif Options
    # ==============
    # http://valgrind.org/docs/manual/ms-manual.html#ms-manual.options

    # Enable heap profiling.
    # =<yes|no> [default: yes]
    # "--massif:heap=no"

    # Enable stack profiling.
    # Note main stack size is counted as zero at start-up.
    # SPEED WARNING: Slows Massif down greatly.
    # =<yes|no> [default: no]
    # "--massif:stacks=yes"

    # Profile memory at the page level rather than at the malloc() level.
    # This measures memory allocations at the lower-level mmap/mremap/brk
    # functions, instead of following the higher-level malloc/realloc/new.
    # This will report numbers closer to tools like `top`.
    # =<yes|no> [default: no]
    # "--massif:pages-as-heap=yes"

    # Add functions to be treated as wrappers to 'malloc' or 'new'.
    # =<name>
    "--massif:alloc-fn=g_malloc"
    "--massif:alloc-fn=g_malloc0"
    "--massif:alloc-fn=g_mem_chunk_alloc"
    "--massif:alloc-fn=g_realloc"
    "--massif:alloc-fn=g_slice_alloc"
    "--massif:alloc-fn=g_try_malloc"

    # The time base used for the profiling results.
    # - 'i': Instructions executed.
    # - 'ms' (i.e. milliseconds): real (wallclock) time.
    # - 'B': Bytes allocated/deallocated on the heap and/or stack.
    # =<i|ms|B> [default: i]
    "--massif:time-unit=B"

    # Frequency of detailed snapshots. 1 = every snapshot is detailed.
    # =<n> [default: 10]
    # "--massif:detailed-freq=3"

    # Write the profiling data to the specified file.
    # =<file> [default: massif.out.%p]
    # "--massif-out-file='valgrind-massif-%n-%p.out'"



    # Callgrind Options
    # =================
    # http://valgrind.org/docs/manual/cl-manual.html#cl-manual.options

    # Start simulation and profiling from the beginning of the program.
    # Use the command `callgrind_control -i on` to manually start profiling.
    # =<yes|no> [default: yes]
    "--callgrind:instr-atstart=no"

    # Start event collection from the beginning of the profile run.
    # =<yes|no> [default: yes]
    # "--callgrind:collect-atstart=no"

    # Separate generation of profile data for every thread.
    # =<no|yes> [default: no]
    # "--callgrind:separate-threads=yes"

    # Do full cache simulation instead of only counting instruction read accesses.
    # =<yes|no> [default: no]
    # "--callgrind:cache-sim=yes"
)
