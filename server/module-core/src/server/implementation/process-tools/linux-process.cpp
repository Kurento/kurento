/*
 * (C) Copyright 2019 Kurento (https://www.kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "linux-process.hpp"

#include <fstream>
#include <sstream>
#include <string>

#include <sched.h>
#include <unistd.h> // sysconf()

#define STAT_PATH "/proc/stat"

#define SELF_STAT_PATH "/proc/self/stat"
#define SELF_STAT_UTIME_FIELD 14

#define SELF_STATM_FILE_PATH "/proc/self/statm"

// ----------------------------------------------------------------------------

unsigned long
cpuCount ()
{
  // Try checking current process' affinity mask
  {
    cpu_set_t set;
    if (sched_getaffinity (0, sizeof (set), &set) == 0) {
      unsigned long count = (unsigned long) CPU_COUNT (&set);
      if (count > 0) {
        return count;
      }
    }
  }

  // Try with `sysconf (_SC_NPROCESSORS_ONLN)`
#if defined _SC_NPROCESSORS_ONLN
  {
    long count = sysconf (_SC_NPROCESSORS_ONLN);
    if (count > 0) {
      return (unsigned long) count;
    }
  }
#endif

  return 1;
}

// ----------------------------------------------------------------------------

/**
 * Total amount of time that this process has been scheduled, in clock ticks.
 *
 * Data is obtained from "/proc/self/stat", as per man proc(5). This value
 * includes time scheduled in user and kernel modes.
 */
static unsigned long
processTicks ()
{
  std::ifstream stat (SELF_STAT_PATH);
  if (!stat) {
    return 0;
  }

  // Skip unwanted fields
  for (int field = 1; field < SELF_STAT_UTIME_FIELD; ++field) {
    std::string unused;
    stat >> unused;
  }

  // (14) utime %lu
  // Amount of time that this process has been scheduled in user mode,
  // measured in clock ticks
  long unsigned utimeTicks;
  stat >> utimeTicks;

  // (15) stime %lu
  // Amount of time that this process has been scheduled in kernel mode,
  // measured in clock ticks
  long unsigned stimeTicks;
  stat >> stimeTicks;

  if (!stat) {
    return 0;
  }

  return utimeTicks + stimeTicks;
}

// ----------------------------------------------------------------------------

/**
 * Total amount of time that the system has spent, in clock ticks.
 *
 * Data is obtained from "/proc/stat", as per man proc(5). This value includes
 * time scheduled in all possible states.
 */
static unsigned long
systemTicks ()
{
  std::ifstream stat (STAT_PATH);
  if (!stat) {
    return 0;
  }

  std::string line;
  std::getline(stat, line);
  std::istringstream cpu(line);

  // Get first field; should be "cpu" (not "cpu0")
  std::string entryName;
  cpu >> entryName;
  if (entryName != "cpu") {
    return 0;
  }

  // Sum all other fields in the line
  unsigned long cpuTicks[10] = {0};
  unsigned long ticks = 0;

  for (int i = 0; i < 10 && cpu >> cpuTicks[i]; ++i) {
    ticks += cpuTicks[i];
  }

  // Guest time is already accounted in usertime
  // https://github.com/hishamhm/htop/blob/402e46bb82964366746b86d77eb5afa69c279539/linux/LinuxProcessList.c#L992
  ticks -= cpuTicks[8];  // man proc(5): guest (9)
  ticks -= cpuTicks[9];  // man proc(5): guest_nice (10)

  return ticks;
}

// ----------------------------------------------------------------------------

void
cpuPercentBegin (struct cpustat_t *cpustat)
{
  cpustat->processTicks = processTicks();
  cpustat->systemTicks = systemTicks();
}

// ----------------------------------------------------------------------------

float cpuPercentEnd (const struct cpustat_t *cpustat)
{
  const unsigned long processTicksInc = processTicks() - cpustat->processTicks;

  // https://github.com/hishamhm/htop/blob/402e46bb82964366746b86d77eb5afa69c279539/linux/LinuxProcessList.c#L1032
  const unsigned long systemTicksInc = (systemTicks() - cpustat->systemTicks)
      / cpuCount();

  // https://github.com/hishamhm/htop/blob/402e46bb82964366746b86d77eb5afa69c279539/linux/LinuxProcessList.c#L832
  return 100.0f * processTicksInc / systemTicksInc;
}

// ----------------------------------------------------------------------------

long int memoryUse ()
{
  std::ifstream statm (SELF_STATM_FILE_PATH);

  if (!statm) {
    return 0;
  }

  // (1) size - total program size (VmSize, in pages)
  long int size_pages;
  statm >> size_pages;

  // (2) resident - resident set size (VmRSS, in pages)
  long int resident_pages;
  statm >> resident_pages;

  if (!statm) {
    return 0;
  }

  const long int pagesize_bytes = sysconf(_SC_PAGESIZE);
  const long int resident_kbytes = resident_pages * pagesize_bytes / 1024;

  return resident_kbytes;
}

// ----------------------------------------------------------------------------
