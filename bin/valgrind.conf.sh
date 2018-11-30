# This file should be sourced by the `valgrind-*` scripts.
# The environment variable VALGRIND_OPTIONS contains default common options
# that will get passed to Valgrind at launch time.

VALGRIND_OPTIONS=(
    # ==== Core options ====
    # http://valgrind.org/docs/manual/manual-core.html

    #--quiet
    --verbose

    # Trace into sub-processes initiated via the `execve` system call.
    # =<yes|no> [default: no]
    --trace-children=yes

    # Print elapsed wallclock timestamps since startup.
    # =<yes|no> [default: no]
    --time-stamp=yes

    # Write all messages to the specified file.
    # Format specifiers:
    # - '%p': the current process ID. Useful for '--trace-children=yes'.
    # - '%n': a file sequence number, unique for the process.
    # - '%q{FOO}': replaced with contents of the environment variable 'FOO'.
    # =<filename>
    # --log-file=valgrind-%n-%p.log


    # ==== Error-related options ====

    # Maximum number of entries shown in stack traces.
    # =<number> [default: 12]
    --num-callers=30

    # Stop reporting errors after 10,000,000 in total, or 1,000 different ones
    # =<yes|no> [default: yes]
    #--error-limit=no

    # Exit code to return if any errors were found by Valgrind.
    # If 0, returns the code of the process being simulated.
    # =<number> [default: 0]
    #--error-exitcode=1

    # Add lines with begin/end markers before/after each error.
    # =<begin>,<end> [default: none]
    #--error-markers=BEGIN_ERROR,END_ERROR

    # Add an extra file from which to read error suppressions.
    # =<filename> [default: $PREFIX/lib/valgrind/default.supp]
    --suppressions="$PWD/3rdparty/valgrind/GNOME.supp"
    --suppressions="$PWD/3rdparty/valgrind/debian.supp"
    --suppressions="$PWD/3rdparty/valgrind/glib.supp"
    --suppressions="$PWD/3rdparty/valgrind/gst.supp"
    --suppressions="$PWD/3rdparty/valgrind/walbottle.supp"

    # Generate a suppression for every reported error.
    # Useful to generate new suppression files.
    # =<yes|no|all> [default: no]
    #--gen-suppressions=all


    # ==== Uncommon options ====
    # Detect self-modifying code.
    # Needed for JIT languages, like Qt's QML interpreter.
    # =<none|stack|all|all-non-file> [default: all-non-file for x86/amd64]
    #--smc-check=all

    # Read debug info about inlined function calls.
    # Results in more detailed stacktraces.
    # WARNING: Slows Valgrind startup and makes it use more memory.
    # =<yes|no> [default: yes for Valgrind >= 3.10.0 on Linux for Memcheck/Helgrind/DRD]
    --read-inline-info=yes

    # Read debug info about stack and global variables.
    # Result in more precise error messages, for Memcheck, Helgrind, and DRD.
    #
    # WARNING: Slows Valgrind startup significantly and makes it use
    # significantly more memory.
    # =<yes|no> [default: no]
    --read-var-info=yes

    # Control the scheduler used to serialise thread execution.
    # Improves reproducibility of thread scheduling for multithreaded applications,
    # which is particularly helpful when using Helgrind or DRD.
    # WARNING: If CPU scaling is enabled, this could decrease significantly the
    # performance of a multithreaded app. Prevent that by disabling the system's
    # CPU scaling mechanism. E.g. with `cpufreq-set`:
    #     for i in $(seq 0 $(($(nproc)-1))); do sudo cpufreq-set -c $i -g performance; done
    # =<no|yes|try> [default: no]
    --fair-sched=try


    # ==== Memcheck options ====

    # TODO
    #--memcheck:leak-check=full
    #--memcheck:show-leak-kinds=definite,indirect
    #--memcheck:track-origins=yes
    #--memcheck:partial-loads-ok=yes
    #--memcheck:keep-stacktraces=alloc-and-free
    #--memcheck:show-possibly-lost=yes
    #--memcheck:leak-check-heuristics=all


    # ==== Massif options ====
    # http://valgrind.org/docs/manual/ms-manual.html

    # Enable heap profiling.
    # =<yes|no> [default: yes]
    #--massif:heap=no

    # Enable stack profiling.
    # Note main stack size is counted as zero at start-up.
    # WARNING: Slows Massif down greatly.
    # =<yes|no> [default: no]
    #--massif:stacks=yes

    # Profile memory at the page level rather than at the malloc() level.
    # This measures memory allocations at the lower-level mmap/mremap/brk
    # functions, instead of following the higher-level malloc/realloc/new.
    # This will report numbers closer to tools like `top`.
    # =<yes|no> [default: no]
    #--massif:pages-as-heap=yes

    # Add functions to be treated as wrappers to 'malloc' or 'new'.
    # =<name>
    --massif:alloc-fn=g_malloc
    --massif:alloc-fn=g_malloc0
    --massif:alloc-fn=g_realloc
    --massif:alloc-fn=g_try_malloc
    --massif:alloc-fn=g_mem_chunk_alloc

    # The time base used for the profiling results.
    # - 'i': Instructions executed.
    # - 'ms' (i.e. milliseconds): real (wallclock) time.
    # - 'B': Bytes allocated/deallocated on the heap and/or stack.
    # =<i|ms|B> [default: i]
    --massif:time-unit=B

    # Frequency of detailed snapshots. 1 = every snapshot is detailed.
    # =<n> [default: 10]
    #--massif:detailed-freq=3

    # Write the profiling data to the specified file.
    # =<file> [default: massif.out.%p]
    # --massif-out-file=valgrind-massif-%n-%p.out
)
